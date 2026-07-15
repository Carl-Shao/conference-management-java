package com.ruoyi.huiyi.service;

public interface IMeetingAsrService {

    /**
     * 对音频文件做语音识别
     * @param audioPath 音频文件（本地磁盘路径）
     * @return 识别出的文本
     */
    String asrTranslateService(String audioPath);
}
