package com.charity.modules.recommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.recommendation.entity.UserBehavior;

import java.math.BigDecimal;

/**
 * 用户行为服务接口
 */
public interface UserBehaviorService extends IService<UserBehavior> {
    
    /**
     * 记录用户行为
     * @param userId 用户ID
     * @param activityId 活动ID
     * @param behaviorType 行为类型
     * @param weight 权重
     */
    void logBehavior(Long userId, Long activityId, String behaviorType, BigDecimal weight);
}
