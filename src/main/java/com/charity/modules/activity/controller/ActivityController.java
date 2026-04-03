package com.charity.modules.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.common.Result;
import com.charity.modules.activity.dto.ActivityDTO;
import com.charity.modules.activity.dto.ActivityQueryDTO;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 活动控制器
 */
@Tag(name = "活动管理", description = "活动的CRUD与状态管理")
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Operation(summary = "分页查询活动")
    @GetMapping
    public Result<IPage<Activity>> findPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            ActivityQueryDTO queryDTO) {
        Page<Activity> page = new Page<>(current, size);
        return Result.success(activityService.findPage(page, queryDTO));
    }

    @Operation(summary = "获取活动详情")
    @GetMapping("/{id}")
    public Result<Activity> getById(@PathVariable Long id) {
        return Result.success(activityService.getById(id));
    }

    @Operation(summary = "创建活动")
    @PostMapping
    @PreAuthorize("hasAuthority('activity:create')")
    public Result<Void> create(@Valid @RequestBody ActivityDTO activityDTO) {
        Long userId = SecurityUtils.getUserId();
        activityService.createActivity(activityDTO, userId);
        return Result.success();
    }

    @Operation(summary = "更新活动")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('activity:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityDTO activityDTO) {
        activityService.updateActivity(id, activityDTO);
        return Result.success();
    }

    @Operation(summary = "删除活动")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('activity:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "发布活动")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('activity:publish')")
    public Result<Void> publish(@PathVariable Long id) {
        activityService.publishActivity(id);
        return Result.success();
    }

    @Operation(summary = "取消活动")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('activity:update')")
    public Result<Void> cancel(@PathVariable Long id) {
        activityService.cancelActivity(id);
        return Result.success();
    }

    @Operation(summary = "退回草稿")
    @PutMapping("/{id}/revert")
    @PreAuthorize("hasAuthority('activity:update')")
    public Result<Void> revert(@PathVariable Long id) {
        activityService.revertToDraft(id);
        return Result.success();
    }
}
