package com.charity.modules.msg.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.charity.modules.msg.entity.SysMessage;

/**
 * 消息通知服务接口
 */
public interface MessageService extends IService<SysMessage> {
    
    /**
     * 发送系统通知 (站内信)
     */
    void sendSysMessage(Long userId, String title, String content, Integer type);
    
    /**
     * 发送邮件通知 (模拟)
     */
    void sendEmail(String toEmail, String subject, String content);
}
