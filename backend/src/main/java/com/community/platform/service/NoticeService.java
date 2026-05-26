package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.Notice;

public interface NoticeService {
    Page<Notice> getList(Long userId, Integer pageNum, Integer pageSize);
    void markRead(Long userId, Long noticeId);
    void markAllRead(Long userId);
    Long getUnreadCount(Long userId);
    void sendNotice(Long userId, String title, String content, String type, Long refId);
}
