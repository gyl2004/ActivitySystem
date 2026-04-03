package com.charity.modules.registration.vo;

import com.charity.modules.registration.entity.ActivityRegistration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationVO extends ActivityRegistration {
    private String nickname;
    private String avatar;
    private String activityTitle;
    private String activityCover;
    private Integer activityPoints; // 潜在积分奖励
    private Double activityDuration; // 潜在时长奖励
}
