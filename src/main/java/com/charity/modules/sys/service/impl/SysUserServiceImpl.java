package com.charity.modules.sys.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.sys.dto.RegisterDTO;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.mapper.SysUserMapper;
import com.charity.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 系统用户服务实现类
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 1. 检查用户名是否存在
        Long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new AppException("用户名已存在");
        }
        
        // 2. 检查邮箱是否存在
        if (registerDTO.getEmail() != null) {
            Long emailCount = this.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, registerDTO.getEmail()));
            if (emailCount > 0) {
                throw new AppException("邮箱已存在");
            }
        }

        // 3. 检查手机号是否存在
        if (registerDTO.getPhone() != null) {
            Long phoneCount = this.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, registerDTO.getPhone()));
            if (phoneCount > 0) {
                throw new AppException("手机号已存在");
            }
        }

        // 4. 保存用户
        SysUser user = new SysUser();
        BeanUtil.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1); // 默认启用
        user.setGender(0); // 默认未知
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        this.save(user);
    }
}
