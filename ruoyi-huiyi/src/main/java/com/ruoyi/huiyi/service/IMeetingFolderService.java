package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.MeetingFolder;

import java.util.List;

public interface IMeetingFolderService {

    MeetingFolder selectMeetingFolderById(Long folderId);

    List<MeetingFolder> selectMeetingFolderList(MeetingFolder meetingFolder);

    int insertMeetingFolder(MeetingFolder meetingFolder);

    int updateMeetingFolder(MeetingFolder meetingFolder);

    /**
     * 删除文件夹。folderId 下的会议记录不会被删除，而是自动移出（folder_id 置为 null），
     * 回到"全部会议纪要"未归档状态，避免误删数据。
     */
    int deleteMeetingFolderByIds(Long[] folderIds);
}
