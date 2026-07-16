package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.RoomBooking;

/**
 * 会议室状态Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public interface BookRoomMapper {
    /**
     * 查询会议室预约信息
     *
     * @param RoomId          会议室信息主键
     * @return                返回结果
     */
    public RoomBooking selectRoomInfo(Long RoomId);

    /**
     * 预约会议室
     *
     * @param EmployeeId      员工信息主键
     * @param RoomId          会议室信息主键
     * @return                返回结果
     */
    public int bookMeetingRome(Long EmployeeId, Long RoomId);

    /**
     * 更新预约会议室
     *
     * @param EmployeeId      员工信息主键
     * @param RoomId          会议室信息主键
     * @return                返回结果
     */
    public int updateBookRoom(Long EmployeeId, Long RoomId);

    /**
     * 取消预约会议室
     *
     * @param EmployeeId      员工信息主键
     * @param RoomId          会议室信息主键
     * @return                返回结果
     */
    public int cancelBookRoom(Long EmployeeId, Long RoomId);
}
