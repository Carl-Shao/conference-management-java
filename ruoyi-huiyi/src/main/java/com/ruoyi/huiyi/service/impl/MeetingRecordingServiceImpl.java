package com.ruoyi.huiyi.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;
import com.ruoyi.huiyi.mapper.MeetingMapper;
import com.ruoyi.huiyi.service.IMeetingRecordingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class MeetingRecordingServiceImpl implements IMeetingRecordingService {

    @Autowired
    private final MeetingMapper meetingMapper;

    public MeetingRecordingServiceImpl(MeetingMapper meetingMapper) {
        this.meetingMapper = meetingMapper;
    }

    @Override
    public void startRecord(Long meetingId) {
        Meeting meeting = meetingMapper.selectMeetingById(meetingId);
        if (meeting == null) {
            throw new ServiceException("会议不存在" + meetingId);
        }
        if (meeting.getRecordStatus() != MeetingRecordStatus.NOT_STARTED.getCode()) {
            throw new ServiceException("会议状态不允许开始录制，当前状态: " + meeting.getStatus());
        }


        meeting.setRecordStatus((int) MeetingRecordStatus.RECORDING.getCode());
        meeting.setRecordStartTime(new Date());
        meetingMapper.updateMeeting(meeting);
    }

    @Override
    public void pauseRecord(Long meetingId) {
        Meeting meeting = meetingMapper.selectMeetingById(meetingId);
        checkStatus(meeting, MeetingRecordStatus.PAUSED);


        meeting.setRecordStatus((int) MeetingRecordStatus.PAUSED.getCode());
        meetingMapper.updateMeeting(meeting);
    }

    @Override
    public void resumeRecord(Long meetingId) {
        Meeting meeting = meetingMapper.selectMeetingById(meetingId);
        checkStatus(meeting, MeetingRecordStatus.PAUSED);


        meeting.setRecordStatus((int) MeetingRecordStatus.RECORDING.getCode());
        meetingMapper.updateMeeting(meeting);
    }

    @Override
    public void stopRecord(Long meetingId) {
        Meeting meeting = meetingMapper.selectMeetingById(meetingId);
        if (meeting.getRecordStatus() != MeetingRecordStatus.RECORDING.getCode()
                && meeting.getRecordStatus() != MeetingRecordStatus.PAUSED.getCode()) {
            throw new ServiceException("当前状态不允许结束录制: " + meeting.getStatus());
        }


        meeting.setRecordStatus((int) MeetingRecordStatus.STOP_PENDING.getCode());
        meeting.setRecordEndTime(new Date());
        meetingMapper.updateMeeting(meeting);
    }

    private void checkStatus(Meeting meeting, MeetingRecordStatus expected) {
        if (meeting == null) {
            throw new ServiceException("会议不存在");
        }
        if (meeting.getRecordStatus() != expected.getCode()) {
            throw new ServiceException("当前状态[" + meeting.getStatus() + "]不满足操作要求[" + expected.getCode() + "]");
        }
    }
}
