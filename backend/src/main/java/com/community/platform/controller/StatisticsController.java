package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.service.StatisticsService;
import com.community.platform.vo.statistics.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "统计分析")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "统计概览")
    @GetMapping("/overview")
    public Result<StatisticsVO> overview() {
        return Result.success(statisticsService.getOverview());
    }

    @Operation(summary = "事项统计")
    @GetMapping("/items")
    public Result<List<Map<String, Object>>> itemStats() {
        return Result.success(statisticsService.getItemStats());
    }

    @Operation(summary = "服务统计")
    @GetMapping("/services")
    public Result<List<Map<String, Object>>> serviceStats() {
        return Result.success(statisticsService.getServiceStats());
    }
}
