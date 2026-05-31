package com.community.platform.vo.application;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Material completeness result for an application.
 */
@Data
public class MaterialCompletenessVO {

    private Long applicationId;

    private Long itemId;

    private Integer requiredCount = 0;

    private Integer uploadedRequiredCount = 0;

    private Boolean complete = false;

    private List<String> missingMaterialNames = new ArrayList<>();
}
