package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.MeetingTranscriptSegment;

import java.util.List;

public interface MeetingTranscriptSegmentMapper {

    int insertSegment(MeetingTranscriptSegment segment);

    /** 按 seq_no 升序查询某场会议的全部转写片段，用于实时字幕补拉 / 结束后拼接全文 */
    List<MeetingTranscriptSegment> selectSegmentsByMeetingId(Long meetingId);
}
