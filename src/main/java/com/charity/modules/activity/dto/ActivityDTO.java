package com.charity.modules.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动数据传输对象
 */
@Data
public class ActivityDTO {
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    @NotNull(message = "报名开始时间不能为空")
    private LocalDateTime registrationStart;
    @NotNull(message = "报名结束时间不能为空")
    private LocalDateTime registrationEnd;
    private String locationName;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer maxParticipants;
    private Integer points;
    private Double volunteerDuration;
}
