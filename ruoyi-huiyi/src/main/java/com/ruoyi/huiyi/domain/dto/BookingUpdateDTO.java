package com.ruoyi.huiyi.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 修改会议室预约请求参数
 *
 * @author ruoyi
 */
public class BookingUpdateDTO {

    // 预约ID
    @NotNull(message = "预约ID不能为空")
    private Long bookingId;

    // 新的会议室ID
    @NotNull(message = "会议室不能为空")
    private Long roomId;

    // 新的预约日期
    @NotNull(message = "预约日期不能为空")
    @DateTimeFormat(pattern = "yyyy_mm_dd")
    private Date bookDate;

    // 新的开始时间
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    // 新的结束时间
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    // 预约事由
    private String bookPurpose;

    public Long getBookingId() {return bookingId;}

    public void setBookingId(Long bookingId) {this.bookingId = bookingId;}

    public Long getRoomId() {return roomId;}

    public void setRoomId(Long roomId) {this.roomId = roomId;}

    public Date getBookDate() {return bookDate;}

    public void setBookDate(Date bookDate) {this.bookDate = bookDate;}

    public String getStartTime() {return startTime;}

    public void setStartTime(String startTime) {this.startTime = startTime;}

    public String getEndTime() {return endTime;}

    public void setEndTime(String endTime) {this.endTime = endTime;}

    public String getBookPurpose() {return bookPurpose;}

    public void setBookPurpose(String bookPurpose) {this.bookPurpose = bookPurpose;}
}
