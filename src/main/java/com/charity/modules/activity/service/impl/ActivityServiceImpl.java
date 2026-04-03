package com.charity.modules.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.dto.ActivityDTO;
import com.charity.modules.activity.dto.ActivityQueryDTO;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.mapper.ActivityMapper;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.sys.service.SysConfigService;
import com.charity.websocket.NotificationServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 活动服务实现类
 */
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Autowired
    private SysConfigService configService;

    @Override
    public IPage<Activity> findPage(Page<Activity> page, ActivityQueryDTO queryDTO) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Activity::getTitle, queryDTO.getTitle())
                    .eq(queryDTO.getCategoryId() != null, Activity::getCategoryId, queryDTO.getCategoryId())
                    .eq(queryDTO.getStatus() != null, Activity::getStatus, queryDTO.getStatus())
                    .ge(queryDTO.getStartTimeBegin() != null, Activity::getStartTime, queryDTO.getStartTimeBegin())
                    .le(queryDTO.getStartTimeEnd() != null, Activity::getStartTime, queryDTO.getStartTimeEnd())
                    .eq(queryDTO.getCreateUserId() != null, Activity::getCreateUserId, queryDTO.getCreateUserId());
        }
        wrapper.orderByDesc(Activity::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Cacheable(value = "activity", key = "#id", unless = "#result == null")
    public Activity getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    @CacheEvict(value = "activity", key = "#entity.id")
    public boolean updateById(Activity entity) {
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createActivity(ActivityDTO activityDTO, Long userId) {
        Activity activity = BeanUtil.copyProperties(activityDTO, Activity.class);
        activity.setCreateUserId(userId);
        
        // 检查系统配置是否自动发布
        String autoPublish = configService.getValueByKey("activity.auto_publish");
        if ("true".equalsIgnoreCase(autoPublish)) {
            activity.setStatus(2); // 已发布
        } else {
            activity.setStatus(0); // 默认为草稿
        }
        
        activity.setRegisteredCount(0);
        activity.setViewCount(0);
        activity.setShareCount(0);
        this.save(activity);

        if (activity.getStatus() == 2) {
            NotificationServer.sendInfo(JSONUtil.createObj()
                    .set("type", "new_activity")
                    .set("activityId", activity.getId())
                    .set("title", activity.getTitle())
                    .toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void updateActivity(Long id, ActivityDTO activityDTO) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        if (activity.getStatus() > 1) {
            throw new AppException("已发布的活动不能直接编辑");
        }
        BeanUtil.copyProperties(activityDTO, activity);
        this.updateById(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void publishActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new AppException("当前状态不允许发布");
        }
        activity.setStatus(2); // 已发布
        this.updateById(activity);

        NotificationServer.sendInfo(JSONUtil.createObj()
                .set("type", "new_activity")
                .set("activityId", activity.getId())
                .set("title", activity.getTitle())
                .toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void cancelActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        activity.setStatus(5); // 已取消
        this.updateById(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "activity", key = "#id")
    public void revertToDraft(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        // 允许从“已发布(2)”或“已取消(5)”退回到“草稿(0)”
        activity.setStatus(0);
        this.updateById(activity);
    }
}
