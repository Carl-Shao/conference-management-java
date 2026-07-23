package com.ruoyi.huiyi.domain;

import java.io.Serializable;
import java.util.Date;

public class MeetingTranscriptSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long meetingId;

    private Integer seqNo;

    private Long startOffsetMs;

    private Long endOffsetMs;

    private String text;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
