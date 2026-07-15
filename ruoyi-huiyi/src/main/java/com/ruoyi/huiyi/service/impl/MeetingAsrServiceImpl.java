package com.ruoyi.huiyi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.huiyi.config.AsrProperties;
import com.ruoyi.huiyi.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.huiyi.service.IMeetingAsrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class MeetingAsrServiceImpl implements IMeetingAsrService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAsrServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AsrProperties asrProperties;

    @Override
    public String asrTranslateService(String audioPath) {
        // request
        try{
            String responseBody = HttpClientUtil.doPostFile(
                    asrProperties.getUrl(),
                    "audio",
                    audioPath,
                    null,
                    null,
                    asrProperties.getConnectTimeout(),
                    asrProperties.getSocketTimeout()
            );

            JsonNode root = MAPPER.readTree(responseBody);
            if (root.path("code").asInt(-1) != 0) {
                throw new RuntimeException("ASR服务返回错误: " + responseBody);
            }
            return root.path("text").asText("");
        }catch (IOException e){
            log.error("调用ASR服务失败, filePath={}", audioPath, e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
}
