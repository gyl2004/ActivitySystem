package com.charity.modules.sys.vo;

import com.charity.modules.sys.entity.SysUser;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {
    private SysUser user;
    private List<String> roles;
    private List<String> permissions;
}
