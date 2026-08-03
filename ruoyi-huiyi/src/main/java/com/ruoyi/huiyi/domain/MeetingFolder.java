package com.ruoyi.huiyi.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会议文件夹 huiyi_folder
 */
public class MeetingFolder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 文件夹ID */
    private long folderId;

    /** 文件夹名称 */
    @Excel(name = "文件夹名称")
    private String folderName;

    /** 文件夹内会议数量，非数据库字段，查询列表时统计出来 */
    private Integer meetingCount;

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public Integer getMeetingCount() { return meetingCount; }
    public void setMeetingCount(Integer meetingCount) { this.meetingCount = meetingCount; }
}
