package com.ruoyi.huiyi.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import java.util.Date;

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
    private Long duration;

    /** 处理状态（0待转写 1转写中 2转写完成 3生成中 4已完成 5失败） */
    private Integer status;

    /** 是否收藏（0否 1是） */
    private String isFavorite;

    /** 按文件夹（标签）过滤时用，不映射数据库列（现在是多对多关联表，不是单列了） */
    private Long folderId;

    /** 排除已经在某个文件夹（标签）里的会议，不映射数据库列。
     *  给"往文件夹里加会议"的选择器用：传当前这个folderId，列表就只返回还没加进来的会议，不会出现重复可选 */
    private Long excludeFolderId;

    /** 该会议当前打了哪些文件夹（标签），不映射数据库列，查详情/列表时按需从关联表填充展示用 */
    private java.util.List<Long> folderIds;

    /** 合并来源的会议ID，逗号分隔 */
    private String mergeFromIds;

    /** 是否已被合并（0否 1是） */
    private String isMerged;

    /** 录制状态（仅sourceType=0录制场景使用，取值对应你的 MeetingRecordStatus 枚举） */
    private Integer recordStatus;

    /** 录制开始时间 */
    private Date recordStartTime;

    /** 录制结束时间 */
    private Date recordEndTime;

    /** 纪要概括总结 **/
    private String summary;

    /** 搜索关键字 **/
    private String keyword;

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getIsFavorite() { return isFavorite; }
    public void setIsFavorite(String isFavorite) { this.isFavorite = isFavorite; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public Long getExcludeFolderId() { return excludeFolderId; }
    public void setExcludeFolderId(Long excludeFolderId) { this.excludeFolderId = excludeFolderId; }

    public java.util.List<Long> getFolderIds() { return folderIds; }
    public void setFolderIds(java.util.List<Long> folderIds) { this.folderIds = folderIds; }

    public String getMergeFromIds() { return mergeFromIds; }
    public void setMergeFromIds(String mergeFromIds) { this.mergeFromIds = mergeFromIds; }

    public String getIsMerged() { return isMerged; }
    public void setIsMerged(String isMerged) { this.isMerged = isMerged; }

    public Integer getRecordStatus() { return recordStatus; }
    public void setRecordStatus(Integer recordStatus) {this.recordStatus = recordStatus; }

    public Date getRecordStartTime() { return recordStartTime; }
    public void setRecordStartTime(Date recordStartTime) { this.recordStartTime = recordStartTime; }

    public Date getRecordEndTime() { return recordEndTime; }
    public void setRecordEndTime(Date recordEndTime) { this.recordEndTime = recordEndTime; }

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeyword() {
        return keyword;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
