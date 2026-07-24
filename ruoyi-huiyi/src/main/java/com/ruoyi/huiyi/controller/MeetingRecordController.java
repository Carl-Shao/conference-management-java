package com.ruoyi.huiyi.controller;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.domain.vo.MeetingRecordStatusVO;
import com.ruoyi.huiyi.domain.vo.MeetingRecordVO;
import com.ruoyi.huiyi.service.IMeetingRecordingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/huiyi/record")
public class MeetingRecordController {

    @Autowired
    private IMeetingRecordingService meetingRecordingService;

    /** 开始录制：返回 WebSocket 路径，前端拿到后立即建连推送麦克风音频流 */
    @PostMapping("/{meetingId}/start")
    public MeetingRecordVO start(@PathVariable Long meetingId) {
        String operator = currentUser();
        return meetingRecordingService.startRecord(meetingId, operator);
    }

    /** 暂停录制 */
    @PostMapping("/{meetingId}/pause")
    public void pause(@PathVariable Long meetingId) {
        meetingRecordingService.pauseRecord(meetingId, currentUser());
    }

    /** 恢复录制 */
    @PostMapping("/{meetingId}/resume")
    public void resume(@PathVariable Long meetingId) {
        meetingRecordingService.resumeRecord(meetingId, currentUser());
    }

    /** 结束录制：立即返回，转写与纪要生成在后台异步进行 */
    @PostMapping("/{meetingId}/stop")
    public void stop(@PathVariable Long meetingId) {
        meetingRecordingService.stopRecord(meetingId, currentUser());
    }

    /** 查询录制/纪要生成状态，前端结束录制后轮询本接口即可 */
    @GetMapping("/{meetingId}/status")
    public MeetingRecordStatusVO status(@PathVariable Long meetingId) {
        return meetingRecordingService.getRecordStatus(meetingId);
    }

    private String currentUser() {
        return SecurityUtils.getUsername();
    }
}
