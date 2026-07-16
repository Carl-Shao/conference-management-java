package com.ruoyi.huiyi.domain;

import com.ruoyi.common.annotation.Excel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 会议室时段 room_time_slot
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public class RoomTimeSlot extends BaseEntity{
    private static final long serialVersionUID = 1L;

    /** 时段ID */
    private Long slotId;

    /** 会议室ID */
    @Excel(name = "会议室ID")
    private Long roomId;

    /** 日期 */
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date slotDate;

    /** 开放开始时间 HH:mm:ss */
    @Excel(name = "开始时间")
    private String startTime;

    /** 开放结束时间 HH:mm:ss */
    @Excel(name = "结束时间")
    private String endTime;

    /** 时段状态（0开放 1关闭/维护） */
    @Excel(name = "状态", readConverterExp = "0=开放,1=关闭")
    private String slotStatus;

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Date getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(Date slotDate) {
        this.slotDate = slotDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(String slotStatus) {
        this.slotStatus = slotStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("slotId", getSlotId())
                .append("roomId", getRoomId())
                .append("slotDate", getSlotDate())
                .append("startTime", getStartTime())
                .append("endTime", getEndTime())
                .append("slotStatus", getSlotStatus())
                .append("remark", getRemark())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
