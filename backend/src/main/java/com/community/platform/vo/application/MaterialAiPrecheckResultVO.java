package com.community.platform.vo.application;

import lombok.Data;

@Data
public class MaterialAiPrecheckResultVO {
    private String precheckStatus;
    private String precheckRemark;
    private String ocrText;
}
