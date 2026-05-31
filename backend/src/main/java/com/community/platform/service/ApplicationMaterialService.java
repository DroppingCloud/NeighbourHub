package com.community.platform.service;

import com.community.platform.dto.material.ApplicationMaterialDTO;
import com.community.platform.dto.material.MaterialPrecheckDTO;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.vo.application.MaterialCompletenessVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicationMaterialService {
    Long upload(Long userId, Long applicationId, ApplicationMaterialDTO dto);
    Long uploadFile(Long userId, Long applicationId, Long templateId, String materialName, MultipartFile file);
    List<ApplicationMaterial> getList(Long userId, Long applicationId);
    MaterialCompletenessVO checkCompleteness(Long userId, Long applicationId);
    MaterialCompletenessVO checkCompletenessForSystem(Long applicationId);
    boolean hasFailedPrecheckForSystem(Long applicationId);
    void precheck(Long userId, Long materialId, MaterialPrecheckDTO dto);
    ApplicationMaterial getReadableMaterial(Long userId, Long materialId);
    Resource loadMaterialFile(Long userId, Long materialId);
}
