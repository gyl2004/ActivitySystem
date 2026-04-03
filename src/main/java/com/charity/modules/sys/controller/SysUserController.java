package com.charity.modules.sys.controller;

import com.charity.common.Result;
import com.charity.modules.sys.dto.UserProfileUpdateDTO;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.service.SysUserService;
import com.charity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户个人信息管理接口")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Validated @RequestBody UserProfileUpdateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        sysUserService.updateById(user);
        return Result.success();
    }

    @Operation(summary = "更新个人头像")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam String avatarUrl) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        sysUserService.updateById(user);
        return Result.success();
    }
}
