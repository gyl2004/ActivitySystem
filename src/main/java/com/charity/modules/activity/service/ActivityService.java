package com.charity.modules.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.dto.ActivityDTO;
import com.charity.modules.activity.dto.ActivityQueryDTO;

import java.util.List;

/**
 * 活动服务接口
 */
public interface ActivityService extends IService<Activity> {
    
    /**
     * 分页查询活动
     */
    IPage<Activity> findPage(Page<Activity> page, ActivityQueryDTO queryDTO);
    
    /**
     * 创建活动
     */
    void createActivity(ActivityDTO activityDTO, Long userId);
    
    /**
     * 更新活动
     */
    void updateActivity(Long id, ActivityDTO activityDTO);
    
    /**
     * 发布活动
     */
    void publishActivity(Long id);
    
    /**
     * 取消活动
     */
    void cancelActivity(Long id);

    /**
     * 退回到草稿状态
     */
    void revertToDraft(Long id);

    /**
     * 全文搜索活动
     */
    IPage<Activity> search(Page<Activity> page, String keyword);

    /**
     * 复制活动
     */
    void copyActivity(Long id, Long userId);

    /**
     * 导出活动数据
     */
    void exportActivities(jakarta.servlet.http.HttpServletResponse response, ActivityQueryDTO queryDTO);

    /**
     * 搜索附近活动
     */
    List<Activity> findNearby(Double longitude, Double latitude, Double distance);
}
