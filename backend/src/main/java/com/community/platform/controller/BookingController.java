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
                               @Valid @RequestBody BookingDTO dto) {
        return Result.success(bookingService.create(userId, dto));
    }

    @Operation(summary = "预约列表")
    @GetMapping("/list")
    public Result<Page<BookingVO>> list(@AuthenticationPrincipal Long userId,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(bookingService.getList(userId, pageNum, pageSize));
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

    @Operation(summary = "分配服务人员")
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody BookingAssignDTO dto) {
        bookingService.assign(id, dto.getStaffUserId());
        return Result.success();
    }

    @Operation(summary = "完成预约服务")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, @RequestBody BookingFeedbackDTO dto) {
        bookingService.complete(id, dto.getFeedback());
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
}
