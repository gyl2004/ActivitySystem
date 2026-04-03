package com.charity.modules.statistics.service;

import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取总体统计数据
     */
    Map<String, Object> getOverallStats();
    
    /**
     * 获取某个活动的详细统计
     */
    Map<String, Object> getActivityStats(Long activityId);
}
