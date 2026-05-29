package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.entity.Notice;
import com.community.platform.mapper.NoticeMapper;
import com.community.platform.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public Page<Notice> getList(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .orderByAsc(Notice::getIsRead)
                .orderByDesc(Notice::getCreateTime);
        return noticeMapper.selectPage(new Page<>(page(pageNum), size(pageSize)), wrapper);
    }

    @Override
    public void markRead(Long userId, Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null || !userId.equals(notice.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if (notice.getIsRead() == null || notice.getIsRead() == 0) {
            notice.setIsRead(1);
            notice.setReadTime(LocalDateTime.now());
            noticeMapper.updateById(notice);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        noticeMapper.update(null, new LambdaUpdateWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0)
                .set(Notice::getIsRead, 1)
                .set(Notice::getReadTime, LocalDateTime.now()));
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return noticeMapper.selectCount(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0));
    }

    @Override
    public void sendNotice(Long userId, String title, String content, String type, Long refId) {
        sendNotice(userId, title, content, type, null, refId);
    }

    @Override
    public void sendNotice(Long userId, String title, String content, String type, String refType, Long refId) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setRefType(refType);
        notice.setRefId(refId);
        notice.setIsRead(0);
        noticeMapper.insert(notice);
    }

    private long page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long size(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
