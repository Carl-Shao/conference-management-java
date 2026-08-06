package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.MeetingRecord;
import com.ruoyi.huiyi.domain.dto.MeetingMergeDTO;
import com.ruoyi.huiyi.domain.dto.MeetingMoveFolderDTO;
import com.ruoyi.huiyi.domain.vo.MeetingDetailVO;

import java.util.List;

public interface IMeetingRecordService {

    MeetingRecord selectMeetingRecordById(Long meetingId);

    /** 会议详情：转写 + 生成纪要 + 笔记 一次性返回，供前端点击跳转后使用 */
    MeetingDetailVO selectMeetingDetail(Long meetingId);

    /** 统一列表查询，全部/收藏/文件夹三个页面复用 */
    List<MeetingRecord> selectMeetingRecordList(MeetingRecord meetingRecord);

    int insertMeetingRecord(MeetingRecord meetingRecord);

    int updateMeetingRecord(MeetingRecord meetingRecord);

    int deleteMeetingRecordByIds(Long[] meetingIds);

    /** 重命名 */
    int renameMeeting(Long meetingId, String title);

    /** 收藏/取消收藏 */
    int toggleFavorite(Long meetingId, boolean favorite);

    /** 批量移动到文件夹 */
    int moveToFolder(MeetingMoveFolderDTO dto);

    /**
     * 合并多条会议记录：
     * 1. 按 meetingIds 顺序读取各自转写内容并拼接
     * 2. 生成一条新的 huiyi_meeting 记录，时长/文件路径信息汇总
     * 3. 原记录标记 is_merged=1（默认从列表隐藏，不物理删除）
     * 4. 如 regenerateMinutes=true，则丢入现有 LLM 生成队列重新生成纪要
     * 返回新生成的会议ID
     */
    Long mergeMeetings(MeetingMergeDTO dto);

    /** 保存/更新用户笔记 */
    int saveNote(Long meetingId, String content);

    void saveMinutesEdit(Long meetingId, String content);

    void saveTranscriptResult(Long meetingId, String content);

    void saveMinutesResult(Long meetingId, String content);

    void markProcessFailed(Long meetingId);
}
