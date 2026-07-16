package com.ruoyi.huiyi.domain.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 会议室时段状态枚举
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public enum SlotStatus {

    // 关闭/维护
    CLOSED("1", "关闭/维护");

    private final String code;
    private final String info;

    SlotStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {return code;}

    public String getIndo() {return info;}

    public static SlotStatus fromCode(String code) {
        for (SlotStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new ServiceException("未知的时段状态编码：" + code);
    }
}
