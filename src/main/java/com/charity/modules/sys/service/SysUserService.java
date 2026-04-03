package com.charity.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.sys.dto.RegisterDTO;
import com.charity.modules.sys.entity.SysUser;

/**
 * 系统用户服务接口
 */
public interface SysUserService extends IService<SysUser> {
    
    /**
     * 注册用户
     */
    void register(RegisterDTO registerDTO);
}
