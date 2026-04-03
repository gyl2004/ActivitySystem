package com.charity.modules.checkin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.dto.CheckinDTO;
import com.charity.modules.checkin.vo.CheckinVO;
import com.charity.modules.registration.vo.RegistrationVO;

import java.util.List;

/**
 * 签到服务接口
 */
public interface CheckinService extends IService<ActivityCheckin> {
    
    /**
     * 签到
     */
    void checkin(CheckinDTO checkinDTO, Long userId, String ip);

    void manualCheckin(Long activityId, Long userId, String ip);

    IPage<CheckinVO> findPage(Page<ActivityCheckin> page, Long activityId, Long userId);

    /**
     * 生成活动签到码（6位数字）
     * @param activityId 活动ID
     * @param expireMinutes 有效期（分钟）
     * @return 6位签到码
     */
    String generateCheckinCode(Long activityId, Integer expireMinutes);

    /**
     * 导出签到数据
     */
    void exportCheckins(jakarta.servlet.http.HttpServletResponse response, Long activityId, Long userId);

    /**
     * 获取待签到用户列表
     */
    List<RegistrationVO> findPendingCheckinUsers(Long activityId);
}
