package com.ruoyi.huiyi.mq.dto;

public class AudioUploadTask {
    private Long meetingId;
    private String filePath;

    public AudioUploadTask() {}

    public AudioUploadTask(Long meetingId, String filePath)
    {
        this.meetingId = meetingId;
        this.filePath = filePath;
    }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
