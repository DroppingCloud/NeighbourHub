package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息表
 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long noticeId;

    private Long userId;

    private String title;

    private String content;

    /** 通知类型: audit_result/supplement/booking/system */
    private String type;

    /** 关联业务 ID（申请ID/预约ID等） */
    private Long refId;

    private Integer isRead;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
