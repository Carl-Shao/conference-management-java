package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.RoomBooking;
import com.ruoyi.huiyi.domain.dto.BookingCreateDTO;
import com.ruoyi.huiyi.domain.dto.BookingUpdateDTO;
import com.ruoyi.huiyi.domain.vo.RoomCurrentStatusVO;
import com.ruoyi.huiyi.domain.vo.RoomFreeTimeVO;

import java.util.Date;
import java.util.List;

/**
 * 预约会议室Service接口
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public interface IBookMeetingRoomService {

    /**
     * 查询会议室预约信息
     *
     * @param bookingId       会议室信息主键
     * @return                返回结果
     */
    RoomBooking selectBookingById(Long bookingId);

    /**
     * 条件查询预约列表
     *
     * @param query
     * @return                返回结果
     */
    List<RoomBooking> selectBookingList(RoomBooking query);

    /**
     * 查询会议室某天空闲时间
     *
     * @param roomId
     * @param bookDate
     * @return
     */
    RoomFreeTimeVO queryFreeTime(Long roomId, Date bookDate);

    /**
     * 查询单个会议室此刻是否被占用
     * @param roomId
     */
    RoomCurrentStatusVO queryCurrentStatus(Long roomId);

    /**
     * 查询多个会议室此刻是否被占用
     * @param roomIds
     */
    List<RoomCurrentStatusVO> queryCurrentStatusList(List<Long> roomIds);

    /**
     * 预约会议室
     *
     * @param dto
     * @return       预约会议室主键
     */
    Long createBooking(BookingCreateDTO dto);

    /**
     * 更新预约会议室
     *
     * @param dto
     */
    void updateBooking(BookingUpdateDTO dto);

    /**
     * 取消预约会议室
     *
     * @param bookingId
     * @param cancelReason
     */
    void cancelBooking(Long bookingId, String cancelReason);
}
