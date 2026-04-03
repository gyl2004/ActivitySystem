package com.charity.modules.activity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivityCategorySaveDTO {
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Integer sort;

    private Integer status;
}
