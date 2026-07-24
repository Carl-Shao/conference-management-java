package com.ruoyi.huiyi.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.huiyi.domain.Meeting;

/**
 * 会议信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
public interface MeetingMapper extends BaseMapper<Meeting> {
    /**
     * 查询会议信息
     * 
     * @param id 会议信息主键
     * @return 会议信息
     */
    public Meeting selectMeetingById(Long id);

    /**
     * 查询会议信息列表
     * 
     * @param meeting 会议信息
     * @return 会议信息集合
     */
    public List<Meeting> selectMeetingList(Meeting meeting);

    /**
     * 新增会议信息
     * 
     * @param meeting 会议信息
     * @return 结果
     */
    public int insertMeeting(Meeting meeting);

    /**
     * 修改会议信息
     * 
     * @param meeting 会议信息
     * @return 结果
     */
    public int updateMeeting(Meeting meeting);

    /**
     * 删除会议信息
     * 
     * @param id 会议信息主键
     * @return 结果
     */
    public int deleteMeetingById(Long id);

    /**
     * 批量删除会议信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMeetingByIds(Long[] ids);

    /**
     * 悲观锁查询：开始/暂停/恢复/结束录制前都要先拿到这把行锁，
     * 防止同一会议被并发重复触发状态迁移(例如前端手抖连点两次"开始")。
     * 必须在 @Transactional 方法内调用。
     */
    public Meeting selectMeetingForUpdate(Long meetingId);

    /** 更新录制状态及关联时间戳/时长字段 */
    int updateRecordStatus(Meeting meeting);

    /** 录制结束、全部转写完成后，回写全文转写与大模型生成的纪要 */
    int updateTranscriptAndSummary(Meeting meeting);
}
