package com.ruoyi.huiyi.mapper;

import java.util.List;
import com.ruoyi.huiyi.domain.MeetingEmployee;

/**
 * 员工信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
public interface MeetingEmployeeMapper 
{
    /**
     * 查询员工信息
     * 
     * @param id 员工信息主键
     * @return 员工信息
     */
    public MeetingEmployee selectMeetingEmployeeById(Long id);

    /**
     * 查询员工信息列表
     *
     * @param meetingEmployee 员工信息
     * @return 员工信息集合
     */
    public List<MeetingEmployee> selectMeetingEmployeeList(MeetingEmployee meetingEmployee);

    /**
     * 根据工号检查员工是否唯一
     *
     * @param employeeNo 员工工号
     * @return 员工信息，如果不存在则返回null
     */
    public MeetingEmployee checkEmployeeNoUnique(String employeeNo);

    /**
     * 新增员工信息
     * 
     * @param meetingEmployee 员工信息
     * @return 结果
     */
    public int insertMeetingEmployee(MeetingEmployee meetingEmployee);

    /**
     * 修改员工信息
     * 
     * @param meetingEmployee 员工信息
     * @return 结果
     */
    public int updateMeetingEmployee(MeetingEmployee meetingEmployee);

    /**
     * 删除员工信息
     * 
     * @param id 员工信息主键
     * @return 结果
     */
    public int deleteMeetingEmployeeById(Long id);

    /**
     * 批量删除员工信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMeetingEmployeeByIds(Long[] ids);
}
