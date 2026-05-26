package com.community.platform.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.Notice;
import com.community.platform.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    @Override
    public Page<Notice> getList(Long userId, Integer pageNum, Integer pageSize) {
        // TODO: 分页查询通知列表
        throw new UnsupportedOperationException("TODO: 实现通知列表查询");
    }

    @Override
    public void markRead(Long userId, Long noticeId) {
        // TODO: 校验通知归属、更新 is_read = 1
        throw new UnsupportedOperationException("TODO: 实现标记已读");
    }

    @Override
    public void markAllRead(Long userId) {
        // TODO: 批量更新该用户所有未读通知
        throw new UnsupportedOperationException("TODO: 实现全部标记已读");
    }

    @Override
    public Long getUnreadCount(Long userId) {
        // TODO: 查询未读通知数量
        throw new UnsupportedOperationException("TODO: 实现获取未读数量");
    }

    @Override
    public void sendNotice(Long userId, String title, String content, String type, Long refId) {
        // TODO: 创建通知记录，后续可扩展推送能力
        throw new UnsupportedOperationException("TODO: 实现发送通知");
    }
}
