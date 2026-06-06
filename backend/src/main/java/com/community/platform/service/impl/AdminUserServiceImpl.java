package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.admin.StaffCreateDTO;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;
import com.community.platform.entity.UserRole;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.UserRoleMapper;
import com.community.platform.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<Map<String, Object>> getUserList(String role, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime);
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        Page<User> page = userMapper.selectPage(new Page<>(page(pageNum), size(pageSize)),
                wrapper);

        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(this::toMap)
                .toList());
        return result;
    }

    @Override
    @Transactional
    public Long createStaff(StaffCreateDTO dto) {
        String staffType = dto.getStaffType() == null ? "" : dto.getStaffType().trim().toLowerCase();
        if (!"application".equals(staffType) && !"booking".equals(staffType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "工作人员类型只能为 application 或 booking");
        }
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()))) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS, "用户名已存在");
        }
        if (StringUtils.hasText(dto.getPhone())
                && userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()))) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS, "手机号已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode("123456"));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus("active");
        user.setRole("staff");
        user.setStaffType(staffType);
        user.setCommunityId(dto.getCommunityId());
        userMapper.insert(user);

        UserRole role = new UserRole();
        role.setUserId(user.getUserId());
        role.setRoleCode("ROLE_STAFF");
        userRoleMapper.insert(role);

        ResidentProfile profile = new ResidentProfile();
        profile.setUserId(user.getUserId());
        profile.setRealName(dto.getRealName());
        profile.setResidentType("local");
        profile.setCommunityId(dto.getCommunityId());
        residentProfileMapper.insert(profile);
        return user.getUserId();
    }

    private Map<String, Object> toMap(User user) {
        UserRole role = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getUserId())
                .last("LIMIT 1"));
        ResidentProfile profile = residentProfileMapper.selectOne(new LambdaQueryWrapper<ResidentProfile>()
                .eq(ResidentProfile::getUserId, user.getUserId())
                .last("LIMIT 1"));

        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getUserId());
        map.put("username", user.getUsername());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("status", user.getStatus());
        map.put("createTime", user.getCreateTime());
        map.put("realName", profile == null ? null : profile.getRealName());
        map.put("role", normalizeRole(role == null ? null : role.getRoleCode()));
        map.put("staffType", user.getStaffType());
        map.put("communityId", user.getCommunityId());
        return map;
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null) return "resident";
        return switch (roleCode) {
            case "ROLE_ADMIN" -> "admin";
            case "ROLE_STAFF" -> "staff";
            case "ROLE_FAMILY" -> "family";
            default -> "resident";
        };
    }

    private long page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long size(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
