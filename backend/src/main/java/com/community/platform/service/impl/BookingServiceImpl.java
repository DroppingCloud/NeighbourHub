package com.community.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.service.BookingService;
import com.community.platform.vo.booking.BookingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    @Override
    public Long create(Long userId, BookingDTO dto) {
        // TODO: 校验服务可用性、校验时间冲突、保存预约单、发送确认通知
        throw new UnsupportedOperationException("TODO: 实现服务预约");
    }

    @Override
    public Page<BookingVO> getList(Long userId, Integer pageNum, Integer pageSize) {
        // TODO: 分页查询预约列表（仅本人）
        throw new UnsupportedOperationException("TODO: 实现预约列表查询");
    }

    @Override
    public BookingVO getDetail(Long userId, Long bookingId) {
        // TODO: 查询预约详情，校验数据权限
        throw new UnsupportedOperationException("TODO: 实现预约详情查询");
    }

    @Override
    public void cancel(Long userId, Long bookingId) {
        // TODO: 校验预约状态（只有 pending/confirmed 可取消）、更新状态
        throw new UnsupportedOperationException("TODO: 实现取消预约");
    }

    @Override
    public void updateStatus(Long bookingId, String status) {
        // TODO: 工作人员更新预约状态
        throw new UnsupportedOperationException("TODO: 实现更新预约状态");
    }
}
