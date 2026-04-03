package com.charity.modules.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评价回复DTO
 */
@Data
public class ReviewReplyDTO {
    @NotNull(message = "评价ID不能为空")
    private Long reviewId;
    @NotBlank(message = "回复内容不能为空")
    private String content;
    private Long parentId; // 默认0
}
