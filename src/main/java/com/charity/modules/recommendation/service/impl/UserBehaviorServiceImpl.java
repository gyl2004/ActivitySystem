package com.charity.modules.recommendation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.modules.recommendation.entity.UserBehavior;
import com.charity.modules.recommendation.mapper.UserBehaviorMapper;
import com.charity.modules.recommendation.service.UserBehaviorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户行为服务实现类
 * 亮点：
 * 1. 异步处理行为日志，不影响主业务响应速度
 * 2. 实时更新 Redis 热度榜 (ZSet)，支持高效的热门推荐
 */
@Service
public class UserBehaviorServiceImpl extends ServiceImpl<UserBehaviorMapper, UserBehavior> implements UserBehaviorService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String POPULAR_KEY = "activity:popular:rank";

    @Override
    @Async // 异步记录行为，避免阻塞主流程
    public void logBehavior(Long userId, Long activityId, String behaviorType, BigDecimal weight) {
        // 1. 持久化行为日志到数据库 (用于后续离线算法分析)
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActivityId(activityId);
        behavior.setBehaviorType(behaviorType);
        behavior.setWeight(weight);
        behavior.setBehaviorTime(LocalDateTime.now());
        this.save(behavior);

        // 2. 实时更新 Redis 热门排行榜权重
        // 使用 ZSet 的 incrementScore 实现热度累加
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, activityId.toString(), weight.doubleValue());
    }
}
