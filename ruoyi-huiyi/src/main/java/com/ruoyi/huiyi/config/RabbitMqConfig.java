package com.ruoyi.huiyi.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // ASR 队列
    public static final String ASR_EXCHANGE = "huiyi.asr.exchange";
    public static final String ASR_QUEUE = "huiyi.asr.queue";
    public static final String ASR_ROUTING_KEY = "huiyi.asr.routing.key";

    @Bean
    public DirectExchange asrExchange() {
        return new DirectExchange(ASR_EXCHANGE, true, false);
    }

    @Bean
    public Queue asrQueue() {
        return QueueBuilder.durable(ASR_QUEUE).build();
    }

    @Bean
    public Binding asrBinding() {
        return BindingBuilder.bind(asrQueue()).to(asrExchange()).with(ASR_ROUTING_KEY);
    }

    // LLM 队列
    public static final String MINUTES_EXCHANGE = "huiyi.minutes.exchange";
    public static final String MINUTES_QUEUE = "huiyi.minutes.queue";
    public static final String MINUTES_ROUTING_KEY = "huiyi.minutes.routing.key";

    @Bean
    public DirectExchange minutesExchange() {
        return new DirectExchange(MINUTES_EXCHANGE, true, false);
    }

    @Bean
    public Queue minutesQueue() {
        return QueueBuilder.durable(MINUTES_QUEUE).build();
    }

    @Bean
    public Binding minutesBinding() {
        return BindingBuilder.bind(minutesQueue()).to(minutesExchange()).with(MINUTES_ROUTING_KEY);
    }
}
