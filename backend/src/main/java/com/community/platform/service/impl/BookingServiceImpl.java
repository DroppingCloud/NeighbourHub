package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.common.utils.SecurityUtils;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.entity.User;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.service.BookingService;
import com.community.platform.service.NoticeService;
import com.community.platform.vo.booking.BookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<String> SERVICE_TYPES = Set.of("dining", "accompany", "home_visit");

    private final ServiceBookingMapper serviceBookingMapper;
    private final NoticeService noticeService;
    private final UserMapper userMapper;                 // 新增
    private final com.community.platform.mapper.UserRoleMapper userRoleMapper;
    private final ResidentProfileMapper residentProfileMapper; // 新增
    private final com.community.platform.service.ProxyPermissionService proxyPermissionService;

    // ==================== 原有方法（保留并增强） ====================

    @Override
    @Transactional
    public Long create(Long userId, BookingDTO dto) {
        Long targetUserId = dto.getProxyFor() != null ? dto.getProxyFor() : userId;
        // 代办权限校验
        if (dto.getProxyFor() != null) {
            proxyPermissionService.validateProxyPermission(userId, targetUserId, "booking");
        }

        if (!SERVICE_TYPES.contains(dto.getServiceType())) {
            throw new BusinessException(ResultCode.SERVICE_NOT_AVAILABLE);
        }
        long conflicts = serviceBookingMapper.selectCount(new LambdaQueryWrapper<ServiceBooking>()
                .eq(ServiceBooking::getUserId, targetUserId)
                .eq(ServiceBooking::getServiceType, dto.getServiceType())
                .eq(ServiceBooking::getExpectTime, dto.getExpectTime())
                .in(ServiceBooking::getStatus, "pending", "confirmed", "in_progress"));
        if (conflicts > 0) {
            throw new BusinessException(ResultCode.TIME_CONFLICT);
        }

        // 获取社区ID
        Long communityId = resolveCommunityId(targetUserId, dto.getProfileId());

        ServiceBooking booking = new ServiceBooking();
        booking.setUserId(targetUserId);
        if (dto.getProfileId() != null) {
            booking.setProfileId(dto.getProfileId());
        } else if (dto.getProxyFor() != null) {
            ResidentProfile profile = residentProfileMapper.selectOne(new LambdaQueryWrapper<ResidentProfile>()
                    .eq(ResidentProfile::getUserId, targetUserId));
            if (profile != null) {
                booking.setProfileId(profile.getProfileId());
            }
        }
        booking.setProxyUserId(dto.getProxyFor() != null ? userId : null);
        booking.setServiceType(dto.getServiceType());
        booking.setExpectTime(dto.getExpectTime());
        booking.setAddress(dto.getAddress());
        booking.setRemark(dto.getRemark());
        booking.setStatus("pending");
        booking.setCommunityId(communityId);
        serviceBookingMapper.insert(booking);

        noticeService.sendNotice(targetUserId, "预约已提交", "您的社区服务预约已提交，等待确认。", "booking", "booking", booking.getBookingId());
        return booking.getBookingId();
    }

    @Override
    public Page<BookingVO> getList(Long userId, Integer pageNum, Integer pageSize, Long proxyFor) {
        LambdaQueryWrapper<ServiceBooking> wrapper = new LambdaQueryWrapper<>();
        if (proxyFor != null) {
            // 校验代理权限
            proxyPermissionService.validateProxyPermission(userId, proxyFor, "query");
            // 家属代办模式：查询为该被代理人预约的记录
            wrapper.eq(ServiceBooking::getUserId, proxyFor);
        } else {
            // 居民本人模式：查询自己提交的 或 别人代自己预约的记录
            wrapper.and(w -> w.eq(ServiceBooking::getUserId, userId)
                             .or().eq(ServiceBooking::getProxyUserId, userId));
        }
        wrapper.orderByDesc(ServiceBooking::getCreateTime);
        Page<ServiceBooking> page = serviceBookingMapper.selectPage(new Page<>(page(pageNum), size(pageSize)), wrapper);
        Page<BookingVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toBookingVO).toList());
        return result;
    }


    @Override
    public BookingVO getDetail(Long userId, Long bookingId) {
        ServiceBooking booking = requireBooking(bookingId);
        if (!userId.equals(booking.getUserId()) && !userId.equals(booking.getProxyUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该预约");
        }
        return toBookingVO(booking);
    }

    @Override
    public void cancel(Long userId, Long bookingId) {
        ServiceBooking booking = requireBooking(bookingId);
        if (!userId.equals(booking.getUserId()) && !userId.equals(booking.getProxyUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权取消该预约");
        }
        if (!"pending".equals(booking.getStatus()) && !"confirmed".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR);
        }
        booking.setStatus("cancelled");
        serviceBookingMapper.updateById(booking);
    }

    @Override
    public void updateStatus(Long bookingId, String status) {
        if (!Set.of("pending", "confirmed", "in_progress", "completed", "cancelled").contains(status)) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR);
        }
        ServiceBooking booking = requireBooking(bookingId);
        booking.setStatus(status);
        if ("completed".equals(status)) {
            booking.setCompleteTime(LocalDateTime.now());
        }
        serviceBookingMapper.updateById(booking);
    }

    @Override
    @Transactional
    public void claim(Long staffUserId, Long bookingId) {
        // 校验操作者身份为 staff
        User staff = userMapper.selectById(staffUserId);
        if (staff == null || !"staff".equals(staff.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有工作人员可以接取预约");
        }
        ServiceBooking booking = requireBooking(bookingId);
        // 社区隔离：仅在双方都配置了 communityId 时生效
        if (staff.getCommunityId() != null && booking.getCommunityId() != null
                && !staff.getCommunityId().equals(booking.getCommunityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能接取本社区的预约");
        }
        // 状态必须是 pending
        if (!"pending".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "只有待调度的预约可以接取");
        }
        // 使用乐观更新，确保只有第一个成功的工作人员能接取该预约
        int updated = serviceBookingMapper.update(null, new LambdaUpdateWrapper<ServiceBooking>()
                .eq(ServiceBooking::getBookingId, bookingId)
                .isNull(ServiceBooking::getStaffUserId)
                .eq(ServiceBooking::getStatus, "pending")
                .set(ServiceBooking::getStaffUserId, staffUserId)
                .set(ServiceBooking::getStatus, "confirmed"));
        if (updated == 0) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "该预约已被其他工作人员接取");
        }
        // 发送通知给居民
        noticeService.sendNotice(booking.getUserId(), "预约已被接取", "您的预约已被工作人员接取，工作人员将按时上门服务。", "booking", "booking", bookingId);
    }

    @Override
    @Transactional
    public void complete(Long staffUserId, Long bookingId, String feedback) {
        // 1. 获取当前登录用户（实际调用时 staffUserId 应该从 SecurityContext 获取，但为了接口兼容，传入参数作为当前用户ID）
        User operator = userMapper.selectById(staffUserId);
        if (operator == null || !"staff".equals(operator.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有工作人员可以完成预约");
        }
    
        ServiceBooking booking = requireBooking(bookingId);
    
        // 2. 权限校验：只能完成自己负责的预约
        if (!booking.getStaffUserId().equals(staffUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能完成自己负责的预约");
        }
    
        // 3. 状态校验：只有进行中（in_progress）的预约才能完成
        if (!"in_progress".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "只有进行中的预约才能完成");
        }
    
        // 4. 时间校验：当前时间必须晚于期望服务时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(booking.getExpectTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能在期望服务时间之前完成预约，请等待服务时间到达后再操作");
        }
    
        // 5. 更新预约状态
        booking.setStatus("completed");
        booking.setFeedback(feedback);
        booking.setCompleteTime(now);
        serviceBookingMapper.updateById(booking);
    
        // 6. 发送通知
        noticeService.sendNotice(booking.getUserId(), "服务已完成", 
            "您的预约服务已完成。" + (feedback != null ? "评价：" + feedback : ""), 
            "booking", "booking", bookingId);
    }

    @Override
    public void feedback(Long userId, Long bookingId, String feedback) {
        ServiceBooking booking = requireBooking(bookingId);
        if (!userId.equals(booking.getUserId()) && !userId.equals(booking.getProxyUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权评价该预约");
        }
        if (!"completed".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR);
        }
        booking.setFeedback(feedback);
        serviceBookingMapper.updateById(booking);
    }

    // ==================== 新增工作人员端方法 ====================

    @Override
    public Page<BookingVO> getStaffList(Long staffUserId, String status, Integer pageNum, Integer pageSize) {
        // 校验当前用户是否是工作人员（优先检查 user.role，兼容老数据再检查 user_role 表）
        User staff = userMapper.selectById(staffUserId);
        boolean isStaff = false;
        if (staff != null && "staff".equals(staff.getRole())) {
            isStaff = true;
        }
        if (!isStaff) {
            // fallback: check user_role mapping
            isStaff = userRoleMapper.selectList(new LambdaQueryWrapper<com.community.platform.entity.UserRole>()
                    .eq(com.community.platform.entity.UserRole::getUserId, staffUserId))
                    .stream().map(com.community.platform.entity.UserRole::getRoleCode)
                    .anyMatch(code -> "ROLE_STAFF".equals(code));
        }
        if (!isStaff) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看预约列表");
        }
        Long communityId = staff.getCommunityId();

        LambdaQueryWrapper<ServiceBooking> wrapper = new LambdaQueryWrapper<ServiceBooking>()
                .orderByDesc(ServiceBooking::getCreateTime);
        // 如果系统仅有单个社区，则不强制要求 staff.user.communityId，兼容未分配社区的用户
        if (communityId != null) {
            wrapper.eq(ServiceBooking::getCommunityId, communityId);
        }
        if (StringUtils.hasText(status)) {
            // pending: 未被任何工作人员接取的预约
            if ("pending".equals(status)) {
                wrapper.eq(ServiceBooking::getStatus, "pending");
                wrapper.isNull(ServiceBooking::getStaffUserId);
            } else {
                // 其它状态：只返回属于当前工作人员的记录
                wrapper.eq(ServiceBooking::getStatus, status);
                wrapper.eq(ServiceBooking::getStaffUserId, staffUserId);
            }
        } else {
            // 未提供状态时：返回未接取的 pending，以及当前工作人员自己的已接取记录
            wrapper.and(w -> w.eq(ServiceBooking::getStatus, "pending").isNull(ServiceBooking::getStaffUserId)
                    .or(e -> e.eq(ServiceBooking::getStaffUserId, staffUserId)));
        }

        Page<ServiceBooking> page = serviceBookingMapper.selectPage(
                new Page<>(page(pageNum), size(pageSize)), wrapper);
        Page<BookingVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toBookingVO).toList());
        return result;
    }

    @Override
    @Transactional
    public void startService(Long staffUserId, Long bookingId) {
        ServiceBooking booking = requireBooking(bookingId);
        if (!booking.getStaffUserId().equals(staffUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有负责该预约的工作人员才能开始服务");
        }
        if (!"confirmed".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "只有已派单状态的预约才能开始服务");
        }
        // 时间校验：当前时间必须晚于或等于期望服务时间（实际业务通常允许提前少量时间，但此处按需求）
        if (LocalDateTime.now().isBefore(booking.getExpectTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能在期望服务时间之前开始服务，请等待预约时间到达后再操作");
        }
        booking.setStatus("in_progress");
        serviceBookingMapper.updateById(booking);
        noticeService.sendNotice(booking.getUserId(), "服务开始", "您的预约服务已开始", "booking", "booking", bookingId);
    }

    // ==================== 私有辅助方法 ====================

    private ServiceBooking requireBooking(Long bookingId) {
        ServiceBooking booking = serviceBookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ResultCode.BOOKING_NOT_EXISTS);
        }
        return booking;
    }

    /**
     * 解析预约所属的社区ID
     */
    private Long resolveCommunityId(Long userId, Long profileId) {
        if (profileId != null) {
            ResidentProfile profile = residentProfileMapper.selectById(profileId);
            if (profile != null && profile.getCommunityId() != null) {
                return profile.getCommunityId();
            }
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getCommunityId();
    }

    private BookingVO toBookingVO(ServiceBooking booking) {
        BookingVO vo = new BookingVO();
        vo.setBookingId(booking.getBookingId());
        vo.setServiceType(booking.getServiceType());
        vo.setServiceTypeLabel(serviceTypeLabel(booking.getServiceType()));
        vo.setExpectTime(booking.getExpectTime());
        vo.setStatus(booking.getStatus());
        vo.setStatusLabel(statusLabel(booking.getStatus()));
        vo.setAddress(booking.getAddress());
        vo.setRemark(booking.getRemark());
        vo.setCreateTime(booking.getCreateTime());
        vo.setFeedback(booking.getFeedback());
        // 可选：补充居民姓名等

        if (booking.getStaffUserId() != null) {
            User staff = userMapper.selectById(booking.getStaffUserId());
            if (staff != null) {
                vo.setStaffName(staff.getUsername());
                vo.setStaffPhone(staff.getPhone());
            }
        }
        // 代办信息
        vo.setIsProxy(booking.getProxyUserId() != null);
        if (booking.getProxyUserId() != null) {
            User proxy = userMapper.selectById(booking.getProxyUserId());
            if (proxy != null) {
                vo.setProxyUserName(proxy.getUsername());
            }
        }
        return vo;
    }

    private String serviceTypeLabel(String serviceType) {
        return switch (serviceType) {
            case "dining" -> "助餐服务";
            case "accompany" -> "陪诊服务";
            case "home_visit" -> "上门服务";
            default -> serviceType;
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "待确认";
            case "confirmed" -> "已确认";
            case "in_progress" -> "服务中";
            case "completed" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    private long page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long size(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}