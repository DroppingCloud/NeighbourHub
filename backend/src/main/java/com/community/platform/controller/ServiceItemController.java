package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.ServiceMaterialTemplate;
import com.community.platform.service.ServiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "事项查询")
@RestController
@RequestMapping("/api/service-item")
@RequiredArgsConstructor
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    @Operation(summary = "可办理事项列表")
    @GetMapping("/list")
    public Result<Page<ServiceItem>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "online") String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(serviceItemService.getList(category, status, pageNum, pageSize));
    }

    @Operation(summary = "事项详情")
    @GetMapping("/{id}")
    public Result<ServiceItem> detail(@PathVariable Long id) {
        return Result.success(serviceItemService.getDetail(id));
    }

    @Operation(summary = "事项材料模板")
    @GetMapping("/{id}/materials")
    public Result<List<ServiceMaterialTemplate>> materials(@PathVariable Long id) {
        return Result.success(serviceItemService.getMaterials(id));
    }
}
