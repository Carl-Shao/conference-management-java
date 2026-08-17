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
            Map<String, String> formParams = new HashMap();
            formParams.put("model", asrProperties.getModel());
            formParams.put("language", "zh");
            formParams.put("response_format", "json");
            log.info("开始调用 ASR, filePath={}, apiUrl={}, model={}",
                    audioPath,
                    asrProperties.getApiUrl(),
                    asrProperties.getModel());

            String responseBody = HttpClientUtil.doPostFile(
                    asrProperties.getApiUrl(),
                    "file",
                    audioPath,
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
            log.error("调用ASR服务失败, filePath={}", audioPath, e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
}
