package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.vo.booking.BookingVO;

public interface BookingService {
    Long create(Long userId, BookingDTO dto);
    Page<BookingVO> getList(Long userId, Integer pageNum, Integer pageSize, Long proxyFor);
    BookingVO getDetail(Long userId, Long bookingId);
    void cancel(Long userId, Long bookingId);
    void updateStatus(Long bookingId, String status);
    /**
     * 工作人员接取预约（claim）：某个工作人员对某个未被接取的预约发起接取请求
     */
    void claim(Long staffUserId, Long bookingId);
    void complete(Long staffUserId, Long bookingId, String feedback);
    void feedback(Long userId, Long bookingId, String feedback);
    Page<BookingVO> getStaffList(Long staffUserId, String status, Integer pageNum, Integer pageSize);
    void startService(Long staffUserId, Long bookingId);
}
