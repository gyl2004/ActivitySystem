package com.charity.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价回复表
 */
@Data
@TableName("activity_review_reply")
public class ActivityReviewReply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long userId;
    private String content;
    private Long parentId; // 0 for top level reply, or id of another reply
    private LocalDateTime createTime;
}
