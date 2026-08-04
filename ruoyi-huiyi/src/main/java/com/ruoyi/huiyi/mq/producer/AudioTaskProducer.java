package com.ruoyi.huiyi.mq.producer;

import com.ruoyi.huiyi.config.RabbitMqConfig;
import com.ruoyi.huiyi.mq.dto.AudioUploadTask;
import com.ruoyi.huiyi.mq.message.AsrTaskMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AudioTaskProducer {

    static {
        System.out.println("========== AudioTaskProducer class 被加载 ==========");
    }

    public AudioTaskProducer() {
        System.out.println("========== AudioTaskProducer 创建成功 ==========");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Logger log = LoggerFactory.getLogger(AudioTaskProducer.class);

    /**
     * 把上传后得到的文件路径列表，逐条发到ASR队列，交给消费者并发处理
     * @param tasks upload service 返回的本地文件路径列表
     * @return 每个文件对应生成的 taskId，用于后续追踪/查询结果
     */
    public List<String> sendBatch(List<AudioUploadTask> tasks) {
        System.out.println("准备发送ASR任务");
        return tasks.stream().map(task -> {
            String taskId = UUID.randomUUID().toString();
            AsrTaskMessage asrTaskMessage = new AsrTaskMessage();
            asrTaskMessage.setTaskId(taskId);
            asrTaskMessage.setMeetingId(task.getMeetingId());
            asrTaskMessage.setAudioPath(task.getFilePath());

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ASR_EXCHANGE,
                    RabbitMqConfig.ASR_ROUTING_KEY,
                    asrTaskMessage,
                    msg -> {

                        msg.getMessageProperties()
                                .setContentType("application/json");

                        return msg;
                    }
            );
            return taskId;
        }).toList();
    }
}
