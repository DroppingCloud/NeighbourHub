package com.community.platform.service.impl;

import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.vo.application.MaterialAiPrecheckResultVO;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialOcrAiPrecheckServiceTest {

    @Test
    void missingFileShouldFailPrecheckWithoutCallingExternalAi() {
        RuleBasedMaterialOcrAiPrecheckService service = new RuleBasedMaterialOcrAiPrecheckService();
        ApplicationMaterial material = new ApplicationMaterial();
        material.setMaterialName("居民身份证");
        material.setFileName("id-card.png");
        material.setFileType("png");

        MaterialAiPrecheckResultVO result = service.precheck(material, Path.of("not-exists.png"));

        assertEquals("failed", result.getPrecheckStatus());
        assertTrue(result.getPrecheckRemark().contains("OCR/AI"));
    }
}
