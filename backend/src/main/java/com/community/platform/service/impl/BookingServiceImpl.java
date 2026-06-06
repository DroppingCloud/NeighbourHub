package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.entity.User;
import com.community.platform.entity.UserRole;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.UserRoleMapper;
import com.community.platform.service.BookingService;
import com.community.platform.service.NoticeService;
import com.community.platform.service.ProxyPermissionService;
import com.community.platform.vo.booking.BookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<String> SERVICE_TYPES = Set.of("dining", "accompany", "home_visit");

    private final ServiceBookingMapper serviceBookingMapper;
    private final NoticeService noticeService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final ProxyPermissionService proxyPermissionService;

    @Override
    @Transactional
    public Long create(Long userId, BookingDTO dto) {
        if (!SERVICE_TYPES.contains(dto.getServiceType())) {
            throw new BusinessException(ResultCode.SERVICE_NOT_AVAILABLE);
        }

        Long ownerUserId = proxyPermissionService.validateAndGetTargetUserId(userId, dto.getProxyFor(), "booking");
        Long targetProfileId = dto.getProxyFor() != null ? dto.getProxyFor() : dto.getProfileId();
        Long proxyUserId = dto.getProxyFor() == null ? null : userId;

        long conflicts = serviceBookingMapper.selectCount(new LambdaQueryWrapper<ServiceBooking>()
                .eq(ServiceBooking::getUserId, ownerUserId)
                .eq(ServiceBooking::getServiceType, dto.getServiceType())
                .eq(ServiceBooking::getExpectTime, dto.getExpectTime())
                .in(ServiceBooking::getStatus, "pending", "confirmed", "in_progress"));
        if (conflicts > 0) {
            throw new BusinessException(ResultCode.TIME_CONFLICT);
        }

        ServiceBooking booking = new ServiceBooking();
        booking.setUserId(ownerUserId);
        booking.setProfileId(targetProfileId);
        booking.setProxyUserId(proxyUserId);
        booking.setServiceType(dto.getServiceType());
        booking.setExpectTime(dto.getExpectTime());
        booking.setAddress(dto.getAddress());
        booking.setRemark(dto.getRemark());
        booking.setStatus("pending");
        booking.setCommunityId(resolveCommunityId(ownerUserId, targetProfileId));
        serviceBookingMapper.insert(booking);

        noticeService.sendNotice(ownerUserId, "预约已提交",
                "您的社区服务预约已提交，等待工作人员接取。",
                "booking", "booking", booking.getBookingId());
        return booking.getBookingId();
    }

    @Override
    public Page<BookingVO> getList(Long userId, Integer pageNum, Integer pageSize, Long proxyFor) {
        LambdaQueryWrapper<ServiceBooking> wrapper = new LambdaQueryWrapper<>();
        if (proxyFor != null) {
            Long targetUserId = proxyPermissionService.validateAndGetTargetUserId(userId, proxyFor, "query");
            wrapper.eq(ServiceBooking::getUserId, targetUserId)
                    .eq(ServiceBooking::getProxyUserId, userId);
        } else {
            wrapper.and(w -> w.eq(ServiceBooking::getUserId, userId)
                    .or()
                    .eq(ServiceBooking::getProxyUserId, userId));
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
    @Transactional
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
    @Transactional
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
        User staff = userMapper.selectById(staffUserId);
        if (!isBookingStaff(staffUserId, staff)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有服务预约工作人员可以接取预约");
        }
        ServiceBooking booking = requireBooking(bookingId);
        if (staff != null && staff.getCommunityId() != null && booking.getCommunityId() != null
                && !staff.getCommunityId().equals(booking.getCommunityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能接取本社区的预约");
        }
        if (!"pending".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "只有待调度的预约可以接取");
        }

        int updated = serviceBookingMapper.update(null, new LambdaUpdateWrapper<ServiceBooking>()
                .eq(ServiceBooking::getBookingId, bookingId)
                .isNull(ServiceBooking::getStaffUserId)
                .eq(ServiceBooking::getStatus, "pending")
                .set(ServiceBooking::getStaffUserId, staffUserId)
                .set(ServiceBooking::getStatus, "confirmed"));
        if (updated == 0) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "该预约已被其他工作人员接取");
        }

        noticeService.sendNotice(booking.getUserId(), "预约已被接取",
                "您的预约已被工作人员接取，工作人员将按时上门服务。",
                "booking", "booking", bookingId);
    }

    @Override
    @Transactional
    public void complete(Long staffUserId, Long bookingId, String feedback) {
        User operator = userMapper.selectById(staffUserId);
        if (!isBookingStaff(staffUserId, operator)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有服务预约工作人员可以完成预约");
        }

        ServiceBooking booking = requireBooking(bookingId);
        if (!staffUserId.equals(booking.getStaffUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能完成自己负责的预约");
        }
        if (!"in_progress".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "只有进行中的预约才能完成");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(booking.getExpectTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能在期望服务时间之前完成预约");
        }

        booking.setStatus("completed");
        booking.setFeedback(feedback);
        booking.setCompleteTime(now);
        serviceBookingMapper.updateById(booking);

        noticeService.sendNotice(booking.getUserId(), "服务已完成",
                "您的预约服务已完成。" + (StringUtils.hasText(feedback) ? "评价：" + feedback : ""),
                "booking", "booking", bookingId);
    }

    @Override
    @Transactional
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

    @Override
    public Page<BookingVO> getStaffList(Long staffUserId, String status, Integer pageNum, Integer pageSize) {
        return getStaffList(staffUserId, null, status, pageNum, pageSize);
    }

    @Override
    public Page<BookingVO> getStaffList(Long staffUserId, Long targetStaffUserId, String status, Integer pageNum, Integer pageSize) {
        User staff = userMapper.selectById(staffUserId);
        boolean admin = isAdmin(staffUserId);
        if (!admin && !isBookingStaff(staffUserId, staff)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看预约列表");
        }
        Long queryStaffId = admin && targetStaffUserId != null ? targetStaffUserId : staffUserId;
        Long staffCommunityId = staff == null ? null : staff.getCommunityId();

        LambdaQueryWrapper<ServiceBooking> wrapper = new LambdaQueryWrapper<ServiceBooking>()
                .orderByDesc(ServiceBooking::getCreateTime);

        if (StringUtils.hasText(status)) {
            if ("pending".equals(status)) {
                wrapper.eq(ServiceBooking::getStatus, "pending")
                        .isNull(ServiceBooking::getStaffUserId);
                if (!admin && staffCommunityId != null) {
                    wrapper.and(w -> w.eq(ServiceBooking::getCommunityId, staffCommunityId)
                            .or()
                            .isNull(ServiceBooking::getCommunityId));
                }
            } else {
                wrapper.eq(ServiceBooking::getStatus, status)
                        .eq(ServiceBooking::getStaffUserId, queryStaffId);
            }
        } else {
            if (admin && targetStaffUserId != null) {
                wrapper.eq(ServiceBooking::getStaffUserId, queryStaffId);
            } else {
                wrapper.and(w -> w.nested(open -> {
                            open.eq(ServiceBooking::getStatus, "pending")
                                    .isNull(ServiceBooking::getStaffUserId);
                            if (!admin && staffCommunityId != null) {
                                open.and(scope -> scope.eq(ServiceBooking::getCommunityId, staffCommunityId)
                                        .or()
                                        .isNull(ServiceBooking::getCommunityId));
                            }
                        })
                        .or(e -> e.eq(ServiceBooking::getStaffUserId, queryStaffId)));
            }
        }

        Page<ServiceBooking> page = serviceBookingMapper.selectPage(new Page<>(page(pageNum), size(pageSize)), wrapper);
        Page<BookingVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toBookingVO).toList());
        return result;
    }

    @Override
    @Transactional
    public void startService(Long staffUserId, Long bookingId) {
        ServiceBooking booking = requireBooking(bookingId);
        if (!staffUserId.equals(booking.getStaffUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有负责该预约的工作人员才能开始服务");
        }
        if (!"confirmed".equals(booking.getStatus())) {
            throw new BusinessException(ResultCode.BOOKING_STATUS_ERROR, "只有已确认的预约才能开始服务");
        }
        if (LocalDateTime.now().isBefore(booking.getExpectTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能在期望服务时间之前开始服务");
        }
        booking.setStatus("in_progress");
        serviceBookingMapper.updateById(booking);
        noticeService.sendNotice(booking.getUserId(), "服务开始",
                "您的预约服务已开始。",
                "booking", "booking", bookingId);
    }

    private ServiceBooking requireBooking(Long bookingId) {
        ServiceBooking booking = serviceBookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ResultCode.BOOKING_NOT_EXISTS);
        }
        return booking;
    }

    private boolean isBookingStaff(Long userId, User user) {
        if (user != null && "staff".equals(user.getRole()) && "booking".equals(user.getStaffType())) {
            return true;
        }
        boolean hasStaffRole = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleCode)
                .anyMatch("ROLE_STAFF"::equals);
        return hasStaffRole && user != null && "booking".equals(user.getStaffType());
    }

    private boolean isAdmin(Long userId) {
        if (userId != null && userId == 0L) {
            return true;
        }
        User user = userMapper.selectById(userId);
        if (user != null && "admin".equals(user.getRole())) {
            return true;
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream()
                .map(UserRole::getRoleCode)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Long resolveCommunityId(Long userId, Long profileId) {
        if (profileId != null) {
            ResidentProfile profile = residentProfileMapper.selectById(profileId);
            if (profile != null && profile.getCommunityId() != null) {
                return profile.getCommunityId();
            }
        }
        User user = userMapper.selectById(userId);
        if (user != null && user.getCommunityId() != null) {
            return user.getCommunityId();
        }
        ResidentProfile profile = residentProfileMapper.selectOne(new LambdaQueryWrapper<ResidentProfile>()
                .eq(ResidentProfile::getUserId, userId)
                .last("LIMIT 1"));
        return profile == null ? null : profile.getCommunityId();
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

        if (booking.getStaffUserId() != null) {
            User staff = userMapper.selectById(booking.getStaffUserId());
            if (staff != null) {
                vo.setStaffName(staff.getUsername());
                vo.setStaffPhone(staff.getPhone());
            }
        }
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
            case "pending" -> "待接取";
            case "confirmed" -> "已接取";
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
