package com.ruoyi.huiyi.domain.enums;

/**
 * 会议来源类型，对应 huiyi_meeting.source_type 字段。
 * code 用 String 是为了跟 RuoYi 字典字段（char(1) + sys_dict_data）风格保持一致。
 */
public enum MeetingSourceType
{
    RECORDING("0", "录制音频"),
    UPLOAD("1", "上传音频"),
    MERGED("2", "合并音频");

    private final String code;
    private final String desc;

    MeetingSourceType(String code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static MeetingSourceType of(String code)
    {
        for (MeetingSourceType type : values())
        {
            if (type.code.equals(code))
            {
                return type;
            }
        }
        return null;
    }
}