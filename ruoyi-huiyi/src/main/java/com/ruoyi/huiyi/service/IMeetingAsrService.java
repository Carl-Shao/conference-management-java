package com.ruoyi.huiyi.service;

import com.ruoyi.huiyi.domain.vo.AsrResultVO;

public interface IMeetingAsrService {

    /**
     * 对音频文件做语音识别
     * @param audioPath 音频文件（本地磁盘路径）
     * @return 识别出的文本
     */
    AsrResultVO asrTranslateService(String audioPath);

    String buildSegmentedTranscript(AsrResultVO asrResult);
}
