package com.ruoyi.huiyi.service.impl;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.huiyi.domain.MeetingFolder;
import com.ruoyi.huiyi.mapper.MeetingFolderMapper;
import com.ruoyi.huiyi.mapper.MeetingRecordMapper;
import com.ruoyi.huiyi.service.IMeetingFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeetingFolderServiceImpl implements IMeetingFolderService {

    @Autowired
    private MeetingFolderMapper meetingFolderMapper;

    @Autowired
    private MeetingRecordMapper meetingRecordMapper;

    @Override
    public MeetingFolder selectMeetingFolderById(Long meetingId) {
        return meetingFolderMapper.selectMeetingFolderById(meetingId);
    }

    @Override
    public List<MeetingFolder> selectMeetingFolderList(MeetingFolder meetingFolder)
    {
        return meetingFolderMapper.selectMeetingFolderList(meetingFolder);
    }

    @Override
    public int insertMeetingFolder(MeetingFolder meetingFolder)
    {
        meetingFolder.setCreateBy(SecurityUtils.getUsername());
        return meetingFolderMapper.insertMeetingFolder(meetingFolder);
    }

    @Override
    public int updateMeetingFolder(MeetingFolder meetingFolder)
    {
        meetingFolder.setUpdateBy(SecurityUtils.getUsername());
        return meetingFolderMapper.updateMeetingFolder(meetingFolder);
    }

    @Override
    @Transactional
    public int deleteMeetingFolderByIds(Long[] folderIds)
    {
        for (Long folderId : folderIds)
        {
            meetingRecordMapper.clearFolderId(folderId);
        }
        return meetingFolderMapper.deleteMeetingFolderByIds(folderIds);
    }
}
