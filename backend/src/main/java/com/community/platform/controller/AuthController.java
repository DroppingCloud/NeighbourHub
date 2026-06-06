package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.auth.LoginDTO;
import com.community.platform.dto.auth.ProfileUpdateDTO;
import com.community.platform.dto.auth.RegisterDTO;
import com.community.platform.dto.auth.ResetPasswordDTO;
import com.community.platform.service.AuthService;
import com.community.platform.vo.auth.LoginVO;
import com.community.platform.vo.user.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "短信登录（模拟/开发用）")
    @PostMapping("/login/sms")
    public Result<LoginVO> loginSms(@Valid @RequestBody com.community.platform.dto.auth.LoginSmsDTO dto) {
        // Note: does not validate SMS code - frontend currently simulates code.
        return Result.success(authService.loginBySms(dto.getPhone()));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> getMe(@AuthenticationPrincipal Long userId) {
        return Result.success(authService.getMe(userId));
    }

    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@AuthenticationPrincipal Long userId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String path = authService.uploadAvatar(userId, file);
        return Result.success(path);
    }

    @Operation(summary = "获取用户头像文件")
    @GetMapping("/avatar/{id}")
    public ResponseEntity<Resource> getAvatar(@PathVariable("id") Long id) {
        Resource resource = authService.loadAvatar(id);
        return resource == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(resource);
    }

    @Operation(summary = "更新当前用户个人信息")
    @PutMapping("/me")
    public Result<Void> updateMe(@AuthenticationPrincipal Long userId,
                                 @RequestBody ProfileUpdateDTO dto) {
        authService.updateMe(userId, dto);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@AuthenticationPrincipal Long userId,
                                       @RequestBody com.community.platform.dto.auth.ChangePasswordDTO dto) {
        if (dto.getNewPassword() == null || !dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail(com.community.platform.common.ResultCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        authService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "重置密码（忘记密码）")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.fail(com.community.platform.common.ResultCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        authService.resetPassword(dto);
        return Result.success();
    }

    @Operation(summary = "注销并删除账号（不可恢复）")
    @DeleteMapping("/account")
    public Result<Void> deleteAccount(@AuthenticationPrincipal Long userId) {
        authService.deleteAccount(userId);
        return Result.success();
    }
}
