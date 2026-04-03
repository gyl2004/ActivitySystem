package com.charity.modules.recommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.recommendation.entity.ActivityRecommendation;

import java.util.List;

/**
 * 推荐服务接口
 */
public interface RecommendationService extends IService<ActivityRecommendation> {
    
    /**
     * 获取用户个性化推荐
     * @param userId 用户ID
     * @param limit 数量
     * @return 活动列表
     */
    List<Activity> recommendForUser(Long userId, int limit);
    
    /**
     * 获取相似活动推荐
     * @param activityId 当前活动ID
     * @param limit 数量
     * @return 相似活动列表
     */
    List<Activity> recommendSimilar(Long activityId, int limit);
    
    /**
     * 获取热门活动推荐
     * @param limit 数量
     * @return 热门活动列表
     */
    List<Activity> recommendPopular(int limit);
}
