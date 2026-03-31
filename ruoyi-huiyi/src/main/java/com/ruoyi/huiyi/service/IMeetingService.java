package com.ruoyi.huiyi.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.huiyi.domain.Meeting;

/**
 * 会议信息Service接口
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
public interface IMeetingService extends IService<Meeting>
{
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
     * 批量删除会议信息
     * 
     * @param ids 需要删除的会议信息主键集合
     * @return 结果
     */
    public int deleteMeetingByIds(Long[] ids);

    /**
     * 删除会议信息信息
     * 
     * @param id 会议信息主键
     * @return 结果
     */
    public int deleteMeetingById(Long id);
}
