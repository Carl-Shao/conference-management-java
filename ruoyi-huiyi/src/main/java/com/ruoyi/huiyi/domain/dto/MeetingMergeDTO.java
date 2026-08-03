package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;
import java.util.List;

public class MeetingMergeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待合并的会议ID列表，按此顺序拼接转写内容 */
    private List<Long> meetingIds;

    /** 合并后的新会议标题 */
    private String title;

    /** 合并后是否立即触发LLM重新生成纪要（走现有RabbitMQ生成队列） */
    private boolean regenerateMinutes = true;

    public List<Long> getMeetingIds() { return meetingIds; }
    public void setMeetingIds(List<Long> meetingIds) { this.meetingIds = meetingIds; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isRegenerateMinutes() { return regenerateMinutes; }
    public void setRegenerateMinutes(boolean regenerateMinutes) { this.regenerateMinutes = regenerateMinutes; }
}
