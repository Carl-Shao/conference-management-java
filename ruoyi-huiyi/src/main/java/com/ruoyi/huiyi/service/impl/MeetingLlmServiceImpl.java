package com.ruoyi.huiyi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.huiyi.config.LlmProperties;
import com.ruoyi.huiyi.service.IMeetingLlmService;
import com.ruoyi.huiyi.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MeetingLlmServiceImpl implements IMeetingLlmService {
    private static final Logger log = LoggerFactory.getLogger(MeetingLlmServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private LlmProperties llmProperties;

    @Override
    public String generateMinutes(String prompt) {
        try{
            String requestBody = buildRequestBody(prompt);
            String responseBody = HttpClientUtil.doPostJson(
                    llmProperties.getUrl(),
                    requestBody,
                    null,
                    llmProperties.getConnectTimeout(),
                    llmProperties.getSocketTimeout()
            );
            return parseResponse(responseBody);
        }catch (IOException e) {
            log.error("调用Ollama服务失败, prompt长度={}", prompt == null ? 0 : prompt.length(), e);
            throw new RuntimeException("会议纪要生成失败: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String prompt) throws IOException{
        ObjectNode node = MAPPER.createObjectNode();
        node.put("modle", llmProperties.getModel());
        node.put("prompt", prompt);
        node.put("stream", false);
        return MAPPER.writeValueAsString(node);
    }

    private String parseResponse(String responseBody) throws IOException {
        JsonNode root = MAPPER.readTree(responseBody);
        return root.path("response").asText("");
    }
}
