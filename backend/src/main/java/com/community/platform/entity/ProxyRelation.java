package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Family proxy authorization relation.
 */
@Data
@TableName("proxy_relation")
public class ProxyRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long proxyUserId;

    private Long targetUserId;

    private Long targetProfileId;

    private String relation;

    private String authorizedActions;

    /** pending/active/revoked/rejected */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String proxyUserName;

    @TableField(exist = false)
    private String targetProfileName;
}
