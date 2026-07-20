package com.ruoyi.huiyi.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 创建会议室预约请求参数
 *
 * @author ruoyi
 */
public class BookingCreateDTO {

    // 会议室ID
    @NotNull(message = "会议室不能为空")
    private Long roomId;

    // 预约日期
    @NotNull(message = "预约日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date bookDate;

    // 开始时间
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    // 结束时间
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    // 关联会议室
    private Long meetingId;

    // 预约事由
    private String bookPurpose;

    public Long getRoomId() {return roomId;}

    public void setRoomId(Long roomId) {this.roomId = roomId;}

    public Date getBookDate() {return bookDate;}

    public void setBookDate(Date bookDate) {this.bookDate = bookDate;}

    public String getStartTime() {return startTime;}

    public void setStartTime(String startTime) {this.startTime = startTime;}

    public String getEndTime() {return endTime;}

    public void setEndTime(String endTime) {this.endTime = endTime;}

    public Long getMeetingId() {return meetingId;}

    public void setMeetingId(Long meetingId) {this.meetingId = meetingId;}

    public String getBookPurpose() {return bookPurpose;}

    public void setBookPurpose(String bookPurpose) {this.bookPurpose = bookPurpose;}
}
