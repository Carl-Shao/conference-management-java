package com.ruoyi.huiyi.service;

public interface IMeetingLlmService {

    /**
     * 根据prompt让模型生成内容
     * @param prompt 提示词（比如把ASR识别出的会议文本拼接进去）
     * @return 生成的文本
     */
    String generateMinutes(String prompt);
}
