package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.mapper.ApplicationFormMapper;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.service.StatisticsService;
import com.community.platform.vo.statistics.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ApplicationFormMapper applicationFormMapper;
    private final ServiceBookingMapper serviceBookingMapper;

    @Override
    public StatisticsVO getOverview() {
        StatisticsVO vo = new StatisticsVO();
        vo.setTotalApplications(countApplications(null));
        vo.setPendingCount(countApplications("pending"));
        vo.setApprovedCount(countApplications("approved"));
        vo.setRejectedCount(countApplications("rejected"));
        vo.setSupplementCount(countApplications("supplement_required"));
        vo.setCompletedCount(countApplications("completed"));
        vo.setServiceBookingCount(serviceBookingMapper.selectCount(null));
        vo.setTopItems(getItemStats());
        vo.setDailyTrend(applicationFormMapper.selectMaps(new QueryWrapper<ApplicationForm>()
                .select("DATE(submit_time) AS day", "COUNT(*) AS count")
                .ge("submit_time", LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay())
                .groupBy("DATE(submit_time)")
                .orderByAsc("day")));
        return vo;
    }

    @Override
    public List<Map<String, Object>> getItemStats() {
        return applicationFormMapper.selectMaps(new QueryWrapper<ApplicationForm>()
                .select("item_id AS itemId", "COUNT(*) AS count")
                .groupBy("item_id")
                .orderByDesc("count")
                .last("LIMIT 10"));
    }

    @Override
    public List<Map<String, Object>> getServiceStats() {
        return serviceBookingMapper.selectMaps(new QueryWrapper<ServiceBooking>()
                .select("service_type AS serviceType", "status", "COUNT(*) AS count")
                .groupBy("service_type", "status")
                .orderByAsc("service_type")
                .orderByAsc("status"));
    }

    private Long countApplications(String status) {
        return applicationFormMapper.selectCount(new LambdaQueryWrapper<ApplicationForm>()
                .eq(status != null, ApplicationForm::getStatus, status));
    }
}
