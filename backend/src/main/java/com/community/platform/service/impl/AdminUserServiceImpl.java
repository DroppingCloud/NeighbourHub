package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;
import com.community.platform.entity.UserRole;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.UserRoleMapper;
import com.community.platform.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ResidentProfileMapper residentProfileMapper;

    @Override
    public Page<Map<String, Object>> getUserList(String role, Integer pageNum, Integer pageSize) {
        Page<User> page = userMapper.selectPage(new Page<>(page(pageNum), size(pageSize)),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));

        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(this::toMap)
                .filter(item -> !StringUtils.hasText(role) || role.equals(item.get("role")))
                .toList());
        return result;
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
