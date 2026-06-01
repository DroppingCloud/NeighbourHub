package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 居民档案表
 */
@Data
@TableName("resident_profile")
public class ResidentProfile {

    @TableId(type = IdType.AUTO)
    private Long profileId;

    /** Linked account ID. Nullable for profiles created before account binding. */
    private Long userId;

    private String realName;

    private String idCard;

    private String address;

    private Integer age;

    private String gender;

    private LocalDate birthday;

    /** 户籍类型: local/non_local */
    private String residentType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long communityId;
}
