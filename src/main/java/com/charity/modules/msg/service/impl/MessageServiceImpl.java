package com.charity.modules.msg.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.charity.modules.msg.entity.SysMessage;
import com.charity.modules.msg.mapper.SysMessageMapper;
import com.charity.modules.msg.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息通知服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements MessageService {

    @Override
    @Async
    public void sendSysMessage(Long userId, String title, String content, Integer type) {
        log.info("向用户 {} 发送站内信: {}", userId, title);
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType(type);
        msg.setStatus(0); // 未读
        msg.setCreateTime(LocalDateTime.now());
        this.save(msg);
    }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String content) {
        log.info("向邮箱 {} 发送邮件: {}", toEmail, subject);
        // 实际集成 Spring Mail
        try {
            // 模拟发送邮件耗时
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error("发送邮件中断: {}", e.getMessage());
        }
    }
}
