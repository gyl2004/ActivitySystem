package com.charity.modules.checkin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.common.Result;
import com.charity.modules.checkin.dto.ManualCheckinDTO;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.service.CheckinService;
import com.charity.modules.checkin.vo.CheckinVO;
import com.charity.modules.registration.vo.RegistrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "签到管理(后台)", description = "后台查询签到列表与手动签到")
@RestController
@RequestMapping("/api/admin/checkins")
public class AdminCheckinController {

    @Autowired
    private CheckinService checkinService;

    @Operation(summary = "分页查询签到列表")
    @GetMapping
    @PreAuthorize("hasAuthority('checkin:manage')")
    public Result<IPage<CheckinVO>> findPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long userId) {
        Page<ActivityCheckin> page = new Page<>(current, size);
        return Result.success(checkinService.findPage(page, activityId, userId));
    }

    @Operation(summary = "管理员手动签到")
    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('checkin:manage')")
    public Result<Void> manualCheckin(@Valid @RequestBody ManualCheckinDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        checkinService.manualCheckin(dto.getActivityId(), dto.getUserId(), ip);
        return Result.success();
    }

    @Operation(summary = "生成签到码")
    @PostMapping("/code")
    @PreAuthorize("hasAuthority('checkin:manage')")
    public Result<String> generateCode(@RequestParam Long activityId, @RequestParam(required = false) Integer expireMinutes) {
        return Result.success(checkinService.generateCheckinCode(activityId, expireMinutes));
    }

    @Operation(summary = "导出签到数据")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('checkin:manage')")
    public void export(jakarta.servlet.http.HttpServletResponse response, 
                       @RequestParam(required = false) Long activityId, 
                       @RequestParam(required = false) Long userId) {
        checkinService.exportCheckins(response, activityId, userId);
    }

    @Operation(summary = "获取待签到用户列表")
    @GetMapping("/pending-users")
    @PreAuthorize("hasAuthority('checkin:manage')")
    public Result<List<RegistrationVO>> getPendingUsers(@RequestParam Long activityId) {
        return Result.success(checkinService.findPendingCheckinUsers(activityId));
    }
}
