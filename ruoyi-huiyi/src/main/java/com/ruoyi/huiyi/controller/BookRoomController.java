package com.ruoyi.huiyi.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.huiyi.domain.RoomBooking;
import com.ruoyi.huiyi.domain.dto.BookingCreateDTO;
import com.ruoyi.huiyi.domain.dto.BookingUpdateDTO;
import com.ruoyi.huiyi.domain.vo.RoomCurrentStatusVO;
import com.ruoyi.huiyi.domain.vo.RoomFreeTimeVO;
import com.ruoyi.huiyi.service.IBookMeetingRoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 会议室预约 Controller
 *
 * @author ruoyi
 * @date 2026-07-16
 */
@RestController
@RequestMapping("/huiyi/bookRoom")
public class BookRoomController extends BaseController {

    @Autowired
    private IBookMeetingRoomService bookMeetingRoomService;

    /**
     * 查询预约列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('meeting:bookRoom:list')")
    @GetMapping("/list")
    public TableDataInfo list(RoomBooking query) {
        startPage();
        List<RoomBooking> list = bookMeetingRoomService.selectBookingList(query);
        return getDataTable(list);
    }

    /**
     * 查询自己预约列表
     */
    @GetMapping("/myList")
    public TableDataInfo myList(RoomBooking query) {
        startPage();
        query.setEmpNo(getUsername());
        List<RoomBooking> list = bookMeetingRoomService.selectBookingList(query);
        return getDataTable(list);
    }

    /**
     * 获取预约详情
     */
    @PreAuthorize("@ss.hasPermi('meeting:bookRoom:query')")
    @GetMapping("/{bookingId}")
    public AjaxResult getInfo(@PathVariable("bookingId") Long bookingId) {
        return success(bookMeetingRoomService.selectBookingById(bookingId));
    }

    /**
     * 查询会议室在指定日期的空闲时间
     */
    @GetMapping("/freeTime")
    public AjaxResult freeTime(@RequestParam("roomId") Long roomId,
                               @RequestParam("bookDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date bookDate) {
        RoomFreeTimeVO vo = bookMeetingRoomService.queryFreeTime(roomId, bookDate);
        return success(vo);
    }

    /**
     * 查询单个会议室"此刻"是否被占用（会议室详情页/预约页展示红绿灯用）
     */
    @GetMapping("/currentStatus/{roomId}")
    public AjaxResult currentStatus(@PathVariable("roomId") Long roomId) {
        return success(bookMeetingRoomService.queryCurrentStatus(roomId));
    }

    /**
     * 批量查询多个会议室"此刻"是否被占用（会议室列表页用，一次查询多个会议室的占用状态）
     * 示例：/meeting/bookRoom/currentStatus/list?roomIds=1,2,3
     */
    @GetMapping("/currentStatus/list")
    public AjaxResult currentStatusList(@RequestParam("roomIds") Long[] roomIds) {
        List<RoomCurrentStatusVO> list = bookMeetingRoomService.queryCurrentStatusList(Arrays.asList(roomIds));
        return success(list);
    }

    /**
     * 提交会议室预约
     */
    @PreAuthorize("@ss.hasPermi('meeting:bookRoom:add')")
    @Log(title = "会议室预约", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody BookingCreateDTO dto) {
        Long bookingId = bookMeetingRoomService.createBooking(dto);
        return success();
    }

    /**
     * 修改会议室预约（改期/改时间段/改事由）
     */
    @PreAuthorize("@ss.hasPermi('meeitng:bookRoom:edit')")
    @Log(title = "会议室预约", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody BookingUpdateDTO dto) {
        bookMeetingRoomService.updateBooking(dto);
        return success();
    }

    /**
     * 取消会议室预约
     */
    @PreAuthorize("@ss.hasPermi('meeting:bookRoom:cancel')")
    @Log(title = "会议室预约", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{bookingId}")
    public AjaxResult cancel(@PathVariable("bookingId") Long bookingId,
                             @RequestParam(value = "cancelReason", required = false) String cancelReason) {
        bookMeetingRoomService.cancelBooking(bookingId, cancelReason);
        return success();
    }
}
