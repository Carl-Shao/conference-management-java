package com.ruoyi.huiyi.domain.enums;

import com.ruoyi.common.exception.ServiceException;

/**
 * 会议室预约状态枚举
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public enum BookRoomStatus {

    /** 待确认：刚提交，等待审批/自动确认 */
    PENDING("0", "待确认"),

    /** 已确认：预约生效，占用对应时间段 */
    CONFIRMED("1", "已确认"),

    /** 已取消：员工主动取消或系统取消 */
    CANCELLED("2", "已取消"),

    /** 已完成：会议时间已结束，系统流转的终态 */
    COMPLETED("3", "已完成");

    private final String code;
    private final String info;

    BookRoomStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {return code;}

    public String getInfo() {return info;}

    /**
     * 根据数据库存储的编码解析枚举
     */
    public static BookRoomStatus fromCode(String code) {
        for(BookRoomStatus status : values()) {
            if (status.code.equals(code)){
                return status;
            }
        }
        throw new ServiceException("未知的预约状态编码：" + code);
    }

    /**
     * 该状态下是否允许取消
     */
    public boolean isCancellable() {return this == PENDING || this == CONFIRMED;}

    /**
     * 该状态下是否允许修改（改期/改时间段）
     */
    public boolean isModifiable() {return this == PENDING || this == CONFIRMED;}

    /**
     * 该状态是否会实际占用时间段（用于冲突检测/空闲时间计算）
     */
    public boolean occupiesTimeSlot() {
        return this == PENDING || this == CONFIRMED;
    }
}
