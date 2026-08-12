package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 批量给会议打标签/去标签，同一个结构给 /folder/add 和 /folder/remove 两个接口共用。
 * 文件夹现在是多对多的标签关系，不再是"移动"（移动意味着互斥、原来的会丢失），
 * 所以这里不叫 MoveFolderDTO 了，避免误导。
 */
public class MeetingFolderTagDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> meetingIds;
    private Long folderId;

    public List<Long> getMeetingIds() {
        return meetingIds;
    }
    public void setMeetingIds(List<Long> meetingIds) {
        this.meetingIds = meetingIds;
    }

    public Long getFolderId() {
        return folderId;
    }
    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }
}
