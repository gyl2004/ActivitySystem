package com.charity.modules.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户行为表
 */
@Data
@TableName("user_behavior")
public class UserBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private String behaviorType; // view, register, checkin, review, share
    private BigDecimal weight;
    private LocalDateTime behaviorTime;
}
