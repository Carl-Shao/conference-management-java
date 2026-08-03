package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.MeetingRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MeetingRecordMapper {

    MeetingRecord selectMeetingRecordById(Long meetingId);

    /**
     * 会议记录统一查询接口
     * 通过 MeetingRecord 上的 query params 区分三个页面：
     *  - 全部会议纪要：folderId 不传，favoriteOnly 不传
     *  - 只看收藏：favoriteOnly = "1"
     *  - 文件夹详情：folderId = 具体文件夹ID
     * 排序、来源筛选统一通过 orderByColumn / sourceType 参数控制（RuoYi PageHelper + BaseEntity.params）
     */
    List<MeetingRecord> selectMeetingRecordList(MeetingRecord meetingRecord);

    int insertMeetingRecord(MeetingRecord meetingRecord);

    int updateMeetingRecord(MeetingRecord meetingRecord);

    int deleteMeetingRecordById(Long meetingId);

    int deleteMeetingRecordByIds(Long[] meetingIds);

    /** 重命名 */
    int updateTitle(@Param("meetingId") Long meetingId, @Param("title") String title);

    /** 收藏/取消收藏 */
    int updateFavorite(@Param("meetingId") Long meetingId, @Param("isFavorite") String isFavorite);

    /** 批量移动到文件夹（folderId 可为 null） */
    int batchMoveFolder(@Param("meetingIds") List<Long> meetingIds, @Param("folderId") Long folderId);

    /** 标记原记录为已合并 */
    int markAsMerged(@Param("meetingIds") List<Long> meetingIds);

    /** 统计某文件夹下的会议数量 */
    int countByFolderId(@Param("folderId") Long folderId);

    /** 删除文件夹时，把该文件夹下所有会议的 folderId 清空（移出文件夹，不删数据） */
    int clearFolderId(@Param("folderId") Long folderId);
}
