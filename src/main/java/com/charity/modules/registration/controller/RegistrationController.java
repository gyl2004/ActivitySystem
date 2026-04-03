package com.charity.modules.registration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.common.Result;
import com.charity.modules.registration.dto.AuditDTO;
import com.charity.modules.registration.dto.RegistrationDTO;
import com.charity.modules.registration.dto.RegistrationQueryDTO;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.service.RegistrationService;
import com.charity.util.SecurityUtils;
import com.charity.modules.registration.vo.RegistrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报名控制器
 */
@Tag(name = "报名管理", description = "用户报名与审核管理")
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Operation(summary = "获取报名状态统计 (管理端)")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('registration:audit')")
    public Result<Map<Integer, Long>> getStatistics() {
        return Result.success(registrationService.getStatusStats());
    }

    @Operation(summary = "提交报名")
    @PostMapping
    public Result<Void> register(@Valid @RequestBody RegistrationDTO registrationDTO) {
        Long userId = SecurityUtils.getUserId();
        registrationService.register(registrationDTO, userId);
        return Result.success();
    }

    @Operation(summary = "取消报名")
    @DeleteMapping("/{activityId}/cancel")
    public Result<Void> cancel(@PathVariable Long activityId) {
        Long userId = SecurityUtils.getUserId();
        registrationService.cancel(activityId, userId);
        return Result.success();
    }

    @Operation(summary = "分页查询所有报名记录 (管理端)")
    @GetMapping
    @PreAuthorize("hasAuthority('registration:audit')")
    public Result<IPage<RegistrationVO>> findPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long activityId) {
        Page<ActivityRegistration> page = new Page<>(current, size);
        return Result.success(registrationService.findPageWithDetails(page, status, activityId));
    }

    @Operation(summary = "审核报名")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('registration:audit')")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO auditDTO) {
        registrationService.audit(id, auditDTO);
        return Result.success();
    }

    @Operation(summary = "批量审核报名")
    @PutMapping("/batch/audit")
    @PreAuthorize("hasAuthority('registration:audit')")
    public Result<Void> batchAudit(@RequestParam List<Long> ids, @Valid @RequestBody AuditDTO auditDTO) {
        registrationService.batchAudit(ids, auditDTO);
        return Result.success();
    }

    @Operation(summary = "报名退款处理")
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('registration:audit')")
    public Result<Void> refund(@PathVariable Long id) {
        registrationService.refund(id);
        return Result.success();
    }

    @Operation(summary = "导出报名数据")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('registration:export')")
    public void export(HttpServletResponse response, RegistrationQueryDTO queryDTO) {
        registrationService.exportRegistrations(response, queryDTO);
    }

    @Operation(summary = "获取我的报名记录")
    @GetMapping("/my")
    public Result<List<RegistrationVO>> getMyRegistrations() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(registrationService.findMyRegistrations(userId));
    }

    @Operation(summary = "检查当前用户特定活动的报名状态")
    @GetMapping("/status/{activityId}")
    public Result<ActivityRegistration> checkStatus(@PathVariable Long activityId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return Result.success(null);
        }
        ActivityRegistration registration = registrationService.getOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .orderByDesc(ActivityRegistration::getCreateTime)
                .last("LIMIT 1"));
        return Result.success(registration);
    }

    @Operation(summary = "下载志愿者证书")
    @GetMapping("/{activityId}/certificate")
    public void downloadCertificate(HttpServletResponse response, @PathVariable Long activityId) {
        Long userId = SecurityUtils.getUserId();
        registrationService.downloadCertificate(response, activityId, userId);
    }
}
