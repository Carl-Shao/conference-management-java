package com.ruoyi.huiyi.service;

import java.util.List;

import com.ruoyi.huiyi.domain.dto.UploadedAudioResult;
import org.springframework.web.multipart.MultipartFile;

public interface IUploadFileService {

    List<UploadedAudioResult> save (MultipartFile[] file);

    UploadedAudioResult saveOneFile(MultipartFile file);
}
