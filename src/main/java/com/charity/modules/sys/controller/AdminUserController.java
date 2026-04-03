package com.charity.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.common.AppException;
import com.charity.common.Result;
import com.charity.modules.sys.dto.AssignRoleDTO;
import com.charity.modules.sys.entity.SysRole;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.entity.SysUserRole;
import com.charity.modules.sys.mapper.SysRoleMapper;
import com.charity.modules.sys.mapper.SysUserMapper;
import com.charity.modules.sys.mapper.SysUserRoleMapper;
import com.charity.modules.sys.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "用户管理(后台)", description = "后台用户列表与角色分配")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<IPage<AdminUserVO>> findPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .orderByDesc(SysUser::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword));
        }
        IPage<SysUser> userPage = userMapper.selectPage(page, wrapper);
        Page<AdminUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (userPage.getRecords().isEmpty()) {
            voPage.setRecords(List.of());
            return Result.success(voPage);
        }

        List<Long> userIds = userPage.getRecords().stream().map(SysUser::getId).toList();
        List<SysUserRole> rels = userRoleMapper.listByUserIds(userIds);
        Map<Long, List<Long>> userRoleIds = rels.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId, Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
        Set<Long> roleIds = rels.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());

        Map<Long, SysRole> roleMap = roleIds.isEmpty()
                ? new HashMap<>()
                : roleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(SysRole::getId, Function.identity(), (a, b) -> a));

        List<AdminUserVO> voList = userPage.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setStatus(u.getStatus());
            vo.setPoints(u.getPoints());
            vo.setVolunteerDuration(u.getVolunteerDuration());
            vo.setCreateTime(u.getCreateTime());

            List<Long> rids = userRoleIds.getOrDefault(u.getId(), List.of());
            List<String> roleKeys = rids.stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .map(SysRole::getRoleKey)
                    .toList();
            vo.setRoleKeys(roleKeys);

            Long mainRoleId = rids.isEmpty() ? null : rids.get(0);
            SysRole role = mainRoleId == null ? null : roleMap.get(mainRoleId);
            if (role != null) {
                vo.setRoleId(role.getId());
                vo.setRoleName(role.getRoleName());
                vo.setRoleKey(role.getRoleKey());
            }
            return vo;
        }).toList();
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @Operation(summary = "分配用户角色(单角色)")
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('user:role:assign')")
    public Result<Void> assignRole(@PathVariable Long userId, @Valid @RequestBody AssignRoleDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new AppException("用户不存在");
        }
        SysRole role = roleMapper.selectById(dto.getRoleId());
        if (role == null || role.getDeleted() != null && role.getDeleted() == 1 || role.getStatus() != null && role.getStatus() == 0) {
            throw new AppException("角色不可用");
        }

        // 业务限制：超级管理员只能赋予管理员或志愿者身份
        if (!"admin".equals(role.getRoleKey()) && !"volunteer".equals(role.getRoleKey())) {
            throw new AppException("超级管理员只能赋予管理员或志愿者身份");
        }

        userRoleMapper.deleteByUserId(userId);
        userRoleMapper.insertRole(userId, dto.getRoleId());
        return Result.success();
    }
}
