package com.charity.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.mapper.SysPermissionMapper;
import com.charity.modules.sys.mapper.SysRoleMapper;
import com.charity.modules.sys.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

/**
 * 用户详情服务
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        List<String> permissions = permissionMapper.findPermissionKeysByUserId(user.getId());
        List<String> roles = roleMapper.findRoleKeysByUserId(user.getId());
        
        // 过滤掉可能存在的 null 或空权限
        HashSet<String> authorities = new HashSet<>();
        if (permissions != null) {
            for (String perm : permissions) {
                if (perm != null && !perm.trim().isEmpty()) {
                    authorities.add(perm);
                }
            }
        }
        
        // 添加角色权限 (带 ROLE_ 前缀以便于 hasRole 使用)
        if (roles != null) {
            for (String role : roles) {
                if (role != null && !role.trim().isEmpty()) {
                    authorities.add("ROLE_" + role);
                }
            }
        }

        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
