package com.charity.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价点赞表
 */
@Data
@TableName("activity_review_like")
public class ActivityReviewLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long userId;
    private Integer type; // 1-点赞, 2-点踩
    private LocalDateTime createTime;
}
