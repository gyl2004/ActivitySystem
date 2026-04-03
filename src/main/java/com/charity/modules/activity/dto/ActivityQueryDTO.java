package com.charity.modules.activity.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动查询参数
 */
@Data
public class ActivityQueryDTO {
    private String title;
    private Long categoryId;
    private Integer status;
    private LocalDateTime startTimeBegin;
    private LocalDateTime startTimeEnd;
    private Long createUserId;
}
