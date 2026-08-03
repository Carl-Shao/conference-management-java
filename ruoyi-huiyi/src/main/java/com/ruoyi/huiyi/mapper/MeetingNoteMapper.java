package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.MeetingNote;

public interface MeetingNoteMapper {

    MeetingNote selectByMeetingId(Long meetingId);

    int insertOrUpdate(MeetingNote note);

    int deleteByMeetingId(Long meetingId);
}
