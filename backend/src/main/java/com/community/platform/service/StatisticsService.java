package com.community.platform.service;

import com.community.platform.vo.statistics.StatisticsVO;

import java.util.List;
import java.util.Map;

public interface StatisticsService {
    StatisticsVO getOverview();
    List<Map<String, Object>> getItemStats();
    List<Map<String, Object>> getServiceStats();
}
