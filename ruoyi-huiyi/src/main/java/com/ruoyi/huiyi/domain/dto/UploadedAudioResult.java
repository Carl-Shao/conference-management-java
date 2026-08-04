package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;

/**
 * 上传音频落盘后的结果：相对路径 + 本地解析出来的时长。
 * 时长在保存文件这一步就地计算好，避免调用方（Controller）再拼一次绝对路径重复计算，
 * 减少路径拼接出错的风险。
 */
public class UploadedAudioResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 相对路径，比如 uploads/20260804_153000_xxx.wav */
    private String relativePath;

    /** 时长（秒），解析失败时为 0 */
    private Long durationSeconds;

    public UploadedAudioResult() {}

    public UploadedAudioResult(String relativePath, Long durationSeconds)
    {
        this.relativePath = relativePath;
        this.durationSeconds = durationSeconds;
    }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
}