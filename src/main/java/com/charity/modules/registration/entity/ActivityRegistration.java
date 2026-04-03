package com.charity.modules.registration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名表
 */
@Data
@TableName("activity_registration")
public class ActivityRegistration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private Integer status; // 0-待审核, 1-审核通过, 2-审核驳回, 3-已取消
    private String remark;
    private String auditRemark;
    private String customFields; // JSON string
    private Integer earnedPoints; // 实际获得积分
    private Double earnedDuration; // 实际获得时长
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
