package com.charity.modules.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.recommendation.entity.ActivityRecommendation;
import com.charity.modules.recommendation.entity.UserBehavior;
import com.charity.modules.recommendation.mapper.ActivityRecommendationMapper;
import com.charity.modules.recommendation.service.RecommendationService;
import com.charity.modules.recommendation.service.UserBehaviorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类 (简版实现，实际可接入更复杂的推荐引擎)
 */
@Slf4j
@Service
public class RecommendationServiceImpl extends ServiceImpl<ActivityRecommendationMapper, ActivityRecommendation> implements RecommendationService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Override
    public List<Activity> recommendForUser(Long userId, int limit) {
        log.info("开始为用户 {} 进行个性化推荐", userId);
        
        // 1. 获取用户近期互动的活动
        List<UserBehavior> myBehaviors = userBehaviorService.list(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .orderByDesc(UserBehavior::getBehaviorTime)
                .last("LIMIT 50"));
        
        if (myBehaviors.isEmpty()) {
            return recommendPopular(limit);
        }

        Set<Long> myActivityIds = myBehaviors.stream()
                .map(UserBehavior::getActivityId)
                .collect(Collectors.toSet());

        // 2. 简单的 ItemCF 逻辑：寻找与用户互动过的活动相似的活动
        // 这里的相似度基于共同被互动的用户数来衡量
        Map<Long, Double> scores = new HashMap<>();
        for (Long activityId : myActivityIds) {
            // 找出也互动过该活动的其他用户
            List<UserBehavior> others = userBehaviorService.list(new LambdaQueryWrapper<UserBehavior>()
                    .eq(UserBehavior::getActivityId, activityId)
                    .ne(UserBehavior::getUserId, userId));
            
            Set<Long> otherUserIds = others.stream()
                    .map(UserBehavior::getUserId)
                    .collect(Collectors.toSet());
            
            if (otherUserIds.isEmpty()) continue;

            // 找出这些用户还互动过哪些其他活动
            List<UserBehavior> candidates = userBehaviorService.list(new LambdaQueryWrapper<UserBehavior>()
                    .in(UserBehavior::getUserId, otherUserIds)
                    .notIn(UserBehavior::getActivityId, myActivityIds));
            
            for (UserBehavior candidate : candidates) {
                scores.put(candidate.getActivityId(), scores.getOrDefault(candidate.getActivityId(), 0.0) + 1.0);
            }
        }

        // 3. 按分数排序并返回
        List<Long> recommendedIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (recommendedIds.isEmpty()) {
            return recommendPopular(limit);
        }

        return activityService.listByIds(recommendedIds);
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
