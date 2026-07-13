package com.ruoyi.huiyi.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface IUploadFileService {

    List<String> save (MultipartFile[] file);

    String saveOneFile(MultipartFile file);
}
