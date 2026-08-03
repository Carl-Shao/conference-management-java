package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.MeetingTranscript;

public interface MeetingTranscriptMapper {

    MeetingTranscript selectByMeetingId(Long meetingId);

    int insertOrUpdate(MeetingTranscript transcript);

    int deleteByMeetingId(Long meetingId);
}
