package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.material.ApplicationMaterialDTO;
import com.community.platform.dto.material.MaterialPrecheckDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.entity.ServiceMaterialTemplate;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ApplicationMaterialMapper;
import com.community.platform.mapper.ServiceMaterialTemplateMapper;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.MaterialOcrAiPrecheckService;
import com.community.platform.vo.application.MaterialCompletenessVO;
import com.community.platform.vo.application.MaterialAiPrecheckResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationMaterialServiceImpl implements ApplicationMaterialService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
    private static final Set<String> PRECHECK_STATUS = Set.of("pending", "passed", "failed");

    private final ApplicationFormMapper applicationFormMapper;
    private final ApplicationMaterialMapper applicationMaterialMapper;
    private final ServiceMaterialTemplateMapper materialTemplateMapper;
    private final MaterialOcrAiPrecheckService materialOcrAiPrecheckService;

    @Value("${app.upload.material-dir:uploads/materials}")
    private String materialUploadDir;

    @Override
    public Long upload(Long userId, Long applicationId, ApplicationMaterialDTO dto) {
        ApplicationForm application = requireReadableApplication(userId, applicationId);
        ServiceMaterialTemplate template = validateTemplate(application, dto.getTemplateId(), dto.getMaterialName());

        ApplicationMaterial material = new ApplicationMaterial();
        material.setApplicationId(applicationId);
        material.setTemplateId(template == null ? dto.getTemplateId() : template.getTemplateId());
        material.setMaterialName(StringUtils.hasText(dto.getMaterialName())
                ? dto.getMaterialName()
                : template.getMaterialName());
        material.setFileName(dto.getFileName());
        material.setFilePath(dto.getFilePath());
        material.setFileSize(dto.getFileSize());
        material.setFileType(dto.getFileType());
        material.setPrecheckStatus("pending");
        applicationMaterialMapper.insert(material);
        return material.getMaterialId();
    }

    @Override
    public Long uploadFile(Long userId, Long applicationId, Long templateId, String materialName, MultipartFile file) {
        ApplicationForm application = requireReadableApplication(userId, applicationId);
        ServiceMaterialTemplate template = validateTemplate(application, templateId, materialName);
        validateFile(file);

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "material" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;
        Path relativePath = Paths.get(String.valueOf(applicationId), storedName);
        Path targetPath = Paths.get(materialUploadDir).toAbsolutePath().normalize().resolve(relativePath).normalize();
        Path root = Paths.get(materialUploadDir).toAbsolutePath().normalize();
        if (!targetPath.startsWith(root)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件路径非法");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }

        ApplicationMaterialDTO dto = new ApplicationMaterialDTO();
        dto.setTemplateId(template == null ? templateId : template.getTemplateId());
        dto.setMaterialName(StringUtils.hasText(materialName) ? materialName : template.getMaterialName());
        dto.setFileName(originalName);
        dto.setFilePath(relativePath.toString().replace('\\', '/'));
        dto.setFileSize(file.getSize());
        dto.setFileType(extension);
        Long materialId = upload(userId, applicationId, dto);
        runAiPrecheck(userId, materialId);
        return materialId;
    }

    @Override
    public List<ApplicationMaterial> getList(Long userId, Long applicationId) {
        requireReadableApplication(userId, applicationId);
        return listMaterials(applicationId);
    }

    @Override
    public MaterialCompletenessVO checkCompleteness(Long userId, Long applicationId) {
        requireReadableApplication(userId, applicationId);
        return checkCompletenessForSystem(applicationId);
    }

    @Override
    public MaterialCompletenessVO checkCompletenessForSystem(Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        List<ServiceMaterialTemplate> requiredTemplates = materialTemplateMapper.selectList(
                new LambdaQueryWrapper<ServiceMaterialTemplate>()
                        .eq(ServiceMaterialTemplate::getItemId, application.getItemId())
                        .eq(ServiceMaterialTemplate::getIsRequired, 1)
                        .orderByAsc(ServiceMaterialTemplate::getSortOrder)
                        .orderByAsc(ServiceMaterialTemplate::getTemplateId));
        List<ApplicationMaterial> uploadedMaterials = listMaterials(applicationId);

        Set<Long> uploadedRequiredTemplateIds = new HashSet<>();
        for (ApplicationMaterial material : uploadedMaterials) {
            if (material.getTemplateId() != null && !"failed".equals(material.getPrecheckStatus())) {
                uploadedRequiredTemplateIds.add(material.getTemplateId());
            }
        }

        MaterialCompletenessVO result = new MaterialCompletenessVO();
        result.setApplicationId(applicationId);
        result.setItemId(application.getItemId());
        result.setRequiredCount(requiredTemplates.size());

        int uploadedRequiredCount = 0;
        for (ServiceMaterialTemplate template : requiredTemplates) {
            if (uploadedRequiredTemplateIds.contains(template.getTemplateId())) {
                uploadedRequiredCount++;
            } else {
                result.getMissingMaterialNames().add(template.getMaterialName());
            }
        }

        result.setUploadedRequiredCount(uploadedRequiredCount);
        result.setComplete(result.getMissingMaterialNames().isEmpty());
        return result;
    }

    @Override
    public boolean hasFailedPrecheckForSystem(Long applicationId) {
        Long count = applicationMaterialMapper.selectCount(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId)
                .eq(ApplicationMaterial::getPrecheckStatus, "failed"));
        return count != null && count > 0;
    }

    @Override
    public void precheck(Long userId, Long materialId, MaterialPrecheckDTO dto) {
        if (!PRECHECK_STATUS.contains(dto.getPrecheckStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "材料预审状态错误");
        }
        ApplicationMaterial material = getReadableMaterial(userId, materialId);
        material.setPrecheckStatus(dto.getPrecheckStatus());
        material.setPrecheckRemark(dto.getPrecheckRemark());
        material.setOcrText(dto.getOcrText());
        applicationMaterialMapper.updateById(material);
    }

    @Override
    public ApplicationMaterial runAiPrecheck(Long userId, Long materialId) {
        ApplicationMaterial material = getReadableMaterial(userId, materialId);
        if (!StringUtils.hasText(material.getFilePath())) {
            material.setPrecheckStatus("failed");
            material.setPrecheckRemark("OCR/AI预审未通过：材料文件路径为空");
            material.setOcrText("OCR/AI预审模式：本地规则引擎演示版\n材料文件路径为空，无法识别。");
            applicationMaterialMapper.updateById(material);
            return material;
        }

        Path path = Paths.get(materialUploadDir).toAbsolutePath().normalize().resolve(material.getFilePath()).normalize();
        Path root = Paths.get(materialUploadDir).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            material.setPrecheckStatus("failed");
            material.setPrecheckRemark("OCR/AI预审未通过：材料文件路径非法");
            material.setOcrText("OCR/AI预审模式：本地规则引擎演示版\n材料文件路径非法，无法识别。");
            applicationMaterialMapper.updateById(material);
            return material;
        }

        MaterialAiPrecheckResultVO result = materialOcrAiPrecheckService.precheck(material, path);
        material.setPrecheckStatus(result.getPrecheckStatus());
        material.setPrecheckRemark(result.getPrecheckRemark());
        material.setOcrText(result.getOcrText());
        applicationMaterialMapper.updateById(material);
        return material;
    }

    @Override
    public ApplicationMaterial getReadableMaterial(Long userId, Long materialId) {
        ApplicationMaterial material = applicationMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请材料不存在");
        }
        requireReadableApplication(userId, material.getApplicationId());
        return material;
    }

    @Override
    public Resource loadMaterialFile(Long userId, Long materialId) {
        ApplicationMaterial material = getReadableMaterial(userId, materialId);
        Path path = Paths.get(materialUploadDir).toAbsolutePath().normalize().resolve(material.getFilePath()).normalize();
        Path root = Paths.get(materialUploadDir).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "材料文件不存在");
        }
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "材料文件读取失败");
        }
    }

    private ServiceMaterialTemplate validateTemplate(ApplicationForm application, Long templateId, String materialName) {
        if (templateId == null) {
            if (!StringUtils.hasText(materialName)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "材料名称不能为空");
            }
            return null;
        }
        ServiceMaterialTemplate template = materialTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "材料模板不存在");
        }
        if (!application.getItemId().equals(template.getItemId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "材料模板不属于当前申请事项");
        }
        return template;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能超过 20MB");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件格式不支持，仅支持 PDF、JPG、PNG、DOC、DOCX");
        }
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private ApplicationForm requireReadableApplication(Long userId, Long applicationId) {
        ApplicationForm application = requireApplication(applicationId);
        if (canStaffAccess()) {
            return application;
        }
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }
        return application;
    }

    private boolean canStaffAccess() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> Set.of("ROLE_STAFF", "ROLE_ADMIN").contains(authority.getAuthority()));
    }

    private ApplicationForm requireApplication(Long applicationId) {
        ApplicationForm application = applicationFormMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ResultCode.APPLICATION_NOT_EXISTS);
        }
        return application;
    }

    private List<ApplicationMaterial> listMaterials(Long applicationId) {
        return applicationMaterialMapper.selectList(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId)
                .orderByAsc(ApplicationMaterial::getMaterialId));
    }
}
