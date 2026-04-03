package com.charity.modules.checkin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动签到表
 */
@Data
@TableName("activity_checkin")
public class ActivityCheckin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private LocalDateTime checkinTime;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer checkinType; // 1-扫描二维码, 2-管理员手动签到
    private String ipAddress;
}
