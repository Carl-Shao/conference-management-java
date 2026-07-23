package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;

import java.io.File;
import java.util.Date;

public interface IMeetingFinalizeService {

    void finalizeMeeting(Long meetingId, File audioFile);
}
