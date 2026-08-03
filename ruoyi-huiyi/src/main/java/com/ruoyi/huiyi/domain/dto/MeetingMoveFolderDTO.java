package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;
import java.util.List;

public class MeetingMoveFolderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> meetingIds;
    private Long folderId;

    public List<Long> getMeetingIds() { return meetingIds; }
    public void setMeetingIds(List<Long> meetingIds) { this.meetingIds = meetingIds; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
}
