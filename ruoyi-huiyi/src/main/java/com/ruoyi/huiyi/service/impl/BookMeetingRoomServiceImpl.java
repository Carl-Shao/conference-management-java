package com.ruoyi.huiyi.service.impl;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.huiyi.domain.MeetingRoom;
import com.ruoyi.huiyi.domain.RoomBooking;
import com.ruoyi.huiyi.domain.RoomTimeSlot;
import com.ruoyi.huiyi.domain.dto.BookingCreateDTO;
import com.ruoyi.huiyi.domain.dto.BookingUpdateDTO;
import com.ruoyi.huiyi.domain.enums.BookRoomStatus;
import com.ruoyi.huiyi.domain.vo.RoomCurrentStatusVO;
import com.ruoyi.huiyi.domain.vo.RoomFreeTimeVO;
import com.ruoyi.huiyi.domain.vo.TimeRangeVO;
import com.ruoyi.huiyi.mapper.BookRoomMapper;
import com.ruoyi.huiyi.mapper.MeetingRoomMapper;
import com.ruoyi.huiyi.mapper.RoomTimeSlotMapper;
import com.ruoyi.huiyi.service.IBookMeetingRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 会议室预约业务层处理
 *
 * @author ruoyi
 * @date 2026-07-16
 */
@Service
public class BookMeetingRoomServiceImpl implements IBookMeetingRoomService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm[:ss]");
    private static final DateTimeFormatter TIME_OUT_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private BookRoomMapper bookRoomMapper;

    @Autowired
    private MeetingRoomMapper meetingRoomMapper;

    @Autowired
    private RoomTimeSlotMapper roomTimeSlotMapper;

    @Override
    public RoomBooking selectBookingById(Long bookingId) {
        return bookRoomMapper.selectBookRoomBookingById(bookingId);
    }

    @Override
    public List<RoomBooking> selectBookingList(RoomBooking query) {
        return bookRoomMapper.selectBookRoomBookingList(query);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public RoomFreeTimeVO queryFreeTime(Long roomId, Date bookDate){
        MeetingRoom room = meetingRoomMapper.selectMeetingRoomById(roomId);
        if (room == null) {
            throw new ServiceException("会议室不存在");
        }

        RoomFreeTimeVO vo = new RoomFreeTimeVO();
        vo.setRoomId(roomId);
        vo.setRoomName(room.getRoomName());
        vo.setBookDate(bookDate);
        vo.setOpen(true);

        List<TimeRangeVO> occupied = new ArrayList<>();
        for (RoomTimeSlot closed : roomTimeSlotMapper.selectClosedSlotsByRoomsAndDate(roomId, bookDate)) {
            occupied.add(new TimeRangeVO(parseTime(closed.getStartTime()), parseTime(closed.getEndTime())));
        }
        for (RoomBooking b : bookRoomMapper.selectActiveBookingsByRoomAndDate(roomId, bookDate)) {
            occupied.add(new TimeRangeVO(parseTime(b.getStartTime()), parseTime(b.getEndTime())));
        }

        TimeRangeVO wholeDay = new TimeRangeVO(LocalTime.MIN, LocalTime.of(23,59,59));
        vo.setFreeRanges(subtractOccupied(wholeDay, occupied));
        return vo;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public RoomCurrentStatusVO queryCurrentStatus(Long roomId) {
        MeetingRoom room = meetingRoomMapper.selectMeetingRoomById(roomId);
        if (room == null) {
            throw new ServiceException("会议室不存在");
        }
        Date today = DateUtils.getNowDate();
        String now = LocalTime.now().withNano(0).toString();

        RoomCurrentStatusVO vo = new RoomCurrentStatusVO();
        vo.setRoomId(roomId);
        vo.setRoomName(room.getRoomName());

        RoomBooking current = bookRoomMapper.selectCurrentOccupyingBooking(roomId, today, now);
        fillCurrentStatus(vo, current);
        return vo;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<RoomCurrentStatusVO> queryCurrentStatusList(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }
        Date today = DateUtils.getNowDate();
        String now = LocalTime.now().withNano(0).toString();

        List<RoomBooking> occupyingList = bookRoomMapper.selectCurrentOccupyingBookings(roomIds, today, now);

        List<RoomCurrentStatusVO> result = new ArrayList<>();
        for (Long roomId : roomIds) {
            MeetingRoom room = meetingRoomMapper.selectMeetingRoomById(roomId);
            RoomCurrentStatusVO vo = new RoomCurrentStatusVO();
            vo.setRoomId(roomId);
            vo.setRoomName(room == null ? null : room.getRoomName());

            RoomBooking current = occupyingList.stream()
                    .filter(b -> roomId.equals(b.getRoomId()))
                    .findFirst()
                    .orElse(null);
            fillCurrentStatus(vo, current);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long createBooking(BookingCreateDTO dto) {
        TimeRangeVO request = buildAndValidateRange(dto.getStartTime(), dto.getEndTime());

        MeetingRoom room = meetingRoomMapper.selectMeetingRoomByIdForUpdate(dto.getRoomId());
        if (room == null) {
            throw new ServiceException("会议室不存在");
        }
        if (!isRoomUsable(room)) {
            throw new ServiceException("会议室当前不可用（维修中或已停用）");
        }

        ensureNotInMaintained(dto.getRoomId(), dto.getBookDate(), request);

        List<RoomBooking> activeBooking =
                bookRoomMapper.selectActiveBookingsByRoomAndDate(dto.getRoomId(), dto.getBookDate());
        ensureNoOverlap(request, activeBooking);

        LoginUser loginUser = SecurityUtils.getLoginUser();
        RoomBooking booking = new RoomBooking();
        booking.setBookNo(generateBookNo());
        booking.setEmpNo(loginUser.getUsername());
        booking.setUserId(loginUser.getUserId());
        booking.setRoomId(dto.getRoomId());
        booking.setBookDate(dto.getBookDate());
        booking.setStartTime(normalizeTime(dto.getStartTime()));
        booking.setEndTime(normalizeTime(dto.getEndTime()));
        booking.setMeetingId(dto.getMeetingId());
        booking.setBookPurpose(dto.getBookPurpose());
        booking.setBookStatus(BookRoomStatus.CONFIRMED.getCode());
        booking.setCreateBy(loginUser.getUsername());
        booking.setCreateTime(DateUtils.getNowDate());

        int rows = bookRoomMapper.insertBookRoomBooking(booking);
        if (rows <= 0) {
            throw new ServiceException("预约提交失败，请重试");
        }
        return booking.getBookingId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void updateBooking(BookingUpdateDTO dto) {
        TimeRangeVO request = buildAndValidateRange(dto.getStartTime(), dto.getEndTime());

        RoomBooking existing = bookRoomMapper.selectBookRoomBookingByIdForUpdate(dto.getBookingId());
        if (existing == null) {
            throw new ServiceException("预约记录不存在");
        }
        checkOwnership(existing);
        if (!existing.statusEnum().isModifiable()) {
            throw new ServiceException("当前状态【" + existing.statusEnum().getInfo() + "】不允许修改");
        }

        MeetingRoom room = meetingRoomMapper.selectMeetingRoomById(dto.getRoomId());
        if (room == null) {
            throw new ServiceException("会议室不存在");
        }
        if (!isRoomUsable(room)) {
            throw new ServiceException("会议室当前不可用（维护中或已停用）");
        }

        ensureNotInMaintained(dto.getRoomId(), dto.getBookDate(), request);

        List<RoomBooking> activeBookings = bookRoomMapper.selectActiveBookingsByRoomAndDateExcludeId(
                dto.getRoomId(), dto.getBookDate(), dto.getBookingId());
        ensureNoOverlap(request, activeBookings);

        existing.setRoomId(dto.getRoomId());
        existing.setBookDate(dto.getBookDate());
        existing.setStartTime(normalizeTime(dto.getStartTime()));
        existing.setEndTime(normalizeTime(dto.getEndTime()));
        existing.setBookPurpose(dto.getBookPurpose());
        existing.setBookStatus(BookRoomStatus.CONFIRMED.getCode());
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setUpdateTime(DateUtils.getNowDate());

        int rows = bookRoomMapper.updateBookRoomBooking(existing);
        if (rows <= 0) {
            throw new ServiceException("预约已被并发修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void cancelBooking(Long bookingId, String cancelReason) {
        RoomBooking existing = bookRoomMapper.selectBookRoomBookingByIdForUpdate(bookingId);
        if (existing == null) {
            throw new ServiceException("预约记录不存在");
        }
        checkOwnership(existing);

        if (!existing.statusEnum().isCancellable()) {
            throw new ServiceException("当前状态【" + existing.statusEnum().getInfo() + "】不允许取消");
        }
        int rows = bookRoomMapper.cancelBookRoomBooking(bookingId, cancelReason, SecurityUtils.getUsername());
        if (rows <= 0) {
            throw new ServiceException("取消失败，请重试");
        }
    }

    private LocalTime parseTime(String time) {
        if (StringUtils.isEmpty(time)) {
            throw new ServiceException("时间不能为空");
        }
        return LocalTime.parse(time.length() == 5 ? time : time.substring(0, 5), TIME_OUT_FMT);
    }

    private TimeRangeVO buildAndValidateRange(String startTime, String endTime) {
        LocalTime start = parseTime(startTime);
        LocalTime end = parseTime(endTime);
        TimeRangeVO range = new TimeRangeVO(start, end);
        if (!range.isValid()) {
            throw new ServiceException("开始时间必须晚于结束时间");
        }
        return range;
    }

    // 用开放窗口减去已占用区间，得到真正空闲的时间段列表
    private List<TimeRangeVO> subtractOccupied(TimeRangeVO window, List<TimeRangeVO> occupiedRanges) {
        List<TimeRangeVO> result = new ArrayList<>();
        result.add(window);
        for (TimeRangeVO occupied : occupiedRanges) {
            List<TimeRangeVO> next = new ArrayList<>();
            for (TimeRangeVO free : result) {
                if(!free.overLap(occupied)) {
                    next.add(free);
                    continue;
                }

                if (occupied.getStartTime().isAfter(free.getStartTime())) {
                    next.add(new TimeRangeVO(free.getStartTime(), occupied.getStartTime()));
                }

                if (occupied.getEndTime().isAfter(free.getEndTime())) {
                    next.add(new TimeRangeVO(free.getEndTime(), occupied.getEndTime()));
                }
            }
            result = next;
        }
        return result;
    }

    private boolean isRoomUsable(MeetingRoom room) {
        return room.getStatus() == null || "0".equals(room.getStatus());
    }

    /**
     * 校验请求时间没有落在会议室当天的"维护/关闭"黑名单时段内。
     * 默认全天可约，room_time_slot 没有记录时不做任何限制；
     * 只有管理员显式配置了 CLOSED 记录（如装修、内部占用）的时间段才会被拒绝。
     */
    private void ensureNotInMaintained(Long roomId, Date bookDate, TimeRangeVO requested) {
        List<RoomTimeSlot> closedSlot = roomTimeSlotMapper.selectClosedSlotsByRoomsAndDate(roomId, bookDate);
        if (closedSlot == null || closedSlot.isEmpty()) {
            return;
        }
        for (RoomTimeSlot closed : closedSlot) {
            TimeRangeVO closedRange = new TimeRangeVO(parseTime(closed.getStartTime()), parseTime(closed.getEndTime()));
            if (requested.overLap(closedRange)) {
                throw new ServiceException("该时间段会议室维护/关闭（" + closed.getStartTime() + "-" + closed.getEndTime() + "），暂不可预约");
            }
        }
    }

    /** 与现有有效预约逐一做时间重叠判断，一旦重叠立即抛出业务异常，事务自动回滚 */
    private void ensureNoOverlap(TimeRangeVO requested, List<RoomBooking> activeBookings) {
        if (activeBookings == null || activeBookings.isEmpty()) {
            return;
        }

        for (RoomBooking existing : activeBookings) {
            TimeRangeVO existingRange = new TimeRangeVO(parseTime(existing.getStartTime()), parseTime(existing.getEndTime()));
            if (requested.overLap(existingRange)) {
                throw new ServiceException("该时间段与已有预约【" + existing.getBookNo()
                        + " " + existing.getStartTime() + "-" + existing.getEndTime() + "】冲突，请重新选择空闲时间");
            }
        }
    }

    private String generateBookNo() {
        String time = DateUtils.dateTimeNow("yyyyMMddHHmmss");
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        return "BK" + time + rand;
    }

    private String normalizeTime(String time) {
        return parseTime(time).toString();
    }

    /** 校验当前登录人是否有权限操作该预约（本人或管理员）*/
    private void checkOwnership(RoomBooking booking) {
        String currentUser = SecurityUtils.getUsername();
        if (!StringUtils.equals(currentUser, booking.getEmpNo()) && SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            throw new ServiceException("无权操作他人的预约记录");
        }
    }

    private void fillCurrentStatus(RoomCurrentStatusVO vo, RoomBooking current) {
        if (current == null) {
            vo.setOccupied(false);
            return;
        }
        vo.setOccupied(true);
        vo.setCurrentBookNo(current.getBookNo());
        vo.setCurrentEmpNo(current.getEmpNo());
        vo.setCurrentPurpose(current.getBookPurpose());
        vo.setCurrentEndTime(current.getEndTime());
    }
}
