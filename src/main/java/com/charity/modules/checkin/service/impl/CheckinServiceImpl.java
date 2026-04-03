package com.charity.modules.checkin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.checkin.dto.CheckinDTO;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.mapper.ActivityCheckinMapper;
import com.charity.modules.checkin.service.CheckinService;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.service.RegistrationService;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 签到服务实现类
 */
@Service
public class CheckinServiceImpl extends ServiceImpl<ActivityCheckinMapper, ActivityCheckin> implements CheckinService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkin(CheckinDTO checkinDTO, Long userId, String ip) {
        Activity activity = activityService.getById(checkinDTO.getActivityId());
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        
        // 1. 检查是否报名且审核通过
        ActivityRegistration registration = registrationService.getOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activity.getId())
                .eq(ActivityRegistration::getUserId, userId));
        if (registration == null || registration.getStatus() != 1) {
            throw new AppException("未报名或报名未通过，无法签到");
        }

        // 2. 检查活动是否正在进行中或已发布但未开始 (签到通常在开始前后)
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getEndTime())) {
            throw new AppException("活动已结束，无法签到");
        }
        // 这里可以根据业务需要增加更精细的时间限制，比如开始前1小时内允许签到

        // 3. 检查是否已经签到过
        Long count = this.count(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activity.getId())
                .eq(ActivityCheckin::getUserId, userId));
        if (count > 0) {
            throw new AppException("请勿重复签到");
        }

        // 4. 保存签到记录
        ActivityCheckin checkin = new ActivityCheckin();
        BeanUtil.copyProperties(checkinDTO, checkin);
        checkin.setUserId(userId);
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setIpAddress(ip);
        this.save(checkin);

        // 5. 计算并增加积分与志愿时长
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            double durationHours = activity.getVolunteerDuration() != null ? activity.getVolunteerDuration() : 0.0;
            int pointsEarned = activity.getPoints() != null ? activity.getPoints() : 0;
            
            // 如果没有预设值，则根据时间自动计算
            if (durationHours <= 0 && activity.getStartTime() != null && activity.getEndTime() != null) {
                Duration duration = Duration.between(activity.getStartTime(), activity.getEndTime());
                durationHours = duration.toMinutes() / 60.0;
                // 保留一位小数
                durationHours = Math.round(durationHours * 10.0) / 10.0;
            }
            
            if (pointsEarned <= 0) {
                // 每小时志愿时长对应 10 积分
                pointsEarned = (int) (durationHours * 10);
                if (pointsEarned == 0) {
                    pointsEarned = 10; // 默认至少给10积分
                }
            }
            
            user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + pointsEarned);
            user.setVolunteerDuration((user.getVolunteerDuration() == null ? 0.0 : user.getVolunteerDuration()) + durationHours);
            sysUserMapper.updateById(user);

            // 6. 将获得的奖励记录到报名表中，便于个人中心展示
            registration.setEarnedPoints(pointsEarned);
            registration.setEarnedDuration(durationHours);
            registrationService.updateById(registration);
        }
    }
}
