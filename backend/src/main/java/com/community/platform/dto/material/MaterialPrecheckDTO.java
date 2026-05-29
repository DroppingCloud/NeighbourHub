package com.community.platform.dto.material;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialPrecheckDTO {

    @NotBlank(message = "预审状态不能为空")
    private String precheckStatus;

    private String precheckRemark;

    private String ocrText;
}
