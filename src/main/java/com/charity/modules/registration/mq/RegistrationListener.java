package com.charity.modules.registration.mq;

import cn.hutool.json.JSONUtil;
import com.charity.config.RabbitMQConfig;
import com.charity.modules.registration.dto.RegistrationDTO;
import com.charity.modules.registration.service.impl.RegistrationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RegistrationListener {

    @Autowired
    private com.charity.modules.registration.service.RegistrationService registrationService;

    @RabbitListener(queues = RabbitMQConfig.REGISTRATION_QUEUE)
    public void handleRegistrationRequest(Map<String, Object> message) {
        log.info("Processing async registration request: {}", message);
        
        try {
            Long userId = Long.valueOf(message.get("userId").toString());
            // 处理嵌套的 DTO
            RegistrationDTO dto = JSONUtil.toBean(JSONUtil.toJsonStr(message.get("registrationDTO")), RegistrationDTO.class);
            
            // 执行实际报名逻辑
            registrationService.doRegisterAsync(dto, userId);
            
            log.info("Successfully processed async registration for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to process async registration: {}", e.getMessage(), e);
        }
    }
}
