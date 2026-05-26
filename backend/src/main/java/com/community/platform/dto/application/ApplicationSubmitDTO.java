package com.community.platform.dto.application;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 申请提交 DTO
 */
@Data
public class ApplicationSubmitDTO {

    @NotNull(message = "事项ID不能为空")
    private Long itemId;

    /** 代理人用户 ID（家属代办时填写） */
    private Long proxyUserId;

    /** 表单填写数据 */
    private Map<String, Object> formData;

    private String remark;
}
