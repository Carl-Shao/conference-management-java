package com.ruoyi.huiyi.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会议资源（完整转录、会议概要、录音）对象 meeting_resource
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MeetingResource extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 资源 ID（主键） */
    private Long id;

    /** 会议 ID（关联 meeting 表） */
    @Excel(name = "会议 ID", readConverterExp = "关=联,m=eeting,表=")
    private Long meetingId;

    /** 会议主题 */
    @Excel(name = "会议主题")
    private String meetingTitle;

    /** 完整会议转录（逐字稿） */
    @Excel(name = "完整会议转录", readConverterExp = "逐=字稿")
    private String fullTranscript;

    /** 会议概要内容（简要总结） */
    @Excel(name = "会议概要内容", readConverterExp = "简=要总结")
    private String summaryContent;

    /** 录音文件路径 */
    @Excel(name = "录音文件路径")
    private String recordingPath;

}
