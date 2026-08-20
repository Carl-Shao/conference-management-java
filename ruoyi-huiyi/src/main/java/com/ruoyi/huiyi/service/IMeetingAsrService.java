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

    /**
     * 转写一个音频文件，自动判断时长：超过阈值（10分钟）就先切片再逐段转写，
     * 避免把几个小时的长音频整个塞给ASR服务导致显存占用过高；没超过阈值就直接单次调用，
     * 效果等同于直接调 asrTranslateService。上传音频这条链路应该用这个，不要用上面那个。
     * 切片产生的临时文件用完会自动清理，调用方不用操心。
     */
    AsrResultVO transcribeAudio(String audioPath);
}
