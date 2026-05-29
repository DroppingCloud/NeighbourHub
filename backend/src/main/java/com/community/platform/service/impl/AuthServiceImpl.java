package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.common.utils.JwtUtil;
import com.community.platform.dto.auth.LoginDTO;
import com.community.platform.dto.auth.ProfileUpdateDTO;
import com.community.platform.dto.auth.RegisterDTO;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;
import com.community.platform.entity.UserRole;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.UserRoleMapper;
import com.community.platform.service.AuthService;
import com.community.platform.vo.auth.LoginVO;
import com.community.platform.vo.user.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        List<String> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getUserId()))
                .stream().map(UserRole::getRoleCode).toList();

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), roles);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRoles(roles);
        return vo;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setStatus("active");
        userMapper.insert(user);

        UserRole role = new UserRole();
        role.setUserId(user.getUserId());
        role.setRoleCode("ROLE_RESIDENT");
        userRoleMapper.insert(role);

        ResidentProfile profile = new ResidentProfile();
        profile.setUserId(user.getUserId());
        profile.setRealName(dto.getRealName());
        profile.setIdCard(dto.getIdCard());
        residentProfileMapper.insert(profile);
    }

    @Override
    public void logout(Long userId) {
        // Token 无状态，前端丢弃即可；如需黑名单可在此加入 Redis
        log.info("User {} logged out", userId);
    }

    @Override
    public UserInfoVO getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }

        List<String> roles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleCode).toList();

        ResidentProfile profile = residentProfileMapper.selectOne(
                new LambdaQueryWrapper<ResidentProfile>().eq(ResidentProfile::getUserId, userId));

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRoles(roles);
        if (profile != null) {
            vo.setRealName(profile.getRealName());
            vo.setIdCard(profile.getIdCard());
            vo.setAddress(profile.getAddress());
            vo.setAge(profile.getAge());
            vo.setGender(profile.getGender());
            vo.setBirthday(profile.getBirthday());
        }
        return vo;
    }

    @Override
    @Transactional
    public void updateMe(Long userId, ProfileUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        user.setEmail(dto.getEmail());
        userMapper.updateById(user);

        ResidentProfile profile = residentProfileMapper.selectOne(
                new LambdaQueryWrapper<ResidentProfile>().eq(ResidentProfile::getUserId, userId));
        if (profile == null) {
            profile = new ResidentProfile();
            profile.setUserId(userId);
            profile.setRealName(dto.getRealName() == null || dto.getRealName().isBlank() ? user.getUsername() : dto.getRealName());
            profile.setResidentType("local");
            profile.setAddress(dto.getAddress());
            profile.setGender(dto.getGender());
            profile.setBirthday(dto.getBirthday());
            residentProfileMapper.insert(profile);
            return;
        }
        profile.setRealName(dto.getRealName());
        profile.setAddress(dto.getAddress());
        profile.setGender(dto.getGender());
        profile.setBirthday(dto.getBirthday());
        residentProfileMapper.updateById(profile);
    }
}
