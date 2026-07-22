package com.ruoyi.huiyi.service;

public interface IMeetingRecordingService {

    /**
     * 开始录制
     */
    void startRecord(Long meetingId);

    /**
     * 暂停录制
     */
    void pauseRecord(Long meetingId);

    /**
     * 恢复录制
     */
    void resumeRecord(Long meetingId);

    /**
     * 结束录制
     */
    void stopRecord(Long meetingId);
}
