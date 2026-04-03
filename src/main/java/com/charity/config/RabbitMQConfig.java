package com.charity.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFY_EXCHANGE = "notify.exchange";
    public static final String NOTIFY_QUEUE = "notify.queue";
    public static final String NOTIFY_ROUTING_KEY = "notify.key";

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue notifyQueue() {
        return new Queue(NOTIFY_QUEUE);
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(notifyExchange()).with(NOTIFY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
