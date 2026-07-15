package com.ruoyi.huiyi.mq.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class AsrTaskMessage implements Serializable {

    private String taskId;
    private String audioPath;
}
