package com.ruoyi.huiyi.domain.vo;

import java.io.Serializable;

public class MeetingCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long meetingId;

    private String meetingTitle;

    public MeetingCreateVO() {}

    public MeetingCreateVO(Long meetingId, String meetingTitle) {
        this.meetingId = meetingId;
        this.meetingTitle = meetingTitle;
    }

    public Long getMeetingId() {
        return meetingId;
    }
    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }
    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }
}
