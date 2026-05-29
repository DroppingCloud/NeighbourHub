package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区服务预约表
 */
@Data
@TableName("service_booking")
public class ServiceBooking {

    @TableId(type = IdType.AUTO)
    private Long bookingId;

    private Long userId;

    /** Resident profile for the real service target. */
    private Long profileId;

    /** Proxy user ID when a family member books for a resident. */
    private Long proxyUserId;

    /** 服务类型: dining/accompany/home_visit */
    private String serviceType;

    private LocalDateTime expectTime;

    /**
     * 预约状态:
     * pending - 待确认
     * confirmed - 已确认
     * in_progress - 服务中
     * completed - 已完成
     * cancelled - 已取消
     */
    private String status;

    private String address;

    private String remark;

    /** Assigned staff user ID. */
    private Long staffUserId;

    private String feedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime completeTime;

    @TableLogic
    private Integer deleted;
}
