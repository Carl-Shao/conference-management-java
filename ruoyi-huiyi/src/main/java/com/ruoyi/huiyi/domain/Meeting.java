package com.ruoyi.huiyi.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会议信息对象 meeting
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Meeting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会议 ID（主键） */
    private Long id;

    /** 会议主题 */
    @Excel(name = "会议主题")
    private String title;

    /** 会议室 ID（关联 meeting_room 表） */
    @Excel(name = "会议室 ID", readConverterExp = "关=联,m=eeting_room,表=")
    private Long roomId;

    /** 会议日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "会议日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date meetingDate;

    /** 开始时间 */
    @Excel(name = "开始时间", width = 30, dateFormat = "HH:mm:ss")
    private String startTime;

    /** 结束时间 */
    @Excel(name = "结束时间", width = 30, dateFormat = "HH:mm:ss")
    private String endTime;

    /** 组织者工号（关联 meeting_employee 表） */
    @Excel(name = "组织者工号", readConverterExp = "关=联,m=eeting_employee,表=")
    private String organizerNo;

    /** 参会人员工号数组 (JSON 格式，如：["E001","E002","E003"]) */
    @Excel(name = "参会人员工号数组", readConverterExp = "J=SON,格=式，如：=[\"E001\",\"E002\",\"E003\"]")
    private String participantNos;

    /** 会议描述 */
    @Excel(name = "会议描述")
    private String description;

    /** 会议状态（scheduled:预定，active:进行中，completed:已结束，cancelled:已取消） */
    @Excel(name = "会议状态", readConverterExp = "s=cheduled:预定，active:进行中，completed:已结束，cancelled:已取消")
    private String status;
}
