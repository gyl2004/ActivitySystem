package com.charity.modules.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.recommendation.entity.ActivityRecommendation;
import com.charity.modules.recommendation.mapper.ActivityRecommendationMapper;
import com.charity.modules.recommendation.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类 (简版实现，实际可接入更复杂的推荐引擎)
 */
@Slf4j
@Service
public class RecommendationServiceImpl extends ServiceImpl<ActivityRecommendationMapper, ActivityRecommendation> implements RecommendationService {

    @Autowired
    private ActivityService activityService;

    @Override
    public List<Activity> recommendForUser(Long userId, int limit) {
        log.info("开始为用户 {} 进行个性化推荐", userId);
        // 这里应该是复杂的推荐算法逻辑 (UserCF/ItemCF/混合推荐)
        // 简单模拟逻辑：从推荐记录表中获取预先计算好的推荐结果
        List<ActivityRecommendation> recommendations = this.list(new LambdaQueryWrapper<ActivityRecommendation>()
                .eq(ActivityRecommendation::getUserId, userId)
                .orderByDesc(ActivityRecommendation::getScore)
                .last("LIMIT " + limit));

        if (recommendations.isEmpty()) {
            // 如果没有个性化推荐，则退而求其次推荐热门活动
            return recommendPopular(limit);
        }

        List<Long> activityIds = recommendations.stream()
                .map(ActivityRecommendation::getActivityId)
                .collect(Collectors.toList());
        return activityService.listByIds(activityIds);
    }

    @Override
    public List<Activity> recommendSimilar(Long activityId, int limit) {
        log.info("开始为活动 {} 推荐相似活动", activityId);
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return new ArrayList<>();
        }
        // 简单逻辑：基于同分类进行推荐
        return activityService.list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCategoryId, activity.getCategoryId())
                .ne(Activity::getId, activityId)
                .eq(Activity::getStatus, 2) // 必须是已发布状态
                .last("LIMIT " + limit));
    }

    @Override
    public List<Activity> recommendPopular(int limit) {
        log.info("开始推荐热门活动");
        // 基于浏览量和报名人数的热度计算
        return activityService.list(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, 2)
                .orderByDesc(Activity::getViewCount, Activity::getRegisteredCount)
                .last("LIMIT " + limit));
    }
}
