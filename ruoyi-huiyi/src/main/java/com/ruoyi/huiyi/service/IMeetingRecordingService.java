package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.vo.MeetingRecordStatusVO;
import com.ruoyi.huiyi.domain.vo.MeetingRecordVO;

public interface IMeetingRecordingService {

    /**
     * 开始录制
     */
    MeetingRecordVO startRecord(Long meetingId);

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

    /**
     * 查询当前录制状态
     */
    MeetingRecordStatusVO getRecordStatus (Long meetingId);
}
