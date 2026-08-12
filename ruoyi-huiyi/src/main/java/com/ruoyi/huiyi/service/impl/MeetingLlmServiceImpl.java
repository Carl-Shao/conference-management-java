package com.ruoyi.huiyi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.huiyi.config.LlmProperties;
import com.ruoyi.huiyi.domain.dto.MeetingMinutesResultDTO;
import com.ruoyi.huiyi.service.IMeetingLlmService;
import com.ruoyi.huiyi.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MeetingLlmServiceImpl implements IMeetingLlmService {
    private static final Logger log = LoggerFactory.getLogger(MeetingLlmServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private LlmProperties llmProperties;

    @Override
    public MeetingMinutesResultDTO generateMinutes(String prompt) {
        try{
            String requestBody = buildRequestBody(prompt);
            String responseBody = HttpClientUtil.doPostJson(
                    llmProperties.getUrl(),
                    requestBody,
                    null,
                    llmProperties.getConnectTimeout(),
                    llmProperties.getSocketTimeout()
            );
            String rawText = extractRawText(responseBody);
            return parseStructuredResult(rawText);
        }catch (IOException e) {
            log.error("调用Ollama服务失败, prompt长度={}", prompt == null ? 0 : prompt.length(), e);
            throw new RuntimeException("会议纪要生成失败: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String prompt) throws IOException{
        ObjectNode node = MAPPER.createObjectNode();
        node.put("model", llmProperties.getModel());
        node.put("prompt", prompt);
        node.put("stream", false);
        return MAPPER.writeValueAsString(node);
    }

    /** 从Ollama的响应信封里取出 response 字段 */
    private String extractRawText(String responseBody) throws IOException {
        JsonNode root = MAPPER.readTree(responseBody);
        return root.path("response").asText("");
    }

    /**
     * 把 response 字段的文本内容当JSON解析，拆出 title/summary/content。
     */
    private MeetingMinutesResultDTO parseStructuredResult(String rawText) {
        String cleaned = stripCodeFence(rawText);
        try {
            JsonNode node = MAPPER.readTree(cleaned);
            String title = node.path("title").asText(null);
            String summary = node.path("summary").asText(null);
            String content = node.path("content").asText(null);
            if(title != null && !title.trim().isEmpty()) {
                String time = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("MM-dd"));
                title = time + " " + title;
            }
            if(content == null || content.isEmpty()) {
                // JSON解析成功了，但content字段是空的，视为格式不对，走兜底
                log.warn("LLM返回的JSON里content字段为空，原文: {}", rawText);
                return new MeetingMinutesResultDTO(null, null, rawText);
            }
            return new MeetingMinutesResultDTO(title, summary, content);
        } catch (Exception e) {
            log.warn("解析LLM返回的JSON失败，原文当作content兜底使用，原文: {}", rawText, e);
            return new MeetingMinutesResultDTO(null, null, rawText);
        }
    }

    private String stripCodeFence(String text) {
        if(text == null) {
            return null;
        }
        String trimmed = text.trim();
        if(trimmed.startsWith("'''")) {
            int firstNewLine = trimmed.indexOf('\n');
            if(firstNewLine != -1) {
                trimmed = trimmed.substring(firstNewLine + 1);
            }
            int lastFence = trimmed.lastIndexOf("'''");
            if(lastFence != -1) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }

}
