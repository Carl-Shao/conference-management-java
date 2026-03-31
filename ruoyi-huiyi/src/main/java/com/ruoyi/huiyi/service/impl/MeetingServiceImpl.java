package com.ruoyi.huiyi.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.huiyi.mapper.MeetingMapper;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.service.IMeetingService;

/**
 * 会议信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@Service
public class MeetingServiceImpl extends ServiceImpl<MeetingMapper,Meeting> implements IMeetingService
{
    @Autowired
    private MeetingMapper meetingMapper;

    /**
     * 查询会议信息
     * 
     * @param id 会议信息主键
     * @return 会议信息
     */
    @Override
    public Meeting selectMeetingById(Long id)
    {
        return meetingMapper.selectMeetingById(id);
    }

    /**
     * 查询会议信息列表
     * 
     * @param meeting 会议信息
     * @return 会议信息
     */
    @Override
    public List<Meeting> selectMeetingList(Meeting meeting)
    {
        return meetingMapper.selectMeetingList(meeting);
    }

    /**
     * 新增会议信息
     * 
     * @param meeting 会议信息
     * @return 结果
     */
    @Override
    public int insertMeeting(Meeting meeting)
    {
        meeting.setCreateTime(DateUtils.getNowDate());
        return meetingMapper.insertMeeting(meeting);
    }

    /**
     * 修改会议信息
     * 
     * @param meeting 会议信息
     * @return 结果
     */
    @Override
    public int updateMeeting(Meeting meeting)
    {
        meeting.setUpdateTime(DateUtils.getNowDate());
        return meetingMapper.updateMeeting(meeting);
    }

    /**
     * 批量删除会议信息
     * 
     * @param ids 需要删除的会议信息主键
     * @return 结果
     */
    @Override
    public int deleteMeetingByIds(Long[] ids)
    {
        return meetingMapper.deleteMeetingByIds(ids);
    }

    /**
     * 删除会议信息信息
     * 
     * @param id 会议信息主键
     * @return 结果
     */
    @Override
    public int deleteMeetingById(Long id)
    {
        return meetingMapper.deleteMeetingById(id);
    }
}
