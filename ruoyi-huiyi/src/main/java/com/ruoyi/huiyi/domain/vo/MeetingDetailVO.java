package com.ruoyi.huiyi.domain.vo;

import com.ruoyi.huiyi.domain.MeetingMinutes;
import com.ruoyi.huiyi.domain.MeetingNote;
import com.ruoyi.huiyi.domain.MeetingRecord;
import com.ruoyi.huiyi.domain.MeetingTranscript;

import java.io.Serializable;

/**
 * 会议详情页返回对象：点击某条会议记录跳转后，前端一次性拿到
 * 会议基本信息 + 转写 + 生成纪要 + 用户笔记
 */
public class MeetingDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private MeetingRecord meeting;
    private MeetingTranscript transcript;
    private MeetingMinutes minutes;
    private MeetingNote note;

    public MeetingRecord getMeeting() { return meeting; }
    public void setMeeting(MeetingRecord meeting) { this.meeting = meeting; }

    public MeetingTranscript getTranscript() { return transcript; }
    public void setTranscript(MeetingTranscript transcript) { this.transcript = transcript; }

    public MeetingMinutes getMinutes() { return minutes; }
    public void setMinutes(MeetingMinutes minutes) { this.minutes = minutes; }

    public MeetingNote getNote() { return note; }
    public void setNote(MeetingNote note) { this.note = note; }
}
