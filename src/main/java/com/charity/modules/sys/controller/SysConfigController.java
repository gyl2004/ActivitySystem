package com.charity.modules.sys.controller;

import com.charity.common.Result;
import com.charity.modules.sys.entity.SysConfig;
import com.charity.modules.sys.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置管理", description = "系统参数配置")
@RestController
@RequestMapping("/api/configs")
public class SysConfigController {

    @Autowired
    private SysConfigService configService;

    @Operation(summary = "获取所有配置")
    @GetMapping
    @PreAuthorize("hasAuthority('statistics:manage')") // 使用现有的统计管理权限
    public Result<List<SysConfig>> list() {
        return Result.success(configService.list());
    }

    @Operation(summary = "更新配置")
    @PutMapping
    @PreAuthorize("hasAuthority('statistics:manage')")
    public Result<Void> update(@RequestBody SysConfig config) {
        configService.updateById(config);
        return Result.success();
    }
}
