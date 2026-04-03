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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.charity.config.RabbitMQConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

        if (activity.getMaxParticipants() > 0 && activity.getRegisteredCount() >= activity.getMaxParticipants()) {
            registration.setStatus(4); // 候补中
            this.save(registration);
            sendNotify(userId, "进入候补队列", "您报名的活动【" + activity.getTitle() + "】名额已满，您已进入候补队列。", 2);
        } else {
            registration.setStatus(0); // 待审核
            this.save(registration);
            sendNotify(userId, "报名申请已提交", "您报名的活动【" + activity.getTitle() + "】已成功提交，请耐心等待管理员审核。", 2);
        }
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
        Activity activity = activityService.getById(activityId);
        if (oldStatus == 1) {
            activity.setRegisteredCount(activity.getRegisteredCount() - 1);
            activityService.updateById(activity);
            
            // 自动补位：找第一个候补的人，转为待审核
            ActivityRegistration firstWaiter = this.getOne(new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .eq(ActivityRegistration::getStatus, 4)
                    .orderByAsc(ActivityRegistration::getCreateTime)
                    .last("LIMIT 1"));
            if (firstWaiter != null) {
                firstWaiter.setStatus(0); // 待审核
                this.updateById(firstWaiter);
                sendNotify(firstWaiter.getUserId(), "获得候补名额", "您候补的活动【" + activity.getTitle() + "】已有名额空出，您已正式进入待审核列表。", 2);
            }
        }

        // 发送取消报名通知
        sendNotify(userId, "报名已取消", "您报名的活动【" + (activity != null ? activity.getTitle() : "未知活动") + "】已取消。", 2);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, AuditDTO auditDTO) {
        this.doAudit(id, auditDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(List<Long> ids, AuditDTO auditDTO) {
        for (Long id : ids) {
            this.doAudit(id, auditDTO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id) {
        ActivityRegistration registration = this.getById(id);
        if (registration == null) {
            throw new AppException("报名信息不存在");
        }
        // 只有已取消的报名才可能需要退款 (假设只有付费活动才需要退款)
        if (registration.getStatus() != 3) {
            throw new AppException("只有已取消的报名才能进行退款处理");
        }
        
        // 此处应集成支付系统的退款接口
        // Mock 退款成功
        sendNotify(registration.getUserId(), "退款成功通知", "您在活动中的报名退款已处理成功，资金将原路返回。", 2);
    }

    private void doAudit(Long id, AuditDTO auditDTO) {
        ActivityRegistration registration = this.getById(id);
        if (registration == null) {
            return; // 忽略不存在的
        }
        if (registration.getStatus() != 0) {
            return; // 忽略已审核的
        }

        registration.setStatus(auditDTO.getStatus());
        registration.setAuditRemark(auditDTO.getAuditRemark());
        this.updateById(registration);

        Activity activity = activityService.getById(registration.getActivityId());
        // 如果审核通过，增加活动报名数
        if (auditDTO.getStatus() == 1) {
            if (activity.getMaxParticipants() > 0 && activity.getRegisteredCount() >= activity.getMaxParticipants()) {
                throw new AppException("活动【" + activity.getTitle() + "】名额已满，无法继续审核通过");
            }
            activity.setRegisteredCount(activity.getRegisteredCount() + 1);
            activityService.updateById(activity);
        }

        // 发送审核结果通知
        String statusText = auditDTO.getStatus() == 1 ? "通过" : "不通过";
        String content = "您报名的活动【" + (activity != null ? activity.getTitle() : "未知活动") + "】审核" + statusText + "。";
        if (StringUtils.hasText(auditDTO.getAuditRemark())) {
            content += " 审核备注：" + auditDTO.getAuditRemark();
        }
        sendNotify(registration.getUserId(), "报名审核结果", content, 2);
    }

    private void sendNotify(Long userId, String title, String content, Integer type) {
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        message.put("title", title);
        message.put("content", content);
        message.put("type", type);
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFY_EXCHANGE, RabbitMQConfig.NOTIFY_ROUTING_KEY, message);
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
