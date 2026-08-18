package com.ruoyi.huiyi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.huiyi.config.AsrProperties;
import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.huiyi.service.IMeetingAsrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class MeetingAsrServiceImpl implements IMeetingAsrService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAsrServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AsrProperties asrProperties;

    @Autowired
    private MeetingRecordProperties recordProperties;

    @Override
    public String asrTranslateService(String audioPath) {
        // audioPath 有两种来源：上传音频存的是相对audioBasePath的相对路径，
        // 实时录制分片传的是chunkFile.getAbsolutePath()绝对路径，两种都要能处理，
        // 不能直接把audioPath原样传给HttpClientUtil——相对路径会按JVM当前工作目录去找，
        // 大概率找不到文件（跟之前streamAudio播放接口踩的是同一个坑，同样的解析逻辑）
        File resolvedFile = resolveAudioFile(audioPath);
        if (!resolvedFile.exists() || !resolvedFile.isFile()) {
            throw new RuntimeException("音频文件不存在: " + resolvedFile.getAbsolutePath());
        }
        String resolvedPath = resolvedFile.getAbsolutePath();

        try{
            Map<String, String> formParams = new HashMap<>();
            formParams.put("model", asrProperties.getModel());
            formParams.put("language", "zh");
            formParams.put("response_format", "json");
            formParams.put("punctuation", "true");
            log.info("开始调用 ASR, filePath={}, apiUrl={}, model={}",
                    resolvedPath,
                    asrProperties.getApiUrl(),
                    asrProperties.getModel());

            String responseBody = HttpClientUtil.doPostFile(
                    asrProperties.getApiUrl(),
                    "file",
                    resolvedPath,
                    formParams,
                    null,
                    asrProperties.getConnectTimeout(),
                    asrProperties.getSocketTimeout()
            );

            log.info("ASR返回: {}", responseBody);
            JsonNode root = MAPPER.readTree(responseBody);
            String text = root.path("text").asText(null);
            if (text == null) {
                throw new RuntimeException(
                        "ASR服务返回格式错误: " + responseBody
                );
            }

            return text.trim();
        }catch (IOException e){
            log.error("调用ASR服务失败, filePath={}", resolvedPath, e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }

    /** 把 audioPath 解析成真实文件：绝对路径直接用，相对路径拼上配置的音频根目录 */
    private File resolveAudioFile(String audioPath) {
        File direct = new File(audioPath);
        if (direct.isAbsolute()) {
            return direct;
        }
        return new File(recordProperties.getAudioBasePath(), audioPath);
    }
}