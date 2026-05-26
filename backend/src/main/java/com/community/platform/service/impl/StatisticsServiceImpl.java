package com.community.platform.service.impl;

import com.community.platform.service.StatisticsService;
import com.community.platform.vo.statistics.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Override
    public StatisticsVO getOverview() {
        // TODO: 查询总申请量、各状态数量、预约数量等汇总数据
        throw new UnsupportedOperationException("TODO: 实现统计概览");
    }

    @Override
    public List<Map<String, Object>> getItemStats() {
        // TODO: 统计各事项申请数量排名
        throw new UnsupportedOperationException("TODO: 实现事项统计");
    }

    @Override
    public List<Map<String, Object>> getServiceStats() {
        // TODO: 统计各类社区服务预约情况
        throw new UnsupportedOperationException("TODO: 实现服务统计");
    }
}
