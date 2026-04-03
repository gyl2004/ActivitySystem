package com.charity.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.sys.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    String getValueByKey(String key);
    void updateValueByKey(String key, String value);
}
