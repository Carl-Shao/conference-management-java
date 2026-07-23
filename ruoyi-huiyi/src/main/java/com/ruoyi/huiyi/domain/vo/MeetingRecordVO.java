package com.ruoyi.huiyi.domain.vo;

import java.io.Serializable;

public class MeetingRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long meetingId;

    private Integer recordStatus;

    private String wsPath;

    public MeetingRecordVO() {}

    public MeetingRecordVO(Long meetingId, Integer recordStatus, String wsPath) {
        this.meetingId = meetingId;
        this.recordStatus = recordStatus;
        this.wsPath = wsPath;
    }

    public Long getMeetingId() {return meetingId;}

    public void setMeetingId(Long meetingId) {this.meetingId = meetingId;}

    public Integer getRecordStatus() {return recordStatus;}

    public void setRecordStatus(Integer recordStatus) {this.recordStatus = recordStatus;}

    public String getWsPath() {return wsPath;}

    public void setWsPath(String wsPath) {this.wsPath = wsPath;}
}
