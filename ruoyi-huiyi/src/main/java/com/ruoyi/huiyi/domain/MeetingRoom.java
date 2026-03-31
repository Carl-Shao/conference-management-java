package com.ruoyi.huiyi.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会议室信息对象 meeting_room
 * 
 * @author ruoyi
 * @date 2026-03-26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MeetingRoom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会议室 ID（主键） */
    private Long id;

    /** 会议室名称（如：会议室 A） */
    @Excel(name = "会议室名称", readConverterExp = "如=：会议室,A=")
    private String roomName;

    /** 会议室位置（如：3 楼 301 室） */
    @Excel(name = "会议室位置", readConverterExp = "如=：3,楼=,3=01,室=")
    private String location;

    /** 会议室容量（可容纳人数） */
    @Excel(name = "会议室容量", readConverterExp = "可=容纳人数")
    private Long capacity;

    /** 设备列表（多个设备用逗号分隔，如：投影仪，音响，白板） */
    @Excel(name = "设备列表", readConverterExp = "多=个设备用逗号分隔，如：投影仪，音响，白板")
    private String equipment;

    /** RTSP 流地址（用于音频采集） */
    @Excel(name = "RTSP 流地址", readConverterExp = "用=于音频采集")
    private String rtspUrl;

    /** 会议室状态（available:空闲，occupied:占用，maintenance:维护） */
    @Excel(name = "会议室状态", readConverterExp = "a=vailable:空闲，occupied:占用，maintenance:维护")
    private String status;

    /** 排序号（数字越小越靠前） */
    @Excel(name = "排序号", readConverterExp = "数=字越小越靠前")
    private Long sortOrder;
}
