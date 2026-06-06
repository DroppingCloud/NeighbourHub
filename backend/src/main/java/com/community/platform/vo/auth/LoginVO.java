package com.community.platform.vo.auth;

import lombok.Data;

import java.util.List;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private List<String> roles;

    private String staffType;
}
