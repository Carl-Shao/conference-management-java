package com.ruoyi.huiyi.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.huiyi.domain.MeetingRecord;
import com.ruoyi.huiyi.mq.dto.AudioUploadTask;
import com.ruoyi.huiyi.mq.producer.AudioTaskProducer;
import com.ruoyi.huiyi.service.IMeetingRecordService;
import com.ruoyi.huiyi.service.IUploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/huiyi/audio")
public class MeetingAudioController extends BaseController{

    @Autowired
    private IUploadFileService uploadFileService;

    @Autowired
    private IMeetingRecordService meetingRecordService;

    @Autowired
    private AudioTaskProducer audioTaskProducer;

    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("files") MultipartFile[] files) {

        List<AudioUploadTask> tasks = new ArrayList<>();
        List<Long> meetingIds = new ArrayList<>();

        for (MultipartFile file : files) {
            // 1. 落盘保存（原有逻辑不变）
            String path = uploadFileService.saveOneFile(file);

            // 2. 先建一条会议主表记录，转写/纪要/笔记此时都还没有，属于正常的"待转写"状态
            MeetingRecord meeting = new MeetingRecord();
            meeting.setTitle(resolveTitle(file.getOriginalFilename()));
            meeting.setSourceType("1"); // 上传音频
            meeting.setAudioPath(path);
            meeting.setDuration(0); // 时长未知，等 ASR/转码环节拿到真实时长后再回写
            meeting.setStatus("1"); // 转写中
            meeting.setIsFavorite("0");
            meetingRecordService.insertMeetingRecord(meeting);

            meetingIds.add(meeting.getMeetingId());
            tasks.add(new AudioUploadTask(meeting.getMeetingId(), path));
        }

        // 3. 把 meetingId 和文件路径一起交给 ASR 队列，
        //    这样 ASR/LLM 消费端处理完之后才知道结果要写回哪一条记录
        audioTaskProducer.sendBatch(tasks);

        return AjaxResult.success(meetingIds);
    }

    private String resolveTitle(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "未命名会议";
        }
        int index = originalFilename.lastIndexOf(".");
        return index > 0 ? originalFilename.substring(0, index) : originalFilename;
    }


}

