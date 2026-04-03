package com.charity.modules.msg.mq;

import cn.hutool.json.JSONUtil;
import com.charity.config.RabbitMQConfig;
import com.charity.modules.msg.entity.SysMessage;
import com.charity.modules.msg.service.MessageService;
import com.charity.websocket.NotificationServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MessageListener {

    @Autowired
    private MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFY_QUEUE)
    public void handleNotifyMessage(Map<String, Object> message) {
        log.info("Received MQ message: {}", message);
        
        Long userId = (Long) message.get("userId");
        String title = (String) message.get("title");
        String content = (String) message.get("content");
        Integer type = (Integer) message.get("type"); // 1-系统, 2-报名, 3-签到, 4-评价

        // 1. 保存到数据库
        SysMessage sysMessage = new SysMessage();
        sysMessage.setUserId(userId);
        sysMessage.setTitle(title);
        sysMessage.setContent(content);
        sysMessage.setType(type);
        sysMessage.setReadStatus(0);
        messageService.save(sysMessage);

        // 2. 实时推送 (WebSocket)
        NotificationServer.sendToUser(userId.toString(), JSONUtil.toJsonStr(sysMessage));
    }
}
