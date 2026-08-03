package com.ruoyi.huiyi.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会议纪要主表 huiyi_meeting
 *
 * 说明：source_type / status / is_favorite / is_merged 均使用 char(1) 字符串，
 * 与 RuoYi 其他模块的字典字段风格保持一致，方便前端用 el-tag + dict 渲染。
 */
public class MeetingRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 会议记录ID */
    private Long meetingId;

    /** 会议标题 */
    @Excel(name = "会议标题")
    private String title;

    /** 来源（0录制音频 1上传音频） */
    @Excel(name = "来源", readConverterExp = "0=录制音频,1=上传音频")
    private String sourceType;

    /** 音频文件路径 */
    private String audioPath;

    /** 会议时长（秒） */
    @Excel(name = "会议时长(秒)")
    private Integer duration;

    /** 处理状态（0待转写 1转写中 2转写完成 3生成中 4已完成 5失败） */
    private String status;

    /** 是否收藏（0否 1是） */
    private String isFavorite;

    /** 所属文件夹ID */
    private Long folderId;

    /** 合并来源的会议ID，逗号分隔 */
    private String mergeFromIds;

    /** 是否已被合并（0否 1是） */
    private String isMerged;

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIsFavorite() { return isFavorite; }
    public void setIsFavorite(String isFavorite) { this.isFavorite = isFavorite; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public String getMergeFromIds() { return mergeFromIds; }
    public void setMergeFromIds(String mergeFromIds) { this.mergeFromIds = mergeFromIds; }

    public String getIsMerged() { return isMerged; }
    public void setIsMerged(String isMerged) { this.isMerged = isMerged; }
}
