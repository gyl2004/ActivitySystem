package com.charity.modules.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.entity.ActivityCategory;
import com.charity.modules.activity.mapper.ActivityMapper;
import com.charity.modules.activity.service.ActivityCategoryService;
import com.charity.modules.checkin.mapper.ActivityCheckinMapper;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.mapper.ActivityRegistrationMapper;
import com.charity.modules.statistics.service.StatisticsService;
import com.charity.modules.sys.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ActivityCategoryService categoryService;

    @Override
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActivities", activityMapper.selectCount(null));
        stats.put("totalRegistrations", registrationMapper.selectCount(null));
        stats.put("totalCheckins", checkinMapper.selectCount(null));
        stats.put("totalUsers", userMapper.selectCount(null));
        
        // 近7天报名趋势
        stats.put("registrationTrend", getRegistrationTrend());
        
        // 活动分类占比
        stats.put("categoryDistribution", getCategoryDistribution());
        
        return stats;
    }

    private Map<String, Object> getRegistrationTrend() {
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));
            
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            Long count = registrationMapper.selectCount(new LambdaQueryWrapper<ActivityRegistration>()
                    .between(ActivityRegistration::getCreateTime, startOfDay, endOfDay));
            counts.add(count);
        }
        
        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("counts", counts);
        return trend;
    }

    private List<Map<String, Object>> getCategoryDistribution() {
        List<ActivityCategory> categories = categoryService.list();
        List<Activity> activities = activityMapper.selectList(null);
        
        Map<Long, Long> categoryCounts = activities.stream()
                .collect(Collectors.groupingBy(Activity::getCategoryId, Collectors.counting()));
        
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (ActivityCategory category : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", category.getName());
            item.put("value", categoryCounts.getOrDefault(category.getId(), 0L));
            distribution.add(item);
        }
        return distribution;
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
