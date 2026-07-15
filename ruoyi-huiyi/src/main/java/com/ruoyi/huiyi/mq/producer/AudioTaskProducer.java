package com.ruoyi.huiyi.mq.producer;

import com.ruoyi.huiyi.config.RabbitMqConfig;
import com.ruoyi.huiyi.mq.message.AsrTaskMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AudioTaskProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 把上传后得到的文件路径列表，逐条发到ASR队列，交给消费者并发处理
     * @param audioPaths upload service 返回的本地文件路径列表
     * @return 每个文件对应生成的 taskId，用于后续追踪/查询结果
     */
    public List<String> sendBatch(List<String> audioPaths) {
        return audioPaths.stream().map(path -> {
            String taskId = UUID.randomUUID().toString();
            AsrTaskMessage asrTaskMessage = new AsrTaskMessage();
            asrTaskMessage.setTaskId(taskId);
            asrTaskMessage.setAudioPath(path);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ASR_EXCHANGE,
                    RabbitMqConfig.ASR_ROUTING_KEY,
                    asrTaskMessage
            );
            return taskId;
        }).toList();
    }
}
