package com.charity.modules.review.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评价审核DTO
 */
@Data
public class ReviewAuditDTO {
    @NotNull(message = "审核状态不能为空")
    private Integer status; // 1-已通过, 2-已拒绝
}
