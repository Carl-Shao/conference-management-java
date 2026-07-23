package com.ruoyi.huiyi.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;
import com.ruoyi.huiyi.domain.vo.MeetingRecordStatusVO;
import com.ruoyi.huiyi.domain.vo.MeetingRecordVO;
import com.ruoyi.huiyi.mapper.MeetingMapper;
import com.ruoyi.huiyi.service.IMeetingRecordingService;
import com.ruoyi.huiyi.websocket.MeetingSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;

@Service
public class MeetingRecordingServiceImpl implements IMeetingRecordingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingRecordingServiceImpl.class);

    private static final String WS_PATH_TEMPLATE = "/ws/huiyi/record/%d";

    @Autowired
    private MeetingMapper meetingMapper;

    @Autowired
    private MeetingSessionManager sessionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingRecordVO startRecord(Long meetingId) {
        Meeting meeting = meetingMapper.selectMeetingById(meetingId);
        if (meeting == null) {
            throw new ServiceException("会议不存在" + meetingId);
        }
        MeetingRecordStatus current = MeetingRecordStatus.of(meeting.getRecordStatus());
        if (current != MeetingRecordStatus.NOT_STARTED && current != MeetingRecordStatus.FAILED) {
            throw new IllegalStateException("会议[" + meetingId + "]当前状态[" + current.getDesc() + "]不允许开始录制");
        }

        try {
            sessionManager.startSession(meetingId);
        } catch (IOException e) {
            log.error("会议[{}]创建录音文件失败", meetingId, e);
            throw new RuntimeException("创建录音文件失败: " + e.getMessage(), e);
        }

        Date now = new Date();
        Meeting update = new Meeting();
        update.setId(meetingId);
        update.setRecordStatus(MeetingRecordStatus.RECORDING.getCode());
        update.setRecordStartTime(now);
        meetingMapper.updateRecordStatus(update);

        insertEvent(meetingId, RecordEventType.START, operator, null);

        return new MeetingRecordVO(meetingId, MeetingRecordStatus.RECORDING.getCode(),
                String.format(WS_PATH_TEMPLATE, meetingId));
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

    @Override
    public MeetingRecordStatusVO getRecordStatus(Long meetingId) {}

    private void checkStatus(Meeting meeting, MeetingRecordStatus expected) {
        if (meeting == null) {
            throw new ServiceException("会议不存在");
        }
        if (meeting.getRecordStatus() != expected.getCode()) {
            throw new ServiceException("当前状态[" + meeting.getStatus() + "]不满足操作要求[" + expected.getCode() + "]");
        }
    }
}
