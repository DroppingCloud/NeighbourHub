package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.material.ApplicationMaterialDTO;
import com.community.platform.dto.material.MaterialPrecheckDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ApplicationMaterialMapper;
import com.community.platform.service.ApplicationMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationMaterialServiceImpl implements ApplicationMaterialService {

    private final ApplicationFormMapper applicationFormMapper;
    private final ApplicationMaterialMapper applicationMaterialMapper;

    @Override
    public Long upload(Long userId, Long applicationId, ApplicationMaterialDTO dto) {
        requireReadableApplication(userId, applicationId);
        ApplicationMaterial material = new ApplicationMaterial();
        material.setApplicationId(applicationId);
        material.setTemplateId(dto.getTemplateId());
        material.setMaterialName(dto.getMaterialName());
        material.setFileName(dto.getFileName());
        material.setFilePath(dto.getFilePath());
        material.setFileSize(dto.getFileSize());
        material.setFileType(dto.getFileType());
        material.setPrecheckStatus("pending");
        applicationMaterialMapper.insert(material);
        return material.getMaterialId();
    }

    @Override
    public List<ApplicationMaterial> getList(Long userId, Long applicationId) {
        requireReadableApplication(userId, applicationId);
        return applicationMaterialMapper.selectList(new LambdaQueryWrapper<ApplicationMaterial>()
                .eq(ApplicationMaterial::getApplicationId, applicationId)
                .orderByAsc(ApplicationMaterial::getMaterialId));
    }

    @Override
    public void precheck(Long materialId, MaterialPrecheckDTO dto) {
        if (!Set.of("pending", "passed", "failed").contains(dto.getPrecheckStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "材料预审状态错误");
        }
        ApplicationMaterial material = applicationMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请材料不存在");
        }
        material.setPrecheckStatus(dto.getPrecheckStatus());
        material.setPrecheckRemark(dto.getPrecheckRemark());
        material.setOcrText(dto.getOcrText());
        applicationMaterialMapper.updateById(material);
    }

    private void requireReadableApplication(Long userId, Long applicationId) {
        ApplicationForm application = applicationFormMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ResultCode.APPLICATION_NOT_EXISTS);
        }
        if (!userId.equals(application.getUserId()) && !userId.equals(application.getProxyUserId())) {
            throw new BusinessException(ResultCode.APPLICATION_NO_PERMISSION);
        }
    }
}
