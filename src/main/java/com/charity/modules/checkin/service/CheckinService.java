package com.charity.modules.checkin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.dto.CheckinDTO;

/**
 * 签到服务接口
 */
public interface CheckinService extends IService<ActivityCheckin> {
    
    /**
     * 签到
     */
    void checkin(CheckinDTO checkinDTO, Long userId, String ip);
}
