package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.entity.Notice;
import com.community.platform.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息通知")
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "通知列表")
    @GetMapping("/list")
    public Result<Page<Notice>> list(@AuthenticationPrincipal Long userId,
                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(noticeService.getList(userId, pageNum, pageSize));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        noticeService.markRead(userId, id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        noticeService.markAllRead(userId);
        return Result.success();
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal Long userId) {
        return Result.success(noticeService.getUnreadCount(userId));
    }
}
