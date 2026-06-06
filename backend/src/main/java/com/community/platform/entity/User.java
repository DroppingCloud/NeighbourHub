package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User account.
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

    /** active/disabled */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** admin/staff/resident/family */
    private String role;

    private Long communityId;

    /** Staff business type: application or booking. */
    private String staffType;

    /** Booking service type for booking staff: dining/accompany/home_visit. */
    private String bookingServiceType;

    /** System generated login account, such as SQ0001. */
    private String account;

    /** Avatar relative file path. */
    private String avatar;
}
