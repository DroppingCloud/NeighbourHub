package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单表
 */
@Data
@TableName("work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
    private Long orderId;

    private Long applicationId;

    /** 处理工作人员 ID */
    private Long staffUserId;

    /**
     * 工单状态:
     * pending - 待审核
     * approved - 审核通过
     * rejected - 已驳回
     * supplement_required - 退回补件
     * completed - 已办结
     */
    private String status;

    private String auditOpinion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime finishTime;

    private Long communityId;
}
