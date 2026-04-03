package com.charity.modules.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.activity.entity.ActivityCategory;

import java.util.List;

/**
 * 活动分类服务接口
 */
public interface ActivityCategoryService extends IService<ActivityCategory> {
    /**
     * 获取所有启用的分类
     */
    List<ActivityCategory> listEnabled();
}
