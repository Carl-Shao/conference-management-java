package com.ruoyi.huiyi.domain.vo;

import java.io.Serializable;

public class MeetingRecordStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long meetingId;

    private Integer recordStatus;

    private String recordStatusDesc;

    private Long recordDurationMs;

    private String audioFilePath;

    private String summaryText;

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Integer getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(Integer recordStatus) {
        this.recordStatus = recordStatus;
    }

    public String getRecordStatusDesc() {
        return recordStatusDesc;
    }

    public void setRecordStatusDesc(String recordStatusDesc) {
        this.recordStatusDesc = recordStatusDesc;
    }

    public Long getRecordDurationMs() {
        return recordDurationMs;
    }

    public void setRecordDurationMs(Long recordDurationMs) {
        this.recordDurationMs = recordDurationMs;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
}
