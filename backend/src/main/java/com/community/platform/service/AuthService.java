package com.community.platform.service;

import com.community.platform.dto.auth.LoginDTO;
import com.community.platform.dto.auth.ProfileUpdateDTO;
import com.community.platform.dto.auth.RegisterDTO;
import com.community.platform.vo.auth.LoginVO;
import com.community.platform.vo.user.UserInfoVO;
import com.community.platform.dto.auth.ResetPasswordDTO; 

public interface AuthService {
    LoginVO login(LoginDTO dto);
    LoginVO loginBySms(String phone);
    void register(RegisterDTO dto);
    void logout(Long userId);
    UserInfoVO getMe(Long userId);
    void updateMe(Long userId, ProfileUpdateDTO dto);
        String uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);
        org.springframework.core.io.Resource loadAvatar(Long userId);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void deleteAccount(Long userId);
    void resetPassword(ResetPasswordDTO dto);

}
