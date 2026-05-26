package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.entity.ServiceItem;
import com.community.platform.service.ServiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ServiceItemService serviceItemService;

    @Operation(summary = "事项列表")
    @GetMapping("/service-item/list")
    public Result<Page<ServiceItem>> listServiceItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(serviceItemService.getList(category, status, pageNum, pageSize));
    }

    @Operation(summary = "创建事项")
    @PostMapping("/service-item")
    public Result<Long> createServiceItem(@RequestBody ServiceItem serviceItem) {
        return Result.success(serviceItemService.create(serviceItem));
    }

    @Operation(summary = "更新事项")
    @PutMapping("/service-item/{id}")
    public Result<Void> updateServiceItem(@PathVariable Long id, @RequestBody ServiceItem serviceItem) {
        serviceItem.setItemId(id);
        serviceItemService.update(serviceItem);
        return Result.success();
    }

    @Operation(summary = "删除事项")
    @DeleteMapping("/service-item/{id}")
    public Result<Void> deleteServiceItem(@PathVariable Long id) {
        serviceItemService.delete(id);
        return Result.success();
    }
}
