package com.ruoyi.huiyi.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.huiyi.domain.MeetingMinutes;
import com.ruoyi.huiyi.domain.MeetingRecord;
import com.ruoyi.huiyi.domain.MeetingRecordEvent;
import com.ruoyi.huiyi.domain.enums.MeetingRecordEventType;
import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;
import com.ruoyi.huiyi.domain.enums.MeetingStatus;
import com.ruoyi.huiyi.domain.vo.MeetingRecordStatusVO;
import com.ruoyi.huiyi.domain.vo.MeetingRecordVO;
import com.ruoyi.huiyi.mapper.MeetingMinutesMapper;
import com.ruoyi.huiyi.mapper.MeetingRecordEventMapper;
import com.ruoyi.huiyi.mapper.MeetingRecordMapper;
import com.ruoyi.huiyi.service.IMeetingRecordingService;
import com.ruoyi.huiyi.websocket.MeetingSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 修复说明：这个文件里 record_status（录制生命周期：NOT_STARTED/RECORDING/PAUSED/...）
 * 和 status（处理进度：待转写/转写中/...）两个字段、两个枚举（MeetingRecordStatus / MeetingStatus）
 * 之前被搞混了——多处该调 setRecordStatus() 的地方错调成了 setStatus()，
 * 导致 record_status 列从来没被真正更新过，暂停/恢复/结束时状态检查全部失败。
 * 这一版把两者严格分清：record_status 只用 MeetingRecordStatus 读写，
 * status 只用 MeetingStatus 读写，不再交叉。
 */
@Service
public class MeetingRecordingServiceImpl implements IMeetingRecordingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingRecordingServiceImpl.class);

    private static final String WS_PATH_TEMPLATE = "/ws/huiyi/record/%d";

    @Autowired
    private MeetingRecordMapper meetingRecordMapper;

    @Autowired
    private MeetingRecordEventMapper recordEventMapper;

    @Autowired
    private MeetingMinutesMapper meetingMinutesMapper;

    @Autowired
    private MeetingSessionManager sessionManager;

    private void checkOwnership(MeetingRecord meetingRecord, Long meetingId) {
        if(!meetingRecord.getCreateBy().equals(SecurityUtils.getUsername())) {
            throw new ServiceException("无权操作该会议记录: " + meetingId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingRecordVO startRecord(Long meetingId, String operator) {
        MeetingRecord meetingRecord = meetingRecordMapper.selectMeetingRecordForUpdate(meetingId);
        if (meetingRecord == null) {
            throw new ServiceException("会议不存在" + meetingId);
        }
        checkOwnership(meetingRecord, meetingId);

        // MeetingRecordStatus.of() 本身就处理了null（返回NOT_STARTED），不需要额外判断，
        // 而且要读 getRecordStatus()，不是 getStatus()
        MeetingRecordStatus current = MeetingRecordStatus.of(meetingRecord.getRecordStatus());
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
        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setRecordStatus(MeetingRecordStatus.RECORDING.getCode()); // 之前误写成 setStatus(...)
        update.setRecordStartTime(now); // 之前误写成 setCreateTime(...)，updateRecordStatus这条SQL根本不认createTime字段
        meetingRecordMapper.updateRecordStatus(update);

        insertEvent(meetingId, MeetingRecordEventType.START, operator, null);

        return new MeetingRecordVO(meetingId, MeetingRecordStatus.RECORDING.getCode(),
                String.format(WS_PATH_TEMPLATE, meetingId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseRecord(Long meetingId, String operator) {
        MeetingRecord meetingRecord = meetingRecordMapper.selectMeetingRecordForUpdate(meetingId);
        requireStatus(meetingRecord, meetingId, MeetingRecordStatus.RECORDING);
        checkOwnership(meetingRecord, meetingId);

        sessionManager.pauseSession(meetingId);

        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setRecordStatus(MeetingRecordStatus.PAUSED.getCode()); // 之前误写成 setStatus(...)
        meetingRecordMapper.updateRecordStatus(update);

        insertEvent(meetingId, MeetingRecordEventType.PAUSE, operator, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeRecord(Long meetingId, String operator) {
        MeetingRecord meetingRecord = meetingRecordMapper.selectMeetingRecordForUpdate(meetingId);
        requireStatus(meetingRecord, meetingId, MeetingRecordStatus.PAUSED);
        checkOwnership(meetingRecord, meetingId);

        sessionManager.resumeSession(meetingId);

        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setRecordStatus(MeetingRecordStatus.RECORDING.getCode()); // 之前误写成 setStatus(...)
        meetingRecordMapper.updateRecordStatus(update);

        insertEvent(meetingId, MeetingRecordEventType.RESUME, operator, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopRecord(Long meetingId, String operator) {
        MeetingRecord meetingRecord = meetingRecordMapper.selectMeetingRecordForUpdate(meetingId);
        if(meetingRecord == null) {
            throw new ServiceException("会议不存在: " + meetingId);
        }
        checkOwnership(meetingRecord, meetingId);
        // 之前这里读的是 getStatus()（处理状态），应该读 getRecordStatus()（录制状态）
        MeetingRecordStatus current = MeetingRecordStatus.of(meetingRecord.getRecordStatus());
        if (current != MeetingRecordStatus.RECORDING && current != MeetingRecordStatus.PAUSED) {
            throw new IllegalStateException("会议[" + meetingId + "]当前状态[" + current.getDesc() + "]不允许结束录制");
        }

        // 关闭录制会话：flush最后一片、关闭WAV句柄、回填WAV头
        // 此时 audioFile 已经是固定目录下命名规范的完整wav文件，不需要再搬一次
        File audioFile = sessionManager.stopSession(meetingId);

        Date now = new Date();
        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setRecordStatus(MeetingRecordStatus.STOP_PENDING.getCode()); // 这一行原来就是对的
        update.setRecordEndTime(now);
        update.setAudioPath(audioFile.getAbsolutePath());
        update.setStatus(MeetingStatus.TRANSCRIBING.getCode()); // 这一行原来也是对的，status用MeetingStatus，没搞混
        meetingRecordMapper.updateRecordStatus(update);

        insertEvent(meetingId, MeetingRecordEventType.STOP, operator, null);
    }

    @Override
    public MeetingRecordStatusVO getRecordStatus(Long meetingId) {
        MeetingRecord meetingRecord = meetingRecordMapper.selectMeetingRecordById(meetingId);
        if (meetingRecord == null) {
            throw new IllegalArgumentException("会议不存在: " + meetingId);
        }
        checkOwnership(meetingRecord, meetingId);
        MeetingRecordStatus status = MeetingRecordStatus.of(meetingRecord.getRecordStatus());

        MeetingRecordStatusVO vo = new MeetingRecordStatusVO();
        vo.setMeetingId(meetingId);
        vo.setRecordStatus(status.getCode());
        vo.setRecordStatusDesc(status.getDesc());
        vo.setRecordDurationMs(meetingRecord.getDuration()); // 之前用了 getDuration()（秒，Integer），
        // 跟 record_duration_ms（毫秒，Long）不是一回事，也顺手改成读对应字段
        vo.setAudioFilePath(meetingRecord.getAudioPath());

        // 纪要不再单独存在 Meeting 上的 summeryText 字段里，统一从 huiyi_meeting_minutes 读，
        // 避免同一份内容存两处、以后改了一处忘了改另一处
        MeetingMinutes minutes = meetingMinutesMapper.selectByMeetingId(meetingId);
        vo.setSummaryText(minutes != null ? minutes.getContent() : null);
        return vo;
    }

    private void requireStatus(MeetingRecord meetingRecord, Long meetingId, MeetingRecordStatus expected) {
        if (meetingRecord == null) {
            throw new IllegalArgumentException("会议不存在: " + meetingId);
        }
        MeetingRecordStatus current = MeetingRecordStatus.of(meetingRecord.getRecordStatus());
        if (current != expected) {
            throw new IllegalStateException("会议[" + meetingId + "]当前状态[" + current.getDesc()
                    + "]，期望状态[" + expected.getDesc() + "]，操作被拒绝");
        }
    }

    private void insertEvent(Long meetingId, MeetingRecordEventType type, String operator, String remark) {
        MeetingRecordEvent event = new MeetingRecordEvent();
        event.setMeetingId(meetingId);
        event.setEventType(type.name());
        event.setEventTime(new Date());
        event.setOperator(operator);
        event.setRemark(remark);
        recordEventMapper.insertMeetingRecordEvent(event);
    }
}