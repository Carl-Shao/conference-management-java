package com.ruoyi.huiyi.domain.vo;

import java.util.Date;
import java.util.List;

public class RoomFreeTimeVO {

    // 会议室Id
    private Long roomId;

    // 会议室名称
    private String roomName;

    // 查询日期
    private Date bookDate;

    // 是否开放
    private boolean open;

    // 空闲时间段列表，扣除已生效的预约
    private List<TimeRangeVO> freeRanges;

    public Long getRoomId() {return roomId;}

    public void setRoomId(Long roomId) {this.roomId = roomId;}

    public String getRoomName() {return roomName;}

    public void setRoomName(String roomName) {this.roomName = roomName;}

    public Date getBookDate() {return bookDate;}

    public void setBookDate(Date bookDate) {this.bookDate = bookDate;}

    public boolean isOpen() {return open;}

    public void setOpen(boolean open) {this.open = open;}

    public List<TimeRangeVO> getFreeRanges() {return freeRanges;}

    public void setFreeRanges(List<TimeRangeVO> freeRanges) {this.freeRanges = freeRanges;}
}
