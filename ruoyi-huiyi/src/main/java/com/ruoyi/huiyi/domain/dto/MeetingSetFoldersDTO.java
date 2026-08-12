package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 单个会议一次性设置完整的标签集合（多选框提交），
 * 跟批量打/去标签是两种不同的交互场景，所以分开一个DTO：
 * 这个是"编辑某一条会议的标签"，那个是"给一批会议加/减某一个标签"。
 */

public class MeetingSetFoldersDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long meetingId;

    private List<Long> folderIds;

    public Long getMeetingId() {
        return meetingId;
    }
    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public List<Long> getFolderIds() {
        return folderIds;
    }
    public void setFolderIds(List<Long> folderIds) {
        this.folderIds = folderIds;
    }
}
