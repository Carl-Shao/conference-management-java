package com.ruoyi.huiyi.mapper;

import com.ruoyi.huiyi.domain.RoomBooking;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 会议室预约 Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-16
 */
public interface BookRoomMapper {

    /**
     * 查询预约详情
     */
    RoomBooking selectBookRoomBookingById(Long bookingId);

    /**
     * 加行锁查询预约详情，用于"取消"、"修改"等需要防止并发操作同一条预约的场景。
     * 对应 SQL: SELECT * FROM room_booking WHERE booking_id = #{bookingId} FOR UPDATE
     */
    RoomBooking selectBookRoomBookingByIdForUpdate(Long bookingId);

    /**
     * 分页/条件查询预约列表
     */
    List<RoomBooking> selectBookRoomBookingList(RoomBooking query);

    /**
     * 查询某会议室在某天，当前仍然"占用时间段"的有效预约（待确认 + 已确认）
     * 用于冲突检测和空闲时间计算
     */
    List<RoomBooking> selectActiveBookingsByRoomAndDate(@Param("roomId") Long roomId,
                                                        @Param("bookDate") Date bookDate);

    /**
     * 排除指定的预约ID（修改预约时，不应该把自己算作冲突）
     */
    List<RoomBooking> selectActiveBookingsByRoomAndDateExcludeId(@Param("roomId") Long roomId,
                                                                 @Param("bookDate") Date bookDate,
                                                                 @Param("excludeBookingId") Long excludeBookingId);

    /**
     * 新增预约
     */
    int insertBookRoomBooking(RoomBooking booking);

    /**
     * 修改预约（改期/改时间段/改事由），使用乐观锁 version 做二次防御
     */
    int updateBookRoomBooking(RoomBooking booking);

    /**
     * 取消预约
     */
    int cancelBookRoomBooking(@Param("bookingId") Long bookingId,
                              @Param("cancelReason") String cancelReason,
                              @Param("updateBy") String updateBy);

    /**
     * 批量删除预约（管理员维护用）
     */
    int deleteBookRoomBookingByIds(Long[] bookingIds);

    /**
     * 查询"此刻"正在占用某会议室的预约（用于会议室实时占用状态展示）
     * 条件：book_date = 今天 AND start_time &lt;= 当前时间 &lt; end_time AND 状态为待确认/已确认
     * 若返回 null 则代表该会议室此刻空闲。
     */
    RoomBooking selectCurrentOccupyingBooking(@Param("roomId") Long roomId,
                                              @Param("bookDate") Date bookDate,
                                              @Param("currentTime") String currentTime);

    /**
     * 批量版本：一次查出多个会议室"此刻"正在占用的预约，用于会议室列表页展示占用状态，
     * 避免对每个会议室单独查一次数据库（N+1 问题）。
     */
    List<RoomBooking> selectCurrentOccupyingBookings(@Param("roomIds") List<Long> roomIds,
                                                     @Param("bookDate") LocalDate bookDate,
                                                     @Param("currentTime") String currentTime);
}
