package com.community.platform.service;

import com.community.platform.dto.material.ApplicationMaterialDTO;
import com.community.platform.dto.material.MaterialPrecheckDTO;
import com.community.platform.entity.ApplicationMaterial;

import java.util.List;

public interface ApplicationMaterialService {
    Long upload(Long userId, Long applicationId, ApplicationMaterialDTO dto);
    List<ApplicationMaterial> getList(Long userId, Long applicationId);
    void precheck(Long materialId, MaterialPrecheckDTO dto);
}
