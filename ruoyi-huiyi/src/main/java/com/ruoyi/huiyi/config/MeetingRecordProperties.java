package com.ruoyi.huiyi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "huiyi.record")
public class MeetingRecordProperties {

    /** 每隔多少秒把已缓冲的音频切一片，投给ASR做实时转写 */
    private int chunkIntervalSeconds = 8;

    /** 前端 AudioWorklet 采集参数，必须与前端一致 */
    private int sampleRate = 16000;
    private int channels = 1;
    private int bitDepth = 16;

    /** 录音文件根目录 */
    private String audioBasePath = "/data/huiyi/audio";

    /**
     * ASR 调用并发线程池大小：不同会议室并发录制时，所有分片共享这一个线程池，
     * 从而把“同时调用ASR接口”的并发数控制在ASR服务能承受的范围内，
     * 不需要引入消息队列。
     */
    private int asrConcurrency = 3;

    /** 大模型(生成纪要)调用的并发线程池大小，纪要生成属于低频、重量级调用，单独给一个小池子 */
    private int llmConcurrency = 2;

    /** 结束录制后，等待所有分片转写完成的最长等待时间(秒)，超时也会强制进入纪要生成 */
    private int finalizeWaitTimeoutSeconds = 60;

    public int getChunkIntervalSeconds() {
        return chunkIntervalSeconds;
    }

    public void setChunkIntervalSeconds(int chunkIntervalSeconds) {
        this.chunkIntervalSeconds = chunkIntervalSeconds;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public void setBitDepth(int bitDepth) {
        this.bitDepth = bitDepth;
    }

    public String getAudioBasePath() {
        return audioBasePath;
    }

    public void setAudioBasePath(String audioBasePath) {
        this.audioBasePath = audioBasePath;
    }

    public int getAsrConcurrency() {
        return asrConcurrency;
    }

    public void setAsrConcurrency(int asrConcurrency) {
        this.asrConcurrency = asrConcurrency;
    }

    public int getLlmConcurrency() {
        return llmConcurrency;
    }

    public void setLlmConcurrency(int llmConcurrency) {
        this.llmConcurrency = llmConcurrency;
    }

    public int getFinalizeWaitTimeoutSeconds() {
        return finalizeWaitTimeoutSeconds;
    }

    public void setFinalizeWaitTimeoutSeconds(int finalizeWaitTimeoutSeconds) {
        this.finalizeWaitTimeoutSeconds = finalizeWaitTimeoutSeconds;
    }
}
