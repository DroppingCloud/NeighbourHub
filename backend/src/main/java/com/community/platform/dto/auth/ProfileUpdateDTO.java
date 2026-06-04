package com.community.platform.dto.auth;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateDTO {

    private String realName;

    private String email;

    private String gender;

    private LocalDate birthday;

    private String address;
    /** 可选：头像相对路径或 URL */
    private String avatar;
}
