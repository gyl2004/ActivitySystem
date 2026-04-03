package com.charity.modules.registration.dto;

import lombok.Data;

@Data
public class RegistrationQueryDTO {
    private Long activityId;
    private Long userId;
    private Integer status;
    private String nickname;
    private String activityTitle;
}
