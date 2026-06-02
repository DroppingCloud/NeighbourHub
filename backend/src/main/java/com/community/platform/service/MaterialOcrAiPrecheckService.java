package com.community.platform.service;

import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.vo.application.MaterialAiPrecheckResultVO;

import java.nio.file.Path;

public interface MaterialOcrAiPrecheckService {
    MaterialAiPrecheckResultVO precheck(ApplicationMaterial material, Path filePath);
}
