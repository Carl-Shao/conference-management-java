package com.ruoyi.huiyi.service.impl;


import com.ruoyi.huiyi.domain.MeetingMinutes;
import com.ruoyi.huiyi.domain.MeetingNote;
import com.ruoyi.huiyi.domain.MeetingRecord;
import com.ruoyi.huiyi.domain.MeetingTranscript;
import com.ruoyi.huiyi.domain.dto.MeetingMergeDTO;
import com.ruoyi.huiyi.domain.dto.MeetingMoveFolderDTO;
import com.ruoyi.huiyi.domain.enums.MeetingStatus;
import com.ruoyi.huiyi.domain.vo.MeetingDetailVO;
import com.ruoyi.huiyi.mapper.MeetingMinutesMapper;
import com.ruoyi.huiyi.mapper.MeetingNoteMapper;
import com.ruoyi.huiyi.mapper.MeetingRecordMapper;
import com.ruoyi.huiyi.mapper.MeetingTranscriptMapper;
import com.ruoyi.huiyi.service.IMeetingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.ruoyi.common.utils.SecurityUtils.*;

@Service
public class MeetingRecordServiceImpl implements IMeetingRecordService {

    @Autowired
    private MeetingRecordMapper meetingRecordMapper;

    @Autowired
    private MeetingTranscriptMapper meetingTranscriptMapper;

    @Autowired
    private MeetingMinutesMapper meetingMinutesMapper;

    @Autowired
    private MeetingNoteMapper meetingNoteMapper;

    @Override
    public MeetingRecord selectMeetingRecordById(Long meetingId) {
        return meetingRecordMapper.selectMeetingRecordById(meetingId);
    }

    @Override
    public MeetingDetailVO selectMeetingDetail(Long meetingId) {
        MeetingDetailVO vo = new MeetingDetailVO();
        vo.setMeeting(meetingRecordMapper.selectMeetingRecordById(meetingId));
        vo.setTranscript(meetingTranscriptMapper.selectByMeetingId(meetingId));
        vo.setMinutes(meetingMinutesMapper.selectByMeetingId(meetingId));
        vo.setNote(meetingNoteMapper.selectByMeetingId(meetingId));
        return vo;
    }

    @Override
    public List<MeetingRecord> selectMeetingRecordList(MeetingRecord meetingRecord)
    {
        return meetingRecordMapper.selectMeetingRecordList(meetingRecord);
    }

    @Override
    public int insertMeetingRecord(MeetingRecord meetingRecord)
    {
        meetingRecord.setCreateBy(getUsername());
        return meetingRecordMapper.insertMeetingRecord(meetingRecord);
    }

    @Override
    public int updateMeetingRecord(MeetingRecord meetingRecord)
    {
        meetingRecord.setUpdateBy(getUsername());
        return meetingRecordMapper.updateMeetingRecord(meetingRecord);
    }

    @Override
    @Transactional
    public int deleteMeetingRecordByIds(Long[] meetingIds)
    {
        return meetingRecordMapper.deleteMeetingRecordByIds(meetingIds);
    }

    @Override
    public int renameMeeting(Long meetingId, String title)
    {
        return meetingRecordMapper.updateTitle(meetingId, title);
    }

    @Override
    public int toggleFavorite(Long meetingId, boolean favorite)
    {
        return meetingRecordMapper.updateFavorite(meetingId, favorite ? "1" : "0");
    }

    @Override
    public int moveToFolder(MeetingMoveFolderDTO dto)
    {
        return meetingRecordMapper.batchMoveFolder(dto.getMeetingIds(), dto.getFolderId());
    }

    @Override
    @Transactional
    public Long mergeMeetings(MeetingMergeDTO dto)
    {
        List<Long> ids = dto.getMeetingIds();
        if (ids == null || ids.size() < 2)
        {
            throw new IllegalArgumentException("合并至少需要选择两条会议记录");
        }

        // 1. 按顺序取出各会议的转写内容，拼接（每段前加标题分隔，便于阅读来源）
        StringBuilder mergedTranscript = new StringBuilder();
        int totalDuration = 0;
        for (Long id : ids)
        {
            MeetingRecord record = meetingRecordMapper.selectMeetingRecordById(id);
            if (record == null) continue;
            if (record.getDuration() != null) totalDuration += record.getDuration();

            MeetingTranscript transcript = meetingTranscriptMapper.selectByMeetingId(id);
            mergedTranscript.append("\n\n===== ").append(record.getTitle()).append(" =====\n");
            if (transcript != null && transcript.getContent() != null)
            {
                mergedTranscript.append(transcript.getContent());
            }
        }

        // 2. 创建合并后的新会议记录
        MeetingRecord merged = new MeetingRecord();
        merged.setTitle(dto.getTitle());
        merged.setSourceType("1"); // 合并结果视为"派生"记录，来源标记按需调整
        merged.setDuration(totalDuration);
        merged.setStatus(MeetingStatus.TRANSCRIBED.getCode()); // 转写内容已就绪，等待/或已生成纪要
        merged.setIsFavorite("0");
        merged.setMergeFromIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        merged.setIsMerged("0");
        merged.setCreateBy(getUsername());
        meetingRecordMapper.insertMeetingRecord(merged);

        MeetingTranscript newTranscript = new MeetingTranscript();
        newTranscript.setMeetingId(merged.getMeetingId());
        newTranscript.setContent(mergedTranscript.toString());
        meetingTranscriptMapper.insertOrUpdate(newTranscript);

        // 3. 原记录标记为已合并，默认从列表隐藏（不物理删除，保留溯源）
        meetingRecordMapper.markAsMerged(ids);

        return merged.getMeetingId();
    }

    @Override
    public int saveNote(Long meetingId, String content)
    {
        MeetingNote note = new MeetingNote();
        note.setMeetingId(meetingId);
        note.setContent(content);
        note.setCreateBy(getUsername());
        return meetingNoteMapper.insertOrUpdate(note);
    }

    @Override
    @Transactional
    public void saveTranscriptResult(Long meetingId, String content)
    {
        MeetingTranscript transcript = new MeetingTranscript();
        transcript.setMeetingId(meetingId);
        transcript.setContent(content);
        meetingTranscriptMapper.insertOrUpdate(transcript);

        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setStatus(MeetingStatus.TRANSCRIBED.getCode()); // 转写完成
        meetingRecordMapper.updateMeetingRecord(update);
    }

    @Override
    @Transactional
    public void saveMinutesResult(Long meetingId, String content)
    {
        MeetingMinutes minutes = new MeetingMinutes();
        minutes.setMeetingId(meetingId);
        minutes.setContent(content);
        meetingMinutesMapper.insertOrUpdate(minutes);

        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setStatus(MeetingStatus.DONE.getCode()); // 已完成
        meetingRecordMapper.updateMeetingRecord(update);
    }

    @Override
    public void markProcessFailed(Long meetingId)
    {
        MeetingRecord update = new MeetingRecord();
        update.setMeetingId(meetingId);
        update.setStatus(MeetingStatus.FAILED.getCode()); // 失败
        meetingRecordMapper.updateMeetingRecord(update);
    }
}
