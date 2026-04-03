package com.charity.modules.activity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.common.AppException;
import com.charity.common.Result;
import com.charity.modules.activity.dto.ActivityCategorySaveDTO;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.entity.ActivityCategory;
import com.charity.modules.activity.service.ActivityCategoryService;
import com.charity.modules.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "活动分类管理(后台)", description = "活动分类的CRUD与状态管理")
@RestController
@RequestMapping("/api/admin/activity-categories")
public class AdminActivityCategoryController {

    @Autowired
    private ActivityCategoryService categoryService;

    @Autowired
    private ActivityService activityService;

    @Operation(summary = "获取全部分类(含禁用)")
    @GetMapping
    @PreAuthorize("hasAuthority('category:manage')")
    public Result<List<ActivityCategory>> listAll() {
        return Result.success(categoryService.list(new LambdaQueryWrapper<ActivityCategory>()
                .orderByAsc(ActivityCategory::getSort, ActivityCategory::getId)));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    @PreAuthorize("hasAuthority('category:create')")
    public Result<Void> create(@Valid @RequestBody ActivityCategorySaveDTO dto) {
        ActivityCategory category = new ActivityCategory();
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setCreateTime(LocalDateTime.now());
        categoryService.save(category);
        return Result.success();
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('category:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityCategorySaveDTO dto) {
        ActivityCategory category = categoryService.getById(id);
        if (category == null) {
            throw new AppException("分类不存在");
        }
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        categoryService.updateById(category);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('category:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        ActivityCategory category = categoryService.getById(id);
        if (category == null) {
            throw new AppException("分类不存在");
        }
        Long count = activityService.count(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCategoryId, id));
        if (count != null && count > 0) {
            throw new AppException("该分类下存在活动，无法删除");
        }
        categoryService.removeById(id);
        return Result.success();
    }
}
