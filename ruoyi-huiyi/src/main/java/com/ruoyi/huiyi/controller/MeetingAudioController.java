package com.ruoyi.huiyi.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.huiyi.mq.producer.AudioTaskProducer;
import com.ruoyi.huiyi.service.IUploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/huiyi/audio")
public class MeetingAudioController extends BaseController{

    @Autowired
    private IUploadFileService IUploadFileService;

    @Autowired
    private AudioTaskProducer audioTaskProducer;

    @PostMapping("/upload")
    public AjaxResult upload(MultipartFile[] files) {

        List<String> filePaths = IUploadFileService.save(files);

        List<String> taskIds = audioTaskProducer.sendBatch(filePaths);

        return AjaxResult.success();
    }


}

