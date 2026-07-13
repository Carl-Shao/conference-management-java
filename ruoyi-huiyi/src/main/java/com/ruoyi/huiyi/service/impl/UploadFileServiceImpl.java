package com.ruoyi.huiyi.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.huiyi.service.IUploadFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
public class UploadFileServiceImpl implements IUploadFileService {

    @Value("${huiyi.storage.audio-path}")
    private String audioPath;

    @Value("${huiyi.storage.allowed-type}")
    private List<String> allowedType;

    @Override
    public List<String> save(MultipartFile[] files){

        List<String> paths = new ArrayList<>();

        for (MultipartFile file : files) {
            String path = saveOneFile(file);

            paths.add(path);
        }
        return paths;
    }

    @Override
    public String saveOneFile(MultipartFile file) {
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
            throw new RuntimeException("请上传war格式音频");
        }

        File dir = new File(audioPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMDD_HHmmss"));

        String path = time + UUID.randomUUID() + "." + suffix;

        File targetFile = new File(dir, path);

        try{
            file.transferTo(targetFile);
        }catch (IOException e){
            throw new RuntimeException("文件保存失败");
        }
        return path;
    }
}
