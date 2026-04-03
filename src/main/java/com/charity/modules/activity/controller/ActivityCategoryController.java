package com.charity.modules.activity.controller;

import com.charity.common.Result;
import com.charity.modules.activity.entity.ActivityCategory;
import com.charity.modules.activity.service.ActivityCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动分类控制器
 */
@Tag(name = "活动分类管理", description = "活动的分类查询与管理")
@RestController
@RequestMapping("/api/activity-categories")
public class ActivityCategoryController {

    @Autowired
    private ActivityCategoryService categoryService;

    @Operation(summary = "获取全部分类")
    @GetMapping
    public Result<List<ActivityCategory>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
