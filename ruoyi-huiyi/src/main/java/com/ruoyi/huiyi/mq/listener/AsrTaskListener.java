package com.ruoyi.huiyi.mq.listener;

import com.rabbitmq.client.Channel;
import com.ruoyi.huiyi.config.RabbitMqConfig;
import com.ruoyi.huiyi.mq.message.AsrTaskMessage;
import com.ruoyi.huiyi.mq.message.MinutesTaskMessage;
import com.ruoyi.huiyi.service.IMeetingAsrService;
import org.springframework.amqp.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AsrTaskListener {

    private static final Logger log = LoggerFactory.getLogger(AsrTaskListener.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IMeetingAsrService meetingAsrService;

    // concurrency = "2-4" 表示最少2个、最多4个并发消费者线程
    @RabbitListener(queues = RabbitMqConfig.ASR_QUEUE, concurrency = "2-4", ackMode = "MANUAL")
    public void handle(AsrTaskMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try{
            log.info("开始处理ASR任务, taskId={}, filePath={}", message.getTaskId(), message.getAudioPath());

            String text = meetingAsrService.asrTranslateService(message.getAudioPath());

            MinutesTaskMessage minutesTaskMessage = new MinutesTaskMessage();
            minutesTaskMessage.setTaskId(message.getTaskId());
            minutesTaskMessage.setRecognizedText(text);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.MINUTES_EXCHANGE,
                    RabbitMqConfig.MINUTES_ROUTING_KEY,
                    minutesTaskMessage
            );

            channel.basicAck(deliveryTag, false);
            log.info("ASR任务处理完成, taskId={}", message.getTaskId());
        }catch (Exception e) {
            log.error("ASR任务处理失败, taskId={}", message.getTaskId(), e);
            // false, false = 不重新入队，避免死循环；建议后续接死信队列做重试/告警
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
