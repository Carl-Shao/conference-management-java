package com.ruoyi.huiyi.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.domain.dto.UploadedAudioResult;
import com.ruoyi.huiyi.service.IUploadFileService;
import com.ruoyi.huiyi.util.WavUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
public class UploadFileServiceImpl implements IUploadFileService {

    @Autowired
    private MeetingRecordProperties recordProperties;

    @Value("${huiyi.upload.allowed-type}")
    private List<String> allowedType;

    private static final String UPLOAD_SUB_DIR = "uploads";

    @Override
    public List<UploadedAudioResult> save(MultipartFile[] files){

        List<UploadedAudioResult> results = new ArrayList<>();

        for (MultipartFile file : files) {
            results.add(saveOneFile(file));
        }
        return results;
    }

    @Override
    public UploadedAudioResult saveOneFile(MultipartFile file) {
        if(file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new RuntimeException("文件名为空");
        }

        int index = filename.lastIndexOf(".");
        String suffix = filename.substring(index + 1);

        if (!allowedType.contains(suffix)) {
            throw new RuntimeException("请上传wav格式音频");
        }

        File dir = new File(recordProperties.getAudioBasePath(), UPLOAD_SUB_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMDD_HHmmss"));

        String filenameOnDisk = time + "_" + UUID.randomUUID() + "." + suffix;

        File targetFile = new File(dir, filenameOnDisk);

        try{
            file.transferTo(targetFile);
        }catch (IOException e){
            throw new RuntimeException("文件保存失败", e);
        }

        long durationMs = WavUtils.readDurationMs(targetFile);
        Long durationSeconds =  durationMs / 1000;
        // 返回相对路径（子目录/文件名），而不是只有文件名，
        // 这样和 MeetingSessionManager 里存的完整录音路径风格一致，方便以后统一按 audioBasePath 拼接读取
        String relativePath = UPLOAD_SUB_DIR + "/" + filenameOnDisk;
        return new UploadedAudioResult(relativePath, durationSeconds);
    }
}
