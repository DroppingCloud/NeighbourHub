package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请单表
 */
@Data
@TableName("application_form")
public class ApplicationForm {

    @TableId(type = IdType.AUTO)
    private Long applicationId;

    private Long userId;

    private Long itemId;

    /** 代理人用户 ID（家属代办时填写） */
    private Long proxyUserId;

    /**
     * 申请状态:
     * pending - 待审核
     * approved - 已通过
     * rejected - 已驳回
     * supplement_required - 需补件
     * supplementing - 补件中
     * completed - 办结
     */
    private String status;

    /** 表单数据（JSON 存储） */
    private String formData;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submitTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
