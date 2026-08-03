package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.MeetingMinutes;

public interface MeetingMinutesMapper {

    MeetingMinutes selectByMeetingId(Long meetingId);

    int insertOrUpdate(MeetingMinutes minutes);

    int deleteByMeetingId(Long meetingId);
}
