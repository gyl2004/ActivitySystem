package com.charity.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动评价表
 */
@Data
@TableName("activity_review")
public class ActivityReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private Integer rating;
    private String content;
    private String images; // JSON array string
    private String tags; // JSON array string
    private Integer likeCount;
    private Integer replyCount;
    private Integer status; // 0-待审核, 1-已通过, 2-已拒绝
    private String sentiment; // positive, negative, neutral
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
