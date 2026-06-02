package com.community.platform.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务预约 DTO
 */
@Data
public class BookingDTO {

    @NotBlank(message = "服务类型不能为空")
    private String serviceType;

    @NotNull(message = "期望服务时间不能为空")
    private LocalDateTime expectTime;

    private String address;

    private String remark;

    /**
     * 居民档案ID（可选，若为空则默认使用当前登录用户关联的档案）
     */
    private Long profileId;

    /**
     * 代办人用户ID（可选，家属代预约时使用）
     */
    private Long proxyUserId;

    private Long proxyFor;
}