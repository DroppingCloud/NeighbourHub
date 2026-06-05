package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.dto.booking.BookingAssignDTO;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.dto.booking.BookingFeedbackDTO;
import com.community.platform.service.BookingService;
import com.community.platform.vo.booking.BookingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "服务预约")
@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "发起预约")
    @PostMapping("/create")
    public Result<Long> create(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody BookingDTO dto,
                               @RequestParam(value = "_proxyFor", required = false) Long proxyFor) {
        dto.setProxyFor(proxyFor);
        return Result.success(bookingService.create(userId, dto));
    }

    @Operation(summary = "预约列表")
    @GetMapping("/list")
    public Result<Page<BookingVO>> list(@AuthenticationPrincipal Long userId,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(value = "_proxyFor", required = false) Long proxyFor) {
        return Result.success(bookingService.getList(userId, pageNum, pageSize, proxyFor));
    }

    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public Result<BookingVO> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return Result.success(bookingService.getDetail(userId, id));
    }

    @Operation(summary = "取消预约")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        bookingService.cancel(userId, id);
        return Result.success();
    }

    @Operation(summary = "工作人员接取预约")
    @PutMapping("/{id}/claim")
    public Result<Void> claim(@AuthenticationPrincipal Long staffUserId,
                              @PathVariable Long id) {
        bookingService.claim(staffUserId, id);
        return Result.success();
    }

    @Operation(summary = "完成预约服务")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@AuthenticationPrincipal Long staffUserId,
                                 @PathVariable Long id,
                                 @RequestBody BookingFeedbackDTO dto) {
        bookingService.complete(staffUserId, id, dto.getFeedback());
        return Result.success();
    }

    @Operation(summary = "提交服务反馈")
    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@AuthenticationPrincipal Long userId,
                                 @PathVariable Long id,
                                 @RequestBody BookingFeedbackDTO dto) {
        bookingService.feedback(userId, id, dto.getFeedback());
        return Result.success();
    }

    @Operation(summary = "工作人员查看预约列表")
    @GetMapping("/staff/list")
    public Result<Page<BookingVO>> staffList(@AuthenticationPrincipal Long userId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        // 注意：userId 是当前登录用户ID，需确保该用户角色为 staff
        return Result.success(bookingService.getStaffList(userId, status, pageNum, pageSize));
    }

    @Operation(summary = "开始服务")
    @PutMapping("/{id}/start")
    public Result<Void> start(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        bookingService.startService(userId, id);
        return Result.success();
    }

}
