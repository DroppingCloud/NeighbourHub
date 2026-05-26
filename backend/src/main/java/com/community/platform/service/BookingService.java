package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.vo.booking.BookingVO;

public interface BookingService {
    Long create(Long userId, BookingDTO dto);
    Page<BookingVO> getList(Long userId, Integer pageNum, Integer pageSize);
    BookingVO getDetail(Long userId, Long bookingId);
    void cancel(Long userId, Long bookingId);
    void updateStatus(Long bookingId, String status);
}
