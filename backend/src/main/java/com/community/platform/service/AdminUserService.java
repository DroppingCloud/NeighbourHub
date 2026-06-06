package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.admin.StaffCreateDTO;

import java.util.Map;

public interface AdminUserService {
    Page<Map<String, Object>> getUserList(String role, Integer pageNum, Integer pageSize);
    Long createStaff(StaffCreateDTO dto);
}
