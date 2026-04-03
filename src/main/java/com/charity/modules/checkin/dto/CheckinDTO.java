package com.charity.modules.checkin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 签到请求
 */
@Data
public class CheckinDTO {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer checkinType; // 1-输入签到码签到, 2-管理员手动签到
    private String token; // 签到码，用于验证
}
