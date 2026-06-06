package com.community.platform.vo.user;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户信息 VO
 */
@Data
public class UserInfoVO {

    private Long userId;

    private String username;

    private String phone;

    private String email;

    private List<String> roles;

    private String realName;

    private String idCard;

    private String address;

    private Integer age;

    private String gender;

    private LocalDate birthday;
    private String avatar;

    private String staffType;

    private String bookingServiceType;
}
