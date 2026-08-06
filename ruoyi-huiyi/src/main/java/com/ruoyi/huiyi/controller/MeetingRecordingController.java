package com.ruoyi.huiyi.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.huiyi.domain.vo.MeetingRecordStatusVO;
import com.ruoyi.huiyi.domain.vo.MeetingRecordVO;
import com.ruoyi.huiyi.service.IMeetingRecordingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 改动说明：原来这个 Controller 不继承 BaseController、方法直接返回VO本身，
 * 跟项目里其他所有接口的 AjaxResult{code,msg,data} 风格不一致。
 * 前端按统一风格读 res.data.xxx，之前这里返回裸VO时读的是不存在的那一层，
 * 拿到的 wsPath 永远是 undefined。现在统一套上 AjaxResult。
 */
@RestController
@RequestMapping("/huiyi/record")
public class MeetingRecordingController extends BaseController {

    @Autowired
    private IMeetingRecordingService meetingRecordingService;

    /** 开始录制：返回 WebSocket 路径，前端拿到后立即建连推送麦克风音频流 */
    @PostMapping("/{meetingId}/start")
    public AjaxResult start(@PathVariable Long meetingId) {
        String operator = currentUser();
        MeetingRecordVO vo = meetingRecordingService.startRecord(meetingId, operator);
        return success(vo); // 前端读 res.data.wsPath
    }

    /** 暂停录制 */
    @PostMapping("/{meetingId}/pause")
    public AjaxResult pause(@PathVariable Long meetingId) {
        meetingRecordingService.pauseRecord(meetingId, currentUser());
        return success();
    }

    /** 恢复录制 */
    @PostMapping("/{meetingId}/resume")
    public AjaxResult resume(@PathVariable Long meetingId) {
        meetingRecordingService.resumeRecord(meetingId, currentUser());
        return success();
    }

    /** 结束录制：立即返回，转写与纪要生成在后台异步进行 */
    @PostMapping("/{meetingId}/stop")
    public AjaxResult stop(@PathVariable Long meetingId) {
        meetingRecordingService.stopRecord(meetingId, currentUser());
        return success();
    }

    /** 查询录制/纪要生成状态，前端结束录制后轮询本接口即可 */
    @GetMapping("/{meetingId}/status")
    public AjaxResult status(@PathVariable Long meetingId) {
        MeetingRecordStatusVO vo = meetingRecordingService.getRecordStatus(meetingId);
        return success(vo); // 前端读 res.data.xxx
    }

    private String currentUser() {
        return SecurityUtils.getUsername();
    }
}