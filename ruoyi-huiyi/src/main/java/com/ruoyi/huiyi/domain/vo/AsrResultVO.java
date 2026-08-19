package com.ruoyi.huiyi.domain.vo;

import java.util.List;

public class AsrResultVO {

    /**
     * ASR最终转写文本
     */
    private String text;

    /**
     * 不带标点的原始文本
     */
    private String rawText;

    /**
     * 时间戳
     *
     * 每个元素：
     * [开始时间ms, 结束时间ms]
     */
    private List<List<Long>> timestamp;

    public AsrResultVO() {}

    public AsrResultVO(String text, String rawText, List<List<Long>> timestamp) {
        this.text = text;
        this.rawText = rawText;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public String getRawText() {
        return rawText;
    }
    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public List<List<Long>> getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(List<List<Long>> timestamp) {
        this.timestamp = timestamp;
    }
}
