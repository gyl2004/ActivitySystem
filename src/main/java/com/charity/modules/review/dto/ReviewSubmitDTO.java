package com.charity.modules.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 评价提交DTO
 */
@Data
public class ReviewSubmitDTO {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;
    @NotBlank(message = "评价内容不能为空")
    private String content;
    private List<String> images;
    private List<String> tags;
}
