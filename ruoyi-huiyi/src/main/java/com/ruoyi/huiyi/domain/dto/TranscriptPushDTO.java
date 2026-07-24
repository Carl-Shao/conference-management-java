package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;

/**
 * 通过 WebSocket text frame(JSON) 推送给前端的实时转写结果。
 */
public class TranscriptPushDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long meetingId;
    private Integer seqNo;
    private Long startOffsetMs;
    private Long endOffsetMs;
    private String text;

    public TranscriptPushDTO() {}

    public TranscriptPushDTO(Long meetingId, Integer seqNo, Long startOffsetMs, Long endOffsetMs, String text) {
        this.meetingId = meetingId;
        this.seqNo = seqNo;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
        this.text = text;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public Long getStartOffsetMs() {
        return startOffsetMs;
    }

    public void setStartOffsetMs(Long startOffsetMs) {
        this.startOffsetMs = startOffsetMs;
    }

    public Long getEndOffsetMs() {
        return endOffsetMs;
    }

    public void setEndOffsetMs(Long endOffsetMs) {
        this.endOffsetMs = endOffsetMs;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
