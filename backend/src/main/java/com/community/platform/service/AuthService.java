package com.community.platform.service;

import com.community.platform.dto.auth.LoginDTO;
import com.community.platform.dto.auth.RegisterDTO;
import com.community.platform.vo.auth.LoginVO;
import com.community.platform.vo.user.UserInfoVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    void register(RegisterDTO dto);
    void logout(Long userId);
    UserInfoVO getMe(Long userId);
}
