package com.ruoyi.huiyi.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MeetingFolderRelMapper
{
    /** 给多个会议批量打上同一个标签（已经打过的自动跳过，不会报唯一键冲突） */
    int batchAdd(@Param("meetingIds") List<Long> meetingIds, @Param("folderId") Long folderId);

    /** 给多个会议批量去掉同一个标签 */
    int batchRemove(@Param("meetingIds") List<Long> meetingIds, @Param("folderId") Long folderId);

    /** 查某个会议当前打了哪些标签 */
    List<Long> selectFolderIdsByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * 批量查多个会议各自打了哪些标签，避免列表页N+1查询。
     * 返回 meeting_id -> folder_id 的原始行，Service层自己按meetingId分组
     */
    List<MeetingFolderRelRow> selectByMeetingIds(@Param("meetingIds") List<Long> meetingIds);

    /** 单个会议：插入一批标签（不清空原有的），配合 deleteByMeetingId 在Service层组合成"全量替换" */
    int insertMeetingFolders(@Param("meetingId") Long meetingId, @Param("folderIds") List<Long> folderIds);

    /** 删除文件夹（标签）时，把关联表里指向这个标签的关系全部清掉 */
    int deleteByFolderId(@Param("folderId") Long folderId);

    /** 删除会议记录时，把关联表里这个会议的所有标签关系清掉 */
    int deleteByMeetingId(@Param("meetingId") Long meetingId);

    /** 统计某个标签下有多少个会议，文件夹列表页展示数量用 */
    int countByFolderId(@Param("folderId") Long folderId);

    /** 简单行结构，对应 meeting_id + folder_id 一行关联记录 */
    class MeetingFolderRelRow
    {
        private Long meetingId;
        private Long folderId;

        public Long getMeetingId() { return meetingId; }
        public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
    }
}
