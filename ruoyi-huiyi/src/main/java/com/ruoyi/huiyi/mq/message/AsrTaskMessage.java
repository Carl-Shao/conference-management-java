package com.ruoyi.huiyi.mq.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class AsrTaskMessage {

    private String taskId;
    private Long meetingId;
    private String audioPath;
}
