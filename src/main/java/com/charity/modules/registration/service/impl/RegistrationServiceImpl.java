package com.charity.modules.registration.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.registration.dto.AuditDTO;
import com.charity.modules.registration.dto.RegistrationDTO;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.mapper.ActivityRegistrationMapper;
import com.charity.modules.registration.service.RegistrationService;
import com.charity.modules.registration.vo.RegistrationVO;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报名服务实现类
 */
@Service
public class RegistrationServiceImpl extends ServiceImpl<ActivityRegistrationMapper, ActivityRegistration> implements RegistrationService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private SysUserService userService;

    @Override
    public Map<Integer, Long> getStatusStats() {
        // 分组统计各状态数量
        return this.list().stream()
                .collect(Collectors.groupingBy(ActivityRegistration::getStatus, Collectors.counting()));
    }

    @Override
    public IPage<RegistrationVO> findPageWithDetails(Page<ActivityRegistration> page, Integer status, Long activityId) {
        LambdaQueryWrapper<ActivityRegistration> queryWrapper = new LambdaQueryWrapper<ActivityRegistration>()
                .orderByDesc(ActivityRegistration::getCreateTime);
        if (status != null) {
            queryWrapper.eq(ActivityRegistration::getStatus, status);
        }
        if (activityId != null) {
            queryWrapper.eq(ActivityRegistration::getActivityId, activityId);
        }
        
        IPage<ActivityRegistration> regPage = this.page(page, queryWrapper);
        Page<RegistrationVO> voPage = new Page<>(regPage.getCurrent(), regPage.getSize(), regPage.getTotal());
        
        List<RegistrationVO> voList = regPage.getRecords().stream().map(reg -> {
            RegistrationVO vo = BeanUtil.copyProperties(reg, RegistrationVO.class);
            
            // 补充用户信息
            SysUser user = userService.getById(reg.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            
            // 补充活动信息
            Activity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
                vo.setActivityCover(activity.getCoverImage());
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegistrationDTO registrationDTO, Long userId) {
        Activity activity = activityService.getById(registrationDTO.getActivityId());
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        if (activity.getStatus() != 2) { // 必须是已发布状态
            throw new AppException("当前活动不可报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistrationStart()) || now.isAfter(activity.getRegistrationEnd())) {
            throw new AppException("不在报名时间内");
        }
        if (activity.getMaxParticipants() > 0 && activity.getRegisteredCount() >= activity.getMaxParticipants()) {
            throw new AppException("报名人数已满");
        }

        // 检查是否已经报名过 (非取消状态)
        Long count = this.count(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activity.getId())
                .eq(ActivityRegistration::getUserId, userId)
                .ne(ActivityRegistration::getStatus, 3)); // 排除已取消的
        if (count > 0) {
            throw new AppException("请勿重复报名");
        }

        ActivityRegistration registration = new ActivityRegistration();
        BeanUtil.copyProperties(registrationDTO, registration);
        registration.setUserId(userId);
        registration.setStatus(0); // 待审核
        this.save(registration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long activityId, Long userId) {
        ActivityRegistration registration = this.getOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (registration == null) {
            throw new AppException("报名信息不存在");
        }
        if (registration.getStatus() == 3) {
            throw new AppException("报名已取消");
        }
        
        Integer oldStatus = registration.getStatus();
        registration.setStatus(3); // 已取消
        this.updateById(registration);

        // 如果之前是通过状态，则需要减少活动报名数
        if (oldStatus == 1) {
            Activity activity = activityService.getById(activityId);
            activity.setRegisteredCount(activity.getRegisteredCount() - 1);
            activityService.updateById(activity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, AuditDTO auditDTO) {
        ActivityRegistration registration = this.getById(id);
        if (registration == null) {
            throw new AppException("报名信息不存在");
        }
        if (registration.getStatus() != 0) {
            throw new AppException("该报名已审核过");
        }

        registration.setStatus(auditDTO.getStatus());
        registration.setAuditRemark(auditDTO.getAuditRemark());
        this.updateById(registration);

        // 如果审核通过，增加活动报名数
        if (auditDTO.getStatus() == 1) {
            Activity activity = activityService.getById(registration.getActivityId());
            if (activity.getMaxParticipants() > 0 && activity.getRegisteredCount() >= activity.getMaxParticipants()) {
                throw new AppException("活动名额已满，无法审核通过");
            }
            activity.setRegisteredCount(activity.getRegisteredCount() + 1);
            activityService.updateById(activity);
        }
    }

    @Override
    public List<RegistrationVO> findMyRegistrations(Long userId) {
        List<ActivityRegistration> list = this.list(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getUserId, userId)
                .orderByDesc(ActivityRegistration::getCreateTime));
        
        return list.stream().map(reg -> {
            RegistrationVO vo = BeanUtil.copyProperties(reg, RegistrationVO.class);
            Activity activity = activityService.getById(reg.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
                vo.setActivityCover(activity.getCoverImage());
                vo.setActivityPoints(activity.getPoints());
                vo.setActivityDuration(activity.getVolunteerDuration());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
