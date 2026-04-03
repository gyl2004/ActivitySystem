package com.charity.modules.activity.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动表
 */
@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private Integer status; // 0-草稿, 1-待发布, 2-已发布, 3-进行中, 4-已结束, 5-已取消
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private String locationName;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer maxParticipants;
    private Integer points;
    private Double volunteerDuration;
    private Integer registeredCount;
    private Integer viewCount;
    private Integer shareCount;
    @TableLogic
    private Integer deleted;
    private Long createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
