package com.ruoyi.huiyi.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.huiyi.mapper.MeetingEmployeeMapper;
import com.ruoyi.huiyi.domain.MeetingEmployee;
import com.ruoyi.huiyi.service.IMeetingEmployeeService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 员工信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@Service
public class MeetingEmployeeServiceImpl implements IMeetingEmployeeService
{
    @Autowired
    private MeetingEmployeeMapper meetingEmployeeMapper;

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 根据员工角色确定系统角色ID
     * @param employeeRole 员工角色(manager:管理员，participants:普通员工)
     * @return 系统角色ID数组
     */
    private Long[] determineRoleIdsForEmployee(String employeeRole) {
        // 注意：这需要根据实际数据库中的角色ID进行调整
        if ("manager".equals(employeeRole)) {
            // 管理员角色，例如ID为2
            return new Long[]{100L}; // 需要根据实际情况修改
        } else {
            // 默认为普通参与者角色，例如ID为1
            return new Long[]{101L}; // 需要根据实际情况修改
        }
    }

    /**
     * 根据工号检查员工是否唯一
     *
     * @param employeeNo 员工工号
     * @return 员工信息，如果不存在则返回null
     */
    @Override
    public MeetingEmployee checkEmployeeNoUnique(String employeeNo)
    {
        return meetingEmployeeMapper.checkEmployeeNoUnique(employeeNo);
    }

    /**
     * 查询员工信息
     * 
     * @param id 员工信息主键
     * @return 员工信息
     */
    @Override
    public MeetingEmployee selectMeetingEmployeeById(Long id)
    {
        return meetingEmployeeMapper.selectMeetingEmployeeById(id);
    }

    /**
     * 查询员工信息列表
     * 
     * @param meetingEmployee 员工信息
     * @return 员工信息
     */
    @Override
    public List<MeetingEmployee> selectMeetingEmployeeList(MeetingEmployee meetingEmployee)
    {
        return meetingEmployeeMapper.selectMeetingEmployeeList(meetingEmployee);
    }

    /**
     * 新增员工信息
     *
     * @param meetingEmployee 员工信息
     * @return 结果
     */
    @Transactional
    @Override
    public int insertMeetingEmployee(MeetingEmployee meetingEmployee)
    {
        // 首先检查工号是否已存在
        MeetingEmployee existingEmployee = meetingEmployeeMapper.checkEmployeeNoUnique(meetingEmployee.getEmployeeNo());
        if (existingEmployee != null) {
            throw new RuntimeException("工号 [" + meetingEmployee.getEmployeeNo() + "] 已存在！员工信息：" + existingEmployee.getEmployeeName());
        }

        // 检查用户名（与工号相同）是否已存在于系统用户表中
        SysUser existingUser = sysUserService.selectUserByUserName(meetingEmployee.getEmployeeNo());
        if (existingUser != null) {
            throw new RuntimeException("用户名 [" + meetingEmployee.getEmployeeNo() + "] 已存在！无法创建重复的系统用户！");
        }

        // 创建系统用户
        SysUser sysUser = new SysUser();
        sysUser.setUserName(meetingEmployee.getEmployeeNo()); // 使用员工工号作为用户名
        sysUser.setNickName(meetingEmployee.getEmployeeName()); // 使用员工姓名作为昵称
        sysUser.setEmail(meetingEmployee.getEmail());
        sysUser.setPhonenumber(meetingEmployee.getPhone());
        sysUser.setSex("0");
        sysUser.setPassword(SecurityUtils.encryptPassword("123456")); // 初始密码设置为123456并加密
        sysUser.setStatus(UserConstants.NORMAL); // 设置为正常状态
        sysUser.setDelFlag("0"); // 未删除

        // 设置默认值
        sysUser.setCreateBy(SecurityUtils.getUsername()); // 获取当前登录用户作为创建人
        sysUser.setCreateTime(DateUtils.getNowDate());

        // 根据员工角色设置系统用户的角色
        Long[] roleIds = determineRoleIdsForEmployee(meetingEmployee.getRole());
        if (roleIds != null && roleIds.length > 0) {
            sysUser.setRoleIds(roleIds);
        }

        // 插入系统用户并获取用户ID
        int rows = sysUserService.insertUser(sysUser);

        if (rows > 0) {
            // 获取新创建的用户ID并设置到会议员工对象
            Long userId = sysUser.getUserId(); // 假设插入后会自动设置ID
            meetingEmployee.setUserId(userId);

            // 设置创建时间
            meetingEmployee.setCreateTime(DateUtils.getNowDate());

            // 插入会议员工信息
            return meetingEmployeeMapper.insertMeetingEmployee(meetingEmployee);
        } else {
            throw new RuntimeException("创建系统用户失败");
        }
    }

    /**
     * 修改员工信息
     *
     * @param meetingEmployee 员工信息
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMeetingEmployee(MeetingEmployee meetingEmployee)
    {
        meetingEmployee.setUpdateTime(DateUtils.getNowDate());

        // 更新会议员工信息
        int result = meetingEmployeeMapper.updateMeetingEmployee(meetingEmployee);

        // 如果员工有关联的系统用户，则同时更新系统用户信息
        if (meetingEmployee.getUserId() != null) {
            SysUser sysUser = sysUserService.selectUserById(meetingEmployee.getUserId());
            if (sysUser != null) {
                sysUser.setNickName(meetingEmployee.getEmployeeName());
                sysUser.setEmail(meetingEmployee.getEmail());
                sysUser.setPhonenumber(meetingEmployee.getPhone());

                sysUserService.updateUser(sysUser);
            }
        }

        return result;
    }

    /**
     * 批量删除员工信息
     *
     * @param ids 需要删除的员工信息主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMeetingEmployeeByIds(Long[] ids)
    {
        // 找出所有需要删除的员工信息及其关联的用户ID
        for (Long id : ids) {
            MeetingEmployee meetingEmployee = meetingEmployeeMapper.selectMeetingEmployeeById(id);
            if (meetingEmployee != null && meetingEmployee.getUserId() != null) {
                // 删除关联的系统用户
                sysUserService.deleteUserById(meetingEmployee.getUserId());
            }
        }

        return meetingEmployeeMapper.deleteMeetingEmployeeByIds(ids);
    }

    /**
     * 删除员工信息信息
     *
     * @param id 员工信息主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMeetingEmployeeById(Long id)
    {
        // 先查询员工信息，获取关联的用户ID
        MeetingEmployee meetingEmployee = meetingEmployeeMapper.selectMeetingEmployeeById(id);
        if (meetingEmployee != null && meetingEmployee.getUserId() != null) {
            // 删除关联的系统用户
            sysUserService.deleteUserById(meetingEmployee.getUserId());
        }

        return meetingEmployeeMapper.deleteMeetingEmployeeById(id);
    }
}
