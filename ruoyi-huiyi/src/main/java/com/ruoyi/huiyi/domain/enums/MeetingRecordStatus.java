package com.ruoyi.huiyi.domain.enums;

public enum MeetingRecordStatus {

    NOT_STARTED(0, "未开始"),
    RECORDING(1, "录制中"),
    PAUSED(2, "已暂停"),
    STOP_PENDING(3, "结束待处理"),
    PROCESSING(4, "离线处理中"),
    COMPLETED(5, "已完成"),
    FAILED(6, "异常终止");

    private final int code;
    private final String desc;

    MeetingRecordStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static MeetingRecordStatus of(int code) {
        for (MeetingRecordStatus s : values()) {
            if(s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("非法的录制状态码: " + code);
    }
}
