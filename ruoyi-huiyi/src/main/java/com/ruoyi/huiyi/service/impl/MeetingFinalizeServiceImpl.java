package com.ruoyi.huiyi.service.impl;

import com.ruoyi.huiyi.config.RabbitMqConfig;
import com.ruoyi.huiyi.domain.MeetingTranscriptSegment;
import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;
import com.ruoyi.huiyi.mapper.MeetingMapper;
import com.ruoyi.huiyi.mq.message.AsrTaskMessage;
import com.ruoyi.huiyi.service.IMeetingFinalizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 录制结束后，把“完整会议录音”作为一个ASR任务丢进你现有的MQ流水线，
 * 复用 AsrTaskListener -> MinutesTaskListener 这条现成链路生成最终纪要。
 * 本类不再自己同步调用ASR/LLM，只负责“投递任务 + 把状态推进到处理中”。
 */
@Service
public class MeetingFinalizeServiceImpl implements IMeetingFinalizeService {

    private Logger log = LoggerFactory.getLogger(MeetingFinalizeServiceImpl.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MeetingMapper meetingMapper;

    @Override
    public void finalizeMeeting(Long meetingId, File audioFile) {
        AsrTaskMessage message = new AsrTaskMessage();
        message.setTaskId(String.valueOf(meetingId));
        message.setAudioPath(audioFile.getAbsolutePath());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ASR_EXCHANGE,
                RabbitMqConfig.MINUTES_ROUTING_KEY,
                message
        );

        log.info("会议[{}]完整录音已投递ASR任务队列, path={}", meetingId, audioFile.getAbsolutePath());
    }
}
