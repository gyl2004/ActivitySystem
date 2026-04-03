package com.charity.modules.registration.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.common.AppException;
import com.charity.modules.activity.entity.Activity;
import com.charity.modules.activity.service.ActivityService;
import com.charity.modules.registration.dto.AuditDTO;
import com.charity.modules.registration.dto.RegistrationDTO;
import com.charity.modules.registration.dto.RegistrationQueryDTO;
import com.charity.modules.registration.entity.ActivityRegistration;
import com.charity.modules.registration.mapper.ActivityRegistrationMapper;
import com.charity.modules.registration.service.RegistrationService;
import com.charity.modules.registration.vo.RegistrationVO;
import com.charity.modules.sys.entity.SysUser;
import com.charity.modules.sys.service.SysUserService;
import com.charity.config.RabbitMQConfig;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报名服务实现类
 */
@Slf4j
@Service
public class RegistrationServiceImpl extends ServiceImpl<ActivityRegistrationMapper, ActivityRegistration> implements RegistrationService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private com.charity.modules.checkin.service.CheckinService checkinService;

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
        if (activity.getStatus() != 2) {
            throw new AppException("当前活动不可报名");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistrationStart()) || now.isAfter(activity.getRegistrationEnd())) {
            throw new AppException("不在报名时间内");
        }

        // 检查是否已经报名过 (同步预检，防止 MQ 堆积无效请求)
        Long count = this.count(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activity.getId())
                .eq(ActivityRegistration::getUserId, userId)
                .ne(ActivityRegistration::getStatus, 3));
        if (count > 0) {
            throw new AppException("请勿重复报名");
        }

        // 亮点：异步 MQ 报名请求削峰
        Map<String, Object> regRequest = new HashMap<>();
        regRequest.put("registrationDTO", registrationDTO);
        regRequest.put("userId", userId);
        
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.REGISTRATION_EXCHANGE, RabbitMQConfig.REGISTRATION_ROUTING_KEY, regRequest);
        } catch (Exception e) {
            log.error("Failed to push registration to MQ: {}", e.getMessage());
            throw new AppException("系统繁忙，请稍后再试");
        }
    }

    /**
     * 实际执行异步报名逻辑 (由 MQ 消费者调用)
     */
    @Transactional(rollbackFor = Exception.class)
    public void doRegisterAsync(RegistrationDTO registrationDTO, Long userId) {
        Activity activity = activityService.getById(registrationDTO.getActivityId());
        if (activity == null) return;

        // 再次检查重复 (异步环境下的双检)
        Long count = this.count(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activity.getId())
                .eq(ActivityRegistration::getUserId, userId)
                .ne(ActivityRegistration::getStatus, 3));
        if (count > 0) return;

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
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFY_EXCHANGE, RabbitMQConfig.NOTIFY_ROUTING_KEY, message);
        } catch (Exception e) {
            // 捕获 RabbitMQ 连接异常，避免影响主流程
            log.error("Failed to send MQ notification: {}", e.getMessage());
        }
    }

    @Override
    public void downloadCertificate(HttpServletResponse response, Long activityId, Long userId) {
        // 1. 校验活动与签到状态
        Activity activity = activityService.getById(activityId);
        if (activity == null) throw new AppException("活动不存在");
        
        SysUser user = userService.getById(userId);
        if (user == null) throw new AppException("用户不存在");

        com.charity.modules.checkin.entity.ActivityCheckin checkin = checkinService.getOne(
                new LambdaQueryWrapper<com.charity.modules.checkin.entity.ActivityCheckin>()
                .eq(com.charity.modules.checkin.entity.ActivityCheckin::getActivityId, activityId)
                .eq(com.charity.modules.checkin.entity.ActivityCheckin::getUserId, userId)
        );
        if (checkin == null) throw new AppException("您尚未完成该活动的签到，无法下载证书");

        // 2. 生成 PDF
        try {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4.rotate());
            response.setContentType("application/pdf");
            String fileName = URLEncoder.encode("志愿服务证书_" + activity.getTitle(), StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".pdf");
            
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // 设置中文字体
            com.itextpdf.text.pdf.BaseFont bf = com.itextpdf.text.pdf.BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED);
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(bf, 36, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.DARK_GRAY);
            com.itextpdf.text.Font subTitleFont = new com.itextpdf.text.Font(bf, 24, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.GRAY);
            com.itextpdf.text.Font contentFont = new com.itextpdf.text.Font(bf, 18, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.BLACK);
            com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(bf, 14, com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.LIGHT_GRAY);

            // 标题
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("志愿服务证书", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingBefore(50);
            document.add(title);

            com.itextpdf.text.Paragraph subTitle = new com.itextpdf.text.Paragraph("CERTIFICATE OF VOLUNTEER SERVICE", subTitleFont);
            subTitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(60);
            document.add(subTitle);

            // 正文
            String contentStr = String.format("亲爱的 %s 同志：\n\n    感谢您参加“%s”志愿服务活动。在该活动中，您表现优异，累计贡献志愿时长 %s 小时。特发此证，以资鼓励！", 
                user.getNickname(), activity.getTitle(), activity.getVolunteerDuration());
            com.itextpdf.text.Paragraph content = new com.itextpdf.text.Paragraph(contentStr, contentFont);
            content.setLeading(30);
            content.setIndentationLeft(50);
            content.setIndentationRight(50);
            document.add(content);

            // 落款
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日");
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph("\n\n\n阳光公益管理平台\n" + LocalDateTime.now().format(dtf), contentFont);
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            footer.setIndentationRight(80);
            document.add(footer);

            // 装饰线
            com.itextpdf.text.Paragraph line = new com.itextpdf.text.Paragraph("\n\n\n————————————————————————————————", footerFont);
            line.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(line);

            document.close();
        } catch (Exception e) {
            log.error("PDF 生成失败", e);
            throw new AppException("证书生成失败: " + e.getMessage());
        }
    }
@Override
    public List<RegistrationVO> findMyRegistrations(Long userId) {
        RegistrationQueryDTO query = new RegistrationQueryDTO();
        query.setUserId(userId);
        return baseMapper.selectVOList(query);
    }

    @Override
    public void exportRegistrations(HttpServletResponse response, RegistrationQueryDTO queryDTO) {
        List<RegistrationVO> list = baseMapper.selectVOList(queryDTO);

        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("id", "报名ID");
        writer.addHeaderAlias("activityTitle", "活动名称");
        writer.addHeaderAlias("nickname", "用户昵称");
        writer.addHeaderAlias("realName", "真实姓名");
        writer.addHeaderAlias("phoneNumber", "手机号码");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("createTime", "报名时间");
        writer.addHeaderAlias("auditRemark", "审核备注");
        writer.setOnlyAlias(true);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<java.util.Map<String, Object>> exportList = list.stream().map(reg -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", reg.getId());
            map.put("activityTitle", reg.getActivityTitle());
            map.put("nickname", reg.getNickname());
            
            String realName = "";
            String phoneNumber = "";
            if (org.springframework.util.StringUtils.hasText(reg.getCustomFields())) {
                try {
                    cn.hutool.json.JSONObject customMap = cn.hutool.json.JSONUtil.parseObj(reg.getCustomFields());
                    realName = customMap.getStr("realName", "");
                    phoneNumber = customMap.getStr("phoneNumber", "");
                } catch (Exception e) {
                    // ignore parse error
                }
            }
            map.put("realName", realName);
            map.put("phoneNumber", phoneNumber);
            
            map.put("status", reg.getStatus() == null ? null : (
                reg.getStatus() == 0 ? "待审核" :
                reg.getStatus() == 1 ? "审核通过" :
                reg.getStatus() == 2 ? "审核驳回" :
                reg.getStatus() == 3 ? "已取消" : reg.getStatus().toString()
            ));
            map.put("createTime", reg.getCreateTime() != null ? reg.getCreateTime().format(formatter) : null);
            map.put("auditRemark", reg.getAuditRemark());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        writer.write(exportList, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("报名数据", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            writer.flush(out, true);
        } catch (IOException e) {
            throw new AppException("导出失败");
        } finally {
            writer.close();
        }
    }
}
