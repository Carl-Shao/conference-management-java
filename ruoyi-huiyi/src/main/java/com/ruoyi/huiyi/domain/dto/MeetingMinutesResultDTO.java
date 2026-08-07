package com.ruoyi.huiyi.domain.dto;

import java.io.Serializable;

public class MeetingMinutesResultDTO implements Serializable {

    private final static long serialVersionUID = 1L;

    /** 会议主题 */
    private String title;

    /** 关键词 */
    private String summary;

    /** 完整纪要正文，markdown格式 */
    private String content;

    public MeetingMinutesResultDTO(String title, String summary, String content) {
        this.title = title;
        this.summary = summary;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
}
