package com.ruoyi.huiyi.domain.enums;

public enum MeetingStatus {

    PENDING_TRANSCRIBE(0, "待转写"),
    TRANSCRIBING(1, "转写中"),
    TRANSCRIBED(2, "转写完成"),
    GENERATING(3, "生成中"),
    DONE(4, "已完成"),
    FAILED(5, "失败");

    private final int code;
    private final String desc;

    MeetingStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static MeetingStatus of(int code) {
        for (MeetingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的会议处理状态码: " + code);
    }
}
