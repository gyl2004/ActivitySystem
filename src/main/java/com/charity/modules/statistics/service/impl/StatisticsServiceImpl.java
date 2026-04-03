package com.charity.modules.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.modules.activity.mapper.ActivityMapper;
import com.charity.modules.checkin.mapper.ActivityCheckinMapper;
import com.charity.modules.registration.mapper.ActivityRegistrationMapper;
import com.charity.modules.statistics.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityRegistrationMapper registrationMapper;

    @Autowired
    private ActivityCheckinMapper checkinMapper;

    @Override
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActivities", activityMapper.selectCount(null));
        stats.put("totalRegistrations", registrationMapper.selectCount(null));
        stats.put("totalCheckins", checkinMapper.selectCount(null));
        return stats;
    }

    @Override
    public Map<String, Object> getActivityStats(Long activityId) {
        Map<String, Object> stats = new HashMap<>();
        
        Long registrationCount = registrationMapper.selectCount(new LambdaQueryWrapper<com.charity.modules.registration.entity.ActivityRegistration>()
                .eq(com.charity.modules.registration.entity.ActivityRegistration::getActivityId, activityId));
        Long checkinCount = checkinMapper.selectCount(new LambdaQueryWrapper<com.charity.modules.checkin.entity.ActivityCheckin>()
                .eq(com.charity.modules.checkin.entity.ActivityCheckin::getActivityId, activityId));

        stats.put("registrationCount", registrationCount);
        stats.put("checkinCount", checkinCount);
        stats.put("attendanceRate", registrationCount > 0 ? (double) checkinCount / registrationCount : 0);
        return stats;
    }
}
