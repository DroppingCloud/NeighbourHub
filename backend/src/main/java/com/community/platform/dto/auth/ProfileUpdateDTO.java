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
}
