package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.MeetingFolder;

import java.util.List;

public interface MeetingFolderMapper {

    MeetingFolder selectMeetingFolderById(Long folderId);

    List<MeetingFolder> selectMeetingFolderList(MeetingFolder meetingFolder);

    int insertMeetingFolder(MeetingFolder meetingFolder);

    int updateMeetingFolder(MeetingFolder meetingFolder);

    int deleteMeetingFolderById(Long folderId);

    int deleteMeetingFolderByIds(Long[] folderIds);
}
