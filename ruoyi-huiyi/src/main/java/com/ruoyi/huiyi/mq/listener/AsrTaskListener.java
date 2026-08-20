package com.ruoyi.huiyi.mq.listener;

import com.rabbitmq.client.Channel;
import com.ruoyi.huiyi.config.RabbitMqConfig;
import com.ruoyi.huiyi.domain.vo.AsrResultVO;
import com.ruoyi.huiyi.mq.message.AsrTaskMessage;
import com.ruoyi.huiyi.mq.message.MinutesTaskMessage;
import com.ruoyi.huiyi.service.IMeetingAsrService;
import com.ruoyi.huiyi.service.IMeetingRecordService;
import org.springframework.amqp.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class AsrTaskListener {

    private static final Logger log = LoggerFactory.getLogger(AsrTaskListener.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IMeetingAsrService meetingAsrService;

    @Autowired
    private IMeetingRecordService meetingRecordService;

    // concurrency = "2-4" 表示最少2个、最多4个并发消费者线程
    @RabbitListener(queues = RabbitMqConfig.ASR_QUEUE, concurrency = "2-4", ackMode = "MANUAL", containerFactory = "rabbitListenerContainerFactory")
    public void handle(AsrTaskMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try{
            log.info("开始处理ASR任务, taskId={}, filePath={}", message.getTaskId(), message.getAudioPath());

            AsrResultVO asrResult = meetingAsrService.transcribeAudio(message.getAudioPath());
            String text = asrResult.getText();
            String transcript = meetingAsrService.buildSegmentedTranscript(asrResult);
            meetingRecordService.saveTranscriptResult(message.getMeetingId(), transcript);

            MinutesTaskMessage minutesTaskMessage = new MinutesTaskMessage();
            minutesTaskMessage.setTaskId(message.getTaskId());
            minutesTaskMessage.setMeetingId(message.getMeetingId());
            minutesTaskMessage.setRecognizedText(text);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.MINUTES_EXCHANGE,
                    RabbitMqConfig.MINUTES_ROUTING_KEY,
                    minutesTaskMessage
            );

            channel.basicAck(deliveryTag, false);
            log.info("ASR任务处理完成, taskId={}, meetingId={}", message.getTaskId(), message.getMeetingId());
        }catch (Exception e) {
            log.error("ASR任务处理失败, taskId={}, meetingId={}", message.getTaskId(), message.getMeetingId(), e);
            // 处理失败也要回写状态，不然前端会一直显示"转写中"，用户不知道出问题了
            try {
                meetingRecordService.markProcessFailed(message.getMeetingId());
            } catch (Exception inner) {
                log.error("回写失败状态时又出错, meetingId={}", message.getMeetingId(), inner);
            }
            // false, false = 不重新入队，避免死循环；建议后续接死信队列做重试/告警
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
