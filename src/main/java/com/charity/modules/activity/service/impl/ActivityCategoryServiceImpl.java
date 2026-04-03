package com.charity.modules.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.modules.activity.entity.ActivityCategory;
import com.charity.modules.activity.mapper.ActivityCategoryMapper;
import com.charity.modules.activity.service.ActivityCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活动分类服务实现类
 */
@Service
public class ActivityCategoryServiceImpl extends ServiceImpl<ActivityCategoryMapper, ActivityCategory> implements ActivityCategoryService {

    @Override
    public List<ActivityCategory> listEnabled() {
        return this.list(new LambdaQueryWrapper<ActivityCategory>()
                .eq(ActivityCategory::getStatus, 1) // 1 表示启用
                .orderByAsc(ActivityCategory::getSort));
    }
}
