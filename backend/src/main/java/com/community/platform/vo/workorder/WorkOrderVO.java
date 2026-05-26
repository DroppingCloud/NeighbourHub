package com.community.platform.vo.workorder;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单 VO
 */
@Data
public class WorkOrderVO {

    private Long orderId;

    private Long applicationId;

    private String itemName;

    private String residentName;

    private String status;

    private String statusLabel;

    private String auditOpinion;

    private String staffName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
