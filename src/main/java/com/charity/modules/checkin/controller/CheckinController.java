package com.charity.modules.checkin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.common.Result;
import com.charity.modules.checkin.dto.CheckinDTO;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.service.CheckinService;
import com.charity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 签到控制器
 */
@Tag(name = "签到管理", description = "活动签到逻辑")
@RestController
@RequestMapping("/api/checkins")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    @Operation(summary = "提交签到")
    @PostMapping
    public Result<Void> checkin(@Valid @RequestBody CheckinDTO checkinDTO, HttpServletRequest request) {
        Long userId = SecurityUtils.getUserId();
        String ip = request.getRemoteAddr(); // 这里可以进一步优化获取真实IP
        checkinService.checkin(checkinDTO, userId, ip);
        return Result.success();
    }

    @Operation(summary = "获取当前用户特定活动的签到状态")
    @GetMapping("/status/{activityId}")
    public Result<ActivityCheckin> checkStatus(@PathVariable Long activityId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return Result.success(null);
        }
        ActivityCheckin checkin = checkinService.getOne(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activityId)
                .eq(ActivityCheckin::getUserId, userId)
                .last("LIMIT 1"));
        return Result.success(checkin);
    }

    @Operation(summary = "生成活动签到码 (管理端)")
    @GetMapping("/code/{activityId}")
    @PreAuthorize("hasAuthority('checkin:audit')")
    public Result<String> generateCheckinCode(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "5") Integer expireMinutes) {
        return Result.success(checkinService.generateCheckinCode(activityId, expireMinutes));
    }
}
