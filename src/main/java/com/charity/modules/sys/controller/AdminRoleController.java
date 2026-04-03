package com.charity.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.common.Result;
import com.charity.modules.sys.entity.SysRole;
import com.charity.modules.sys.mapper.SysRoleMapper;
import com.charity.modules.sys.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "角色管理(后台)", description = "后台角色列表查询")
@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    @Autowired
    private SysRoleMapper roleMapper;

    @Operation(summary = "获取角色列表")
    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<List<RoleVO>> listRoles() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
        
        // 业务限制：如果是超级管理员查询角色列表，可能需要过滤（根据需求：超级管理员只能赋予管理员身份）
        // 这里为了前端展示一致性，返回所有角色，但在分配时拦截
        
        List<RoleVO> voList = roles.stream().map(r -> {
            RoleVO vo = new RoleVO();
            vo.setId(r.getId());
            vo.setRoleName(r.getRoleName());
            vo.setRoleKey(r.getRoleKey());
            vo.setStatus(r.getStatus());
            return vo;
        }).toList();
        return Result.success(voList);
    }
}
