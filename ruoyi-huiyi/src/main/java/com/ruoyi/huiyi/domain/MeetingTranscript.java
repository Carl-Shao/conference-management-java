package com.ruoyi.huiyi.domain;

import java.io.Serializable;
import java.util.Date;

/** 会议转写内容 huiyi_meeting_transcript */
public class MeetingTranscript implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long meetingId;
    /** 转写文本，建议存 JSON 数组：[{speaker, startTime, endTime, text}, ...] */
    private String content;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
