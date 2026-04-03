package com.charity.modules.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐记录表
 */
@Data
@TableName("activity_recommendation")
public class ActivityRecommendation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private String algorithmType; // UserCF, ItemCF, Content, Hot
    private BigDecimal score;
    @TableField("`rank`")
    private Integer rank;
    private Integer isClicked;
    private Integer isRegistered;
    private LocalDateTime createTime;
}
