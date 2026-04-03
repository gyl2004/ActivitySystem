package com.charity.modules.checkin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckinVO {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private Long userId;
    private String nickname;
    private String avatar;
    private LocalDateTime checkinTime;
    private Integer checkinType;
    private String ipAddress;
}
