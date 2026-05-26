package com.community.platform.vo.statistics;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 统计概览 VO
 */
@Data
public class StatisticsVO {

    private Long totalApplications;

    private Long pendingCount;

    private Long approvedCount;

    private Long rejectedCount;

    private Long supplementCount;

    private Long completedCount;

    private Long serviceBookingCount;

    /** 各事项申请数量 Top N */
    private List<Map<String, Object>> topItems;

    /** 近7天申请趋势 */
    private List<Map<String, Object>> dailyTrend;
}
