package com.charity.modules.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核请求
 */
@Data
public class AuditDTO {
    @NotNull(message = "审核状态不能为空")
    private Integer status; // 1-通过, 2-驳回
    private String auditRemark;
}
