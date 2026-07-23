package com.ruoyi.huiyi.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 会议录制事件流水（对应 meeting_record_event 表）
 */
public class MeetingRecordEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long meetingId;

    /** START/PAUSE/RESUME/STOP/ERROR */
    private String eventType;

    private Date eventTime;

    private String operator;

    private String remark;

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
