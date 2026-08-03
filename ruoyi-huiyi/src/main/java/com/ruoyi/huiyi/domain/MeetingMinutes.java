package com.ruoyi.huiyi.domain;

import java.io.Serializable;
import java.util.Date;

public class MeetingMinutes implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long meetingId;
    /** markdown 内容 */
    private String content;
    private Integer version;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
