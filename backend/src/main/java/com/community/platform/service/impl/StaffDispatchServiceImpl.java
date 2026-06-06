package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.entity.User;
import com.community.platform.entity.WorkOrder;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.WorkOrderMapper;
import com.community.platform.service.StaffDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffDispatchServiceImpl implements StaffDispatchService {

    private final UserMapper userMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ServiceBookingMapper serviceBookingMapper;

    @Override
    public User selectBestStaff(Long communityId, Long excludeUserId, boolean allowGlobalFallback) {
        return selectBestStaff(communityId, excludeUserId, allowGlobalFallback, null);
    }

    @Override
    public User selectBestStaff(Long communityId, Long excludeUserId, boolean allowGlobalFallback, String staffType) {
        List<User> candidates = listStaff(communityId, excludeUserId, staffType);
        if (candidates.isEmpty() && communityId == null && allowGlobalFallback) {
            candidates = listStaff(null, excludeUserId, staffType);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator
                        .comparingLong(this::workloadScore)
                        .thenComparing(User::getUserId))
                .orElse(null);
    }

    private List<User> listStaff(Long communityId, Long excludeUserId, String staffType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "staff")
                .eq(User::getStatus, "active")
                .orderByAsc(User::getUserId);
        if (StringUtils.hasText(staffType)) {
            wrapper.eq(User::getStaffType, staffType);
        }
        if (communityId != null) {
            wrapper.eq(User::getCommunityId, communityId);
        }
        if (excludeUserId != null) {
            wrapper.ne(User::getUserId, excludeUserId);
        }
        return userMapper.selectList(wrapper);
    }

    private long workloadScore(User staff) {
        Long staffId = staff.getUserId();
        if ("booking".equals(staff.getStaffType())) {
            return countBookings(staffId, "confirmed") * 3
                    + countBookings(staffId, "in_progress") * 5;
        }
        return countWorkOrders(staffId, "pending") * 3
                + countWorkOrders(staffId, "processing") * 5;
    }

    private long countWorkOrders(Long staffId, String status) {
        return workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getStaffUserId, staffId)
                .eq(WorkOrder::getStatus, status));
    }

    private long countBookings(Long staffId, String status) {
        return serviceBookingMapper.selectCount(new LambdaQueryWrapper<ServiceBooking>()
                .eq(ServiceBooking::getStaffUserId, staffId)
                .eq(ServiceBooking::getStatus, status));
    }
}
