package com.charity.modules.checkin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.checkin.dto.CheckinDTO;
import com.charity.modules.checkin.entity.ActivityCheckin;
import com.charity.modules.checkin.mapper.ActivityCheckinMapper;
import com.charity.modules.checkin.service.CheckinService;
import com.charity.modules.checkin.vo.CheckinVO;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.service.RegistrationService;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.mapper.SysUserMapper;
import com.charity.websocket.NotificationServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * 签到服务实现类
 */
@Service
public class CheckinServiceImpl extends ServiceImpl<ActivityCheckinMapper, ActivityCheckin> implements CheckinService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private com.charity.modules.registration.mapper.ActivityRegistrationMapper registrationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CHECKIN_QR_PREFIX = "checkin:qr:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkin(CheckinDTO checkinDTO, Long userId, String ip) {
        Activity activity = activityService.getById(checkinDTO.getActivityId());
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        
        // 1. 验证签到码 (如果是签到码签到)
        if (checkinDTO.getCheckinType() != null && checkinDTO.getCheckinType() == 1) {
            if (!StringUtils.hasText(checkinDTO.getToken())) {
                throw new AppException("签到码不能为空");
            }
            String key = CHECKIN_QR_PREFIX + activity.getId();
            String validToken = redisTemplate.opsForValue().get(key);
            if (!checkinDTO.getToken().equals(validToken)) {
                throw new AppException("签到码不正确或已过期，请向管理员确认");
            }
        }
        
        // 1. 检查是否报名且审核通过
        ActivityRegistration registration = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
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
            registrationMapper.updateById(registration);

            // 7. 实时推送签到数据 (WebSocket 广播给管理端或大屏)
            NotificationServer.sendInfo(JSONUtil.createObj()
                    .set("type", "checkin")
                    .set("activityId", activity.getId())
                    .set("activityTitle", activity.getTitle())
                    .set("userId", userId)
                    .set("nickname", user.getNickname())
                    .set("checkinTime", LocalDateTime.now().toString())
                    .toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualCheckin(Long activityId, Long userId, String ip) {
        CheckinDTO dto = new CheckinDTO();
        dto.setActivityId(activityId);
        dto.setCheckinType(2);
        this.checkin(dto, userId, ip);
    }

    @Override
    public IPage<CheckinVO> findPage(Page<ActivityCheckin> page, Long activityId, Long userId) {
        LambdaQueryWrapper<ActivityCheckin> wrapper = new LambdaQueryWrapper<ActivityCheckin>()
                .orderByDesc(ActivityCheckin::getCheckinTime);
        if (activityId != null) {
            wrapper.eq(ActivityCheckin::getActivityId, activityId);
        }
        if (userId != null) {
            wrapper.eq(ActivityCheckin::getUserId, userId);
        }
        IPage<ActivityCheckin> checkinPage = this.page(page, wrapper);
        Page<CheckinVO> voPage = new Page<>(checkinPage.getCurrent(), checkinPage.getSize(), checkinPage.getTotal());
        List<ActivityCheckin> records = checkinPage.getRecords();
        if (records.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }

        List<Long> activityIds = records.stream().map(ActivityCheckin::getActivityId).distinct().toList();
        List<Long> userIds = records.stream().map(ActivityCheckin::getUserId).distinct().toList();

        Map<Long, Activity> activityMap = activityService.listByIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));

        List<CheckinVO> voList = records.stream().map(c -> {
            CheckinVO vo = BeanUtil.copyProperties(c, CheckinVO.class);
            Activity a = activityMap.get(c.getActivityId());
            if (a != null) {
                vo.setActivityTitle(a.getTitle());
            }
            SysUser u = userMap.get(c.getUserId());
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
            }
            return vo;
        }).toList();

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public String generateCheckinCode(Long activityId, Integer expireMinutes) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new AppException("活动不存在");
        }
        
        if (expireMinutes == null || expireMinutes <= 0) {
            expireMinutes = 5; // 默认 5 分钟
        }
        
        // 生成 6 位随机数字签到码并存入 Redis
        String code = cn.hutool.core.util.RandomUtil.randomNumbers(6);
        String key = CHECKIN_QR_PREFIX + activityId;
        redisTemplate.opsForValue().set(key, code, expireMinutes, TimeUnit.MINUTES);
        
        return code;
    }

    @Override
    public void exportCheckins(jakarta.servlet.http.HttpServletResponse response, Long activityId, Long userId) {
        // 1. 获取全量数据 (不分页)
        Page<ActivityCheckin> page = new Page<>(1, 10000);
        IPage<CheckinVO> voPage = this.findPage(page, activityId, userId);
        List<CheckinVO> records = voPage.getRecords();

        // 2. 转换数据为 Excel 导出格式
        List<Map<String, Object>> rows = records.stream().map(v -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("签到ID", v.getId());
            map.put("活动ID", v.getActivityId());
            map.put("活动标题", v.getActivityTitle());
            map.put("用户ID", v.getUserId());
            map.put("用户昵称", v.getNickname());
            map.put("签到时间", v.getCheckinTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            map.put("签到类型", v.getCheckinType() == 1 ? "签到码签到" : "管理员代签");
            map.put("签到IP", v.getIpAddress());
            return map;
        }).collect(Collectors.toList());

        // 3. 导出到 HTTP 响应流
        try {
            cn.hutool.poi.excel.ExcelWriter writer = cn.hutool.poi.excel.ExcelUtil.getWriter(true);
            writer.write(rows, true);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = "签到记录_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
            
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (Exception e) {
            throw new AppException("导出 Excel 失败: " + e.getMessage());
        }
    }

    @Override
    public List<com.charity.modules.registration.vo.RegistrationVO> findPendingCheckinUsers(Long activityId) {
        // 1. 获取该活动所有审核通过的报名记录
        com.charity.modules.registration.dto.RegistrationQueryDTO query = new com.charity.modules.registration.dto.RegistrationQueryDTO();
        query.setActivityId(activityId);
        query.setStatus(1); // 已通过
        List<com.charity.modules.registration.vo.RegistrationVO> allApproved = registrationMapper.selectVOList(query);

        if (allApproved.isEmpty()) {
            return List.of();
        }

        // 2. 获取该活动已经签到的用户ID
        List<Long> checkedInUserIds = this.list(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activityId))
                .stream().map(ActivityCheckin::getUserId).toList();

        // 3. 过滤掉已经签到的用户
        return allApproved.stream()
                .filter(r -> !checkedInUserIds.contains(r.getUserId()))
                .toList();
    }
}
