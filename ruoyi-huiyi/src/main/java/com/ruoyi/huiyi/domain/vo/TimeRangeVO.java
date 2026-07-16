package com.ruoyi.huiyi.domain.vo;

import java.time.LocalTime;

public class TimeRangeVO {

    private LocalTime startTime;

    private LocalTime endTime;

    public TimeRangeVO() {}

    public TimeRangeVO(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {return startTime;}

    public void setStartTime(LocalTime startTime) {this.startTime = startTime;}

    public LocalTime getEndTime() {return endTime;}

    public void setEndTime(LocalTime endTime) {this.endTime = endTime;}

    public boolean overLap(TimeRangeVO other) {
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    public boolean isValid() {return startTime != null && endTime != null && startTime.isBefore(endTime);}

    @Override
    public String toString() {return startTime + "-" + endTime;}
}
