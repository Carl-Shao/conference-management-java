package com.ruoyi.huiyi.service.impl;

import com.ruoyi.huiyi.domain.RoomBooking;
import com.ruoyi.huiyi.mapper.BookRoomMapper;
import com.ruoyi.huiyi.service.IBookMeetingRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 会议室预约业务层处理
 *
 * @author ruoyi
 * @date 2026-07-16
 */
@Service
public class BookMeetingRoomImpl implements IBookMeetingRoom {

    @Autowired
    private BookRoomMapper bookRoomMapper;


}
