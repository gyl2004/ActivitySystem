package com.charity.modules.sys.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleDTO {
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}
