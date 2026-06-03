package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.dto.workorder.WorkOrderQueryDTO;
import com.community.platform.entity.WorkOrderLog;
import com.community.platform.service.WorkOrderService;
import com.community.platform.vo.workorder.WorkOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "工单管理")
@RestController
@RequestMapping("/api/workorder")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "工单列表")
    @GetMapping("/list")
    public Result<Page<WorkOrderVO>> list(WorkOrderQueryDTO query) {
        return Result.success(workOrderService.getList(query));
    }

    @Operation(summary = "工单统计")
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        return Result.success(workOrderService.getStats());
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public Result<WorkOrderVO> detail(@PathVariable Long id) {
        return Result.success(workOrderService.getDetail(id));
    }

    @Operation(summary = "审核工单")
    @PostMapping("/audit")
    public Result<Void> audit(@AuthenticationPrincipal Long userId,
                              @Valid @RequestBody WorkOrderAuditDTO dto) {
        workOrderService.audit(userId, dto);
        return Result.success();
    }

    @Operation(summary = "工单操作日志")
    @GetMapping("/{id}/logs")
    public Result<List<WorkOrderLog>> logs(@PathVariable Long id) {
        return Result.success(workOrderService.getLogs(id));
    }
}
