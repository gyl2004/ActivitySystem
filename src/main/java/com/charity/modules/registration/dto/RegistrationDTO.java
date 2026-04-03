package com.charity.modules.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报名请求
 */
@Data
public class RegistrationDTO {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    private String remark;
    private String customFields; // JSON format
}
