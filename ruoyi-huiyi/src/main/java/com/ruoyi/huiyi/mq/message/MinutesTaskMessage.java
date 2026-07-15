package com.ruoyi.huiyi.mq.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class MinutesTaskMessage implements Serializable {

    private String taskId;
    private String recognizedText;
}
