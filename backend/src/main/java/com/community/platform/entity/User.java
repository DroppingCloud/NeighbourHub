package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    private String password;

    private String phone;

    private String email;

    /** 状态: active/disabled */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    private String role;
    private Long communityId;
    /** 系统分配账号（如 SQxxxx） */
    private String account;
    /** 头像文件相对路径，例如 uploads/avatars/xxx.jpg */
    private String avatar;
}
