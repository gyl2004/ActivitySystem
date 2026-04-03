package com.charity.modules.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐配置表
 */
@Data
@TableName("recommendation_config")
public class RecommendationConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String algorithmType;
    private BigDecimal weight;
    private String params; // JSON string
    private Integer isEnabled;
    private LocalDateTime updateTime;
}
