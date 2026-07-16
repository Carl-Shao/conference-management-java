package com.ruoyi.huiyi.domain.vo;

public class RoomCurrentStatusVO {

    // 会议室ID
    private Long roomId;

    // 会议室名称
    private String roomName;

    // 是否被占用
    private boolean occupied;

    // 占用中，当前会议室的会议室预约单号
    private String currentBookNo;

    // 占用中，占用人工号
    private String currentEmpNo;

    // 占用中，事由
    private String currentPurpose;

    // 占用时，预计结束时间
    private String currentEndTime;

    public Long getRoomId() {return roomId;}

    public void setRoomId(Long roomId) {this.roomId = roomId;}

    public String getRoomName() {return roomName;}

    public void setRoomName(String roomName) {this.roomName = roomName;}

    public boolean isOccupied() {return occupied;}

    public void setOccupied(boolean occupied) {this.occupied = occupied;}

    public String getCurrentBookNo() {return currentBookNo;}

    public void setCurrentBookNo(String currentBookNo) {this.currentBookNo = currentBookNo;}

    public String getCurrentEmpNo() {return currentEmpNo;}

    public void setCurrentEmpNo(String currentEmpNo) {this.currentEmpNo = currentEmpNo;}

    public String getCurrentPurpose() {return currentPurpose;}

    public void setCurrentPurpose(String currentPurpose) {this.currentPurpose = currentPurpose;}

    public String getCurrentEndTime() {return currentEndTime;}

    public void setCurrentEndTime(String endTime) {this.currentEndTime = currentEndTime;}
}
