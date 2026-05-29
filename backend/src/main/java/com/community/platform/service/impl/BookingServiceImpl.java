package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.service.BookingService;
import com.community.platform.service.NoticeService;
import com.community.platform.vo.booking.BookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<String> SERVICE_TYPES = Set.of("dining", "accompany", "home_visit");

    private final ServiceBookingMapper serviceBookingMapper;
    private final NoticeService noticeService;

    @Override
    @Transactional
    public Long create(Long userId, BookingDTO dto) {
        if (!SERVICE_TYPES.contains(dto.getServiceType())) {
            throw new BusinessException(ResultCode.SERVICE_NOT_AVAILABLE);
        }
        long conflicts = serviceBookingMapper.selectCount(new LambdaQueryWrapper<ServiceBooking>()
                .eq(ServiceBooking::getUserId, userId)
                .eq(ServiceBooking::getServiceType, dto.getServiceType())
                .eq(ServiceBooking::getExpectTime, dto.getExpectTime())
                .in(ServiceBooking::getStatus, "pending", "confirmed", "in_progress"));
        if (conflicts > 0) {
            throw new BusinessException(ResultCode.TIME_CONFLICT);
        }

        ServiceBooking booking = new ServiceBooking();
        booking.setUserId(userId);
        booking.setServiceType(dto.getServiceType());
        booking.setExpectTime(dto.getExpectTime());
        booking.setAddress(dto.getAddress());
        booking.setRemark(dto.getRemark());
        booking.setStatus("pending");
        serviceBookingMapper.insert(booking);

        noticeService.sendNotice(userId, "预约已提交", "您的社区服务预约已提交，等待确认。", "booking", "booking", booking.getBookingId());
        return booking.getBookingId();
    }

    @Override
    public Page<BookingVO> getList(Long userId, Integer pageNum, Integer pageSize) {
        Page<ServiceBooking> page = serviceBookingMapper.selectPage(
                new Page<>(page(pageNum), size(pageSize)),
                new LambdaQueryWrapper<ServiceBooking>()
                        .eq(ServiceBooking::getUserId, userId)
                        .orderByDesc(ServiceBooking::getCreateTime));
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
    public void assign(Long bookingId, Long staffUserId) {
        ServiceBooking booking = requireBooking(bookingId);
        booking.setStaffUserId(staffUserId);
        booking.setStatus("confirmed");
        serviceBookingMapper.updateById(booking);
        noticeService.sendNotice(booking.getUserId(), "预约已确认", "您的社区服务预约已分配工作人员。", "booking", "booking", bookingId);
    }

    @Override
    public void complete(Long bookingId, String feedback) {
        ServiceBooking booking = requireBooking(bookingId);
        booking.setStatus("completed");
        booking.setFeedback(feedback);
        booking.setCompleteTime(LocalDateTime.now());
        serviceBookingMapper.updateById(booking);
        noticeService.sendNotice(booking.getUserId(), "服务已完成", "您的社区服务预约已完成。", "booking", "booking", bookingId);
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

    private ServiceBooking requireBooking(Long bookingId) {
        ServiceBooking booking = serviceBookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ResultCode.BOOKING_NOT_EXISTS);
        }
        return booking;
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
