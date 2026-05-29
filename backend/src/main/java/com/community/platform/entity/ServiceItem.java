package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政务事项表
 */
@Data
@TableName("service_item")
public class ServiceItem {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private String itemName;

    private String itemCode;

    /** 事项分类: 证明/补贴/证件/服务 */
    private String category;

    private String description;

    /** 办理条件说明（JSON 或文本） */
    private String conditions;

    /** Process steps JSON. */
    private String processSteps;

    /** 状态: online/offline */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
