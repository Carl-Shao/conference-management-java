package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.vo.MeetingRecordStatusVO;
import com.ruoyi.huiyi.domain.vo.MeetingRecordVO;

public interface IMeetingRecordingService {

    /**
     * 开始录制
     */
    MeetingRecordVO startRecord(Long meetingId, String operator);

    /**
     * 暂停录制
     */
    void pauseRecord(Long meetingId, String operator);

    /**
     * 恢复录制
     */
    void resumeRecord(Long meetingId, String operator);

    /**
     * 结束录制
     */
    void stopRecord(Long meetingId, String operator);

    /**
     * 查询当前录制状态
     */
    MeetingRecordStatusVO getRecordStatus (Long meetingId);
}
