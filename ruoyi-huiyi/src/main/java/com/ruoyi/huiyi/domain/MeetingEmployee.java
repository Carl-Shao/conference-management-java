package com.ruoyi.huiyi.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 员工信息对象 meeting_employee
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MeetingEmployee extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 员工 ID（主键） */
    private Long id;

    /** 工号（如：E001） */
    @Excel(name = "工号", readConverterExp = "如=：E001")
    private String employeeNo;

    /** 姓名 */
    @Excel(name = "姓名")
    private String employeeName;

    /** 部门（如：技术部） */
    @Excel(name = "部门", readConverterExp = "如=：技术部")
    private String department;

    /** 职位（如：工程师） */
    @Excel(name = "职位", readConverterExp = "如=：工程师")
    private String position;

    /** 邮箱地址 */
    @Excel(name = "邮箱地址")
    private String email;

    /** 电话号码 */
    @Excel(name = "电话号码")
    private String phone;

    /** 角色（manager:管理员，participants:普通员工） */
    @Excel(name = "角色", readConverterExp = "manager:管理员，participants:普通员工")
    private String role;

    /** 关联的系统用户ID */
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

}
