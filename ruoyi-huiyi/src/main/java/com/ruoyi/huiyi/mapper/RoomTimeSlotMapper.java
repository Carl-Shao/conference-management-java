package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.RoomTimeSlot;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 会议室时段（开放窗口） Mapper 接口
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public interface RoomTimeSlotMapper {
    /**
     * 查询某会议室某天的"维护/关闭"黑名单时段（默认全天可约，只有命中这些时段才不可约）
     */
    List<RoomTimeSlot> selectClosedSlotsByRoomsAndDate(@Param("roomId") Long roomId, @Param("slotDate") Date slotDate);

    /**
     * 新增/维护时段配置（管理员用）
     */
    int insertRoomTimeSlot(RoomTimeSlot slot);

    int updateRoomTimeSlot(RoomTimeSlot slot);

    int deleteRoomTimeSlot(Long slotId);
}
