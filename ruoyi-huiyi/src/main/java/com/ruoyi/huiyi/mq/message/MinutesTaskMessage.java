package com.ruoyi.huiyi.mq.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class MinutesTaskMessage {

    private String taskId;
    private Long meetingId;
    private String recognizedText;
}
