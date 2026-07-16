package com.ruoyi.huiyi.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.huiyi.domain.enums.BookRoomStatus;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 预约会议室对象 room_booking
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public class RoomBooking extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // 预约ID
    private Long bookingId;

    // 预约号
    @Excel(name = "预约单号")
    private String bookNo;

    // 员工号
    @Excel(name = "员工工号")
    private String empNo;

    // 系统用户ID
    private Long userId;

    // 会议室ID
    @Excel(name = "会议室ID")
    private Long roomId;

    // 会议室名称
    private String roomName;

    // 预约日期
    @Excel(name = "预约日期", width = 30, dateFormat = "yyyy_mm__dd")
    private Date bookDate;

    // 开始日期
    @Excel(name = "开始时间")
    private String startTime;

    @Excel(name = "结束时间")
    private String endTime;

    // 关联会议ID
    private Long meetingId;

    // 预约状态
    @Excel(name = "预约状态", readConverterExp = "0=待确认,1=已确认,2=已取消,3=已完成")
    private String bookStatus;

    // 预约事由
    @Excel(name = "预约事由")
    private String bookPurpose;

    // 取消原因
    private String cancelReason;

    // 乐观锁版本号
    private Integer version;

    public Long getBookingId() {return bookingId;}

    public void setBookingId(Long bookingId) {this.bookingId = bookingId;}

    public String getBookNo() {return bookNo;}

    public void setBookNo(String bookNo) {this.bookNo = bookNo;}

    public String getEmpNo() {return empNo;}

    public void setEmpNo(String empNo) {this.empNo = empNo;}

    public Long getUserId() {return userId;}

    public void setUserId(Long userId) {this.userId = userId;}

    public Long getRoomId() {return roomId;}

    public void setRoomId(Long roomId) {this.roomId = roomId;}

    public String getRoomName() {return roomName;}

    public void setRoomName(String roomName) {this.roomName = roomName;}

    public Date getBookDate() {return bookDate;}

    public void setBookDate(Date bookDate) {this.bookDate = bookDate;}

    public String getStartTime() {return startTime;}

    public void setStartTime(String startTime) {this.startTime = startTime;}

    public String getEndTime() {return endTime;}

    public void setEndTime(String endTime) {this.endTime = endTime;}

    public Long getMeetingId() {return meetingId;}

    public void setMeetingId(Long meetingId) {this.meetingId = meetingId;}

    public String getBookStatus() {return bookStatus;}

    public void setBookStatus(String bookStatus) {this.bookStatus = bookStatus;}

    public String getBookPurpose() {return bookPurpose;}

    public void setBookPurpose(String bookPurpose) {this.bookPurpose = bookPurpose;}

    public String getCancelReason() {return cancelReason;}

    public void setCancelReason(String cancelReason) {this.cancelReason = cancelReason;}

    public Integer getVersion() {return version;}

    public void setVersion(Integer version) {this.version = version;}

    public BookRoomStatus statusEnum() {
        return BookRoomStatus.fromCode(this.bookStatus);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("bookingId", getBookingId())
                .append("bookNo", getBookNo())
                .append("empNo", getEmpNo())
                .append("userId", getUserId())
                .append("roomId", getRoomId())
                .append("bookDate", getBookDate())
                .append("startTime", getStartTime())
                .append("endTime", getEndTime())
                .append("meetingId", getMeetingId())
                .append("bookStatus", getBookStatus())
                .append("bookPurpose", getBookPurpose())
                .append("cancelReason", getCancelReason())
                .append("version", getVersion())
                .append("remark", getRemark())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
