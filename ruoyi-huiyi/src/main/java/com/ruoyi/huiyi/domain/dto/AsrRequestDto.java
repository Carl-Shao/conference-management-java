package com.ruoyi.huiyi.domain.dto;

/**
 * ASR请求数据传输对象
 */
public class AsrRequestDto {

    /**
     * audio directory path
     */
    private String audioPath;

    private String audioFormat;

    /**
     * get audio path
     */
    public String getAudioPath() {return audioPath;}

    public String getAudioFormat() {return audioFormat;}
}