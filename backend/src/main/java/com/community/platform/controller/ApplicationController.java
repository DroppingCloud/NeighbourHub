package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.dto.application.ApplicationQueryDTO;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.dto.material.ApplicationMaterialDTO;
import com.community.platform.dto.material.MaterialPrecheckDTO;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.ApplicationService;
import com.community.platform.vo.application.ApplicationVO;
import com.community.platform.vo.application.MaterialCompletenessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "事项申请")
@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMaterialService applicationMaterialService;

    @PostMapping("/submit")
    public Result<Long> submit(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody ApplicationSubmitDTO dto,
                               @RequestParam(value = "_proxyFor", required = false) Long proxyFor) {
        dto.setProxyFor(proxyFor);
        return Result.success(applicationService.submit(userId, dto));
    }

    @GetMapping("/list")
    public Result<Page<ApplicationVO>> list(@AuthenticationPrincipal Long userId,
                                            ApplicationQueryDTO query,
                                            @RequestParam(value = "_proxyFor", required = false) Long proxyFor) {
        return Result.success(applicationService.getList(userId, query, proxyFor));
    }

    @Operation(summary = "申请详情")
    @GetMapping("/{id}")
    public Result<ApplicationVO> detail(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id) {
        return Result.success(applicationService.getDetail(userId, id));
    }

    @Operation(summary = "补件或撤回后重新提交")
    @PutMapping("/{id}/resubmit")
    public Result<Void> resubmit(@AuthenticationPrincipal Long userId,
                                 @PathVariable Long id,
                                 @Valid @RequestBody ApplicationSubmitDTO dto) {
        applicationService.resubmit(userId, id, dto);
        return Result.success();
    }

    @Operation(summary = "撤回申请")
    @PutMapping("/{id}/withdraw")
    public Result<Void> withdraw(@AuthenticationPrincipal Long userId,
                                 @PathVariable Long id) {
        applicationService.withdraw(userId, id);
        return Result.success();
    }

    @Operation(summary = "登记申请材料元数据")
    @PostMapping("/{id}/materials")
    public Result<Long> uploadMaterial(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ApplicationMaterialDTO dto) {
        return Result.success(applicationMaterialService.upload(userId, id, dto));
    }

    @Operation(summary = "上传申请材料文件")
    @PostMapping(value = "/{id}/materials/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> uploadMaterialFile(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long id,
                                           @RequestParam(required = false) Long templateId,
                                           @RequestParam(required = false) String materialName,
                                           @RequestParam MultipartFile file) {
        return Result.success(applicationMaterialService.uploadFile(userId, id, templateId, materialName, file));
    }

    @Operation(summary = "申请材料列表")
    @GetMapping("/{id}/materials")
    public Result<List<ApplicationMaterial>> listMaterials(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long id) {
        return Result.success(applicationMaterialService.getList(userId, id));
    }

    @Operation(summary = "材料完整性校验")
    @GetMapping("/{id}/materials/completeness")
    public Result<MaterialCompletenessVO> checkMaterialCompleteness(@AuthenticationPrincipal Long userId,
                                                                    @PathVariable Long id) {
        return Result.success(applicationMaterialService.checkCompleteness(userId, id));
    }

    @Operation(summary = "材料预审")
    @PutMapping("/material/{id}/precheck")
    public Result<Void> precheckMaterial(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long id,
                                         @Valid @RequestBody MaterialPrecheckDTO dto) {
        applicationMaterialService.precheck(userId, id, dto);
        return Result.success();
    }

    @Operation(summary = "下载或预览材料文件")
    @GetMapping("/material/{id}/file")
    public ResponseEntity<Resource> getMaterialFile(@AuthenticationPrincipal Long userId,
                                                    @PathVariable Long id) {
        ApplicationMaterial material = applicationMaterialService.getReadableMaterial(userId, id);
        Resource resource = applicationMaterialService.loadMaterialFile(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + material.getFileName() + "\"")
                .contentType(resolveContentType(material.getFileType(), material.getFileName()))
                .body(resource);
    }

    private MediaType resolveContentType(String fileType, String fileName) {
        String extension = fileType;
        if (extension == null || extension.isBlank()) {
            int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
            extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1) : "";
        }
        String normalized = extension.toLowerCase();
        Map<String, MediaType> types = Map.of(
                "pdf", MediaType.APPLICATION_PDF,
                "jpg", MediaType.IMAGE_JPEG,
                "jpeg", MediaType.IMAGE_JPEG,
                "png", MediaType.IMAGE_PNG,
                "doc", MediaType.parseMediaType("application/msword"),
                "docx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
        return types.getOrDefault(normalized, MediaType.APPLICATION_OCTET_STREAM);
    }
}
