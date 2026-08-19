package com.ruoyi.huiyi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.huiyi.config.AsrProperties;
import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.domain.vo.AsrResultVO;
import com.ruoyi.huiyi.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.huiyi.service.IMeetingAsrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class MeetingAsrServiceImpl implements IMeetingAsrService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAsrServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AsrProperties asrProperties;

    @Autowired
    private MeetingRecordProperties recordProperties;

    @Override
    public AsrResultVO asrTranslateService(String audioPath) {
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
            String rawText = root.path("raw_text").asText("");
            List<List<Long>> timestamp = MAPPER.convertValue(root.path("timestamp"), new TypeReference<List<List<Long>>>() {});
            if (text == null) {
                throw new RuntimeException(
                        "ASR服务返回格式错误: " + responseBody
                );
            }

            AsrResultVO result = new AsrResultVO();
            result.setText(text.trim());
            result.setRawText(rawText);
            result.setTimestamp(timestamp);

            log.info("========== ASR最终转写结果 ==========");
            log.info("text={}", result.getText());
            log.info("rawText长度={}", result.getRawText() == null ? 0 : result.getRawText().length());
            log.info("timestamp数量={}", result.getTimestamp() == null ? 0 : result.getTimestamp().size());
            log.info("=====================================");
            return result;
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

    private static final int MIN_SEGMENT_LENGTH = 60;
    private static final int NORMAL_SEGMENT_LENGTH = 80;

    private static final long PAUSE_THRESHOLD = 1800;
    private static final long MAX_SEGMENT_DURATION = 60_000;

    public String buildSegmentedTranscript(AsrResultVO asrResult) {
        String text = asrResult.getText();
        String rawText = asrResult.getRawText();
        List<List<Long>> timestamps = asrResult.getTimestamp();

        if (text == null || text.trim().isEmpty()) {
            return "[]";
        }
        if (timestamps == null || timestamps.isEmpty()) {
            log.warn("ASR没有返回timestamp，直接保存完整文本");
            ObjectNode segment = MAPPER.createObjectNode();

            segment.put("start", 0);
            segment.put("end", 0);
            segment.put("text", text.trim());

            ArrayNode array = MAPPER.createArrayNode();
            array.add(segment);
            return array.toString();
        }
        List<String> tokens = splitRawText(rawText);

        List<TextTimeUnit> units =
                alignTextWithTimestamp(
                        text,
                        tokens,
                        timestamps
                );

        if (units.isEmpty()) {

            ObjectNode segment = MAPPER.createObjectNode();

            segment.put("start", 0);
            segment.put(
                    "end",
                    timestamps.get(timestamps.size() - 1).get(1)
            );
            segment.put("text", text.trim());

            ArrayNode array = MAPPER.createArrayNode();
            array.add(segment);

            return array.toString();
        }
        List<TranscriptSegment> segments =
                splitIntoSegments(units);
        try {

            return MAPPER.writeValueAsString(segments);

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "生成转写分段JSON失败",
                    e
            );
        }
    }
    private List<String> splitRawText(String rawText) {
        List<String> tokens = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            return tokens;
        }
        String normalized = rawText.trim();
        if (normalized.contains(" ")) {
            String[] parts = normalized.split("\\s+");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    tokens.add(part);
                }
            }
        } else {
            for (int i = 0; i < normalized.length(); i++) {
                tokens.add(String.valueOf(normalized.charAt(i)));
            }
        }
        return tokens;
    }
    private static class TextTimeUnit {
        private String text;
        private long start;
        private long end;
        public TextTimeUnit(
                String text,
                long start,
                long end
        ) {
            this.text = text;
            this.start = start;
            this.end = end;
        }
        public String getText() {
            return text;
        }
        public long getStart() {
            return start;
        }
        public long getEnd() {
            return end;
        }
        public void setText(String text) {
            this.text = text;
        }
    }
    private List<TextTimeUnit> alignTextWithTimestamp(String text, List<String> rawTokens, List<List<Long>> timestamps) {
        List<TextTimeUnit> units = new ArrayList<>();
        if (text == null || rawTokens == null || timestamps == null) {
            return units;
        }
        int tokenIndex = 0;
        for (int i = 0; i < rawTokens.size(); i++) {
            if (tokenIndex >= timestamps.size()) {
                break;
            }
            String token = rawTokens.get(i);
            List<Long> ts = timestamps.get(tokenIndex);
            if (ts == null || ts.size() < 2) {
                continue;
            }

            long start = ts.get(0);
            long end = ts.get(1);

            units.add(new TextTimeUnit(token, start, end));
            tokenIndex++;
        }
        addPunctuationToUnits(text, units);
        return units;
    }
    private void addPunctuationToUnits(String text, List<TextTimeUnit> units) {
        if (text == null || text.isEmpty() || units == null || units.isEmpty()) {
            return;
        }
        StringBuilder result = new StringBuilder();
        int textIndex = 0;
        for (TextTimeUnit unit : units) {
            String token = unit.getText();
            int index = text.indexOf(token, textIndex);
            if (index < 0) {
                continue;
            }
            int tokenEnd = index + token.length();
            int punctuationEnd = tokenEnd;
            while (punctuationEnd < text.length()) {
                char c = text.charAt(punctuationEnd);
                if (isPunctuation(c)) {
                    punctuationEnd++;
                } else {
                    break;
                }
            }
            if (punctuationEnd > tokenEnd) {
                String punctuation = text.substring(tokenEnd, punctuationEnd);
                unit.setText(unit.getText() + punctuation);
                textIndex = punctuationEnd;
            } else {
                textIndex = tokenEnd;
            }
        }
    }
    private List<TranscriptSegment> splitIntoSegments(List<TextTimeUnit> units) {
        List<TranscriptSegment> segments = new ArrayList<>();

        if (units == null || units.isEmpty()) {
            return segments;
        }

        StringBuilder currentText = new StringBuilder();

        long segmentStart = -1;
        long segmentEnd = -1;

        for (int i = 0; i < units.size(); i++) {
            TextTimeUnit unit = units.get(i);
            if (segmentStart < 0) {
                segmentStart = unit.getStart();
            }
            currentText.append(unit.getText());
            segmentEnd = unit.getEnd();
            String current = currentText.toString().trim();
            boolean strongPunctuation = isStrongPunctuation(unit.getText());
            boolean nextExists = i + 1 < units.size();
            long pause = 0;
            if (nextExists) {
                TextTimeUnit next = units.get(i + 1);
                pause = next.getStart() - unit.getEnd();
            }
            boolean longPause = pause >= PAUSE_THRESHOLD;
            boolean durationExceeded = segmentEnd - segmentStart >= MAX_SEGMENT_DURATION;
            int textLength = current.length();
            boolean tooShort = textLength < MIN_SEGMENT_LENGTH;
            boolean normalLength = textLength >= NORMAL_SEGMENT_LENGTH;
            boolean fillerOnly = isFillerText(current);
            boolean shouldBreak = false;
            if (durationExceeded) {

                shouldBreak = true;

            } else {
                if (strongPunctuation) {
                    if (!tooShort && !fillerOnly) {
                        if (normalLength) {
                            shouldBreak = true;
                        }
                        else if (longPause) {
                            shouldBreak = true;
                        }
                    }
                }
                if (longPause
                        && textLength >= MIN_SEGMENT_LENGTH
                        && !fillerOnly) {

                    shouldBreak = true;
                }
            }
            if (shouldBreak) {

                TranscriptSegment segment =
                        new TranscriptSegment();

                segment.setStart(segmentStart);
                segment.setEnd(segmentEnd);
                segment.setText(current);

                segments.add(segment);
                currentText.setLength(0);
                segmentStart = -1;
                segmentEnd = -1;
            }
        }
        if (currentText.length() > 0) {

            TranscriptSegment segment =
                    new TranscriptSegment();

            segment.setStart(segmentStart);
            segment.setEnd(segmentEnd);
            segment.setText(currentText.toString().trim());

            segments.add(segment);
        }

        return segments;
    }
    private boolean isPunctuation(char c) {
        return "。！？；，、：！？；，、：!?;,:".indexOf(c) >= 0;
    }
    private boolean isStrongPunctuation(String text) {

        if (text == null || text.isEmpty()) {
            return false;
        }

        char last =
                text.charAt(text.length() - 1);

        return last == '。'
                || last == '！'
                || last == '？'
                || last == '；'
                || last == '!'
                || last == '?'
                || last == ';';
    }
    private static class TranscriptSegment {

        private long start;

        private long end;

        private String text;

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
    private boolean isFillerText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String normalized = text.trim().replaceAll("[。！？；，、：!?;,:]", "");
        if (normalized.isEmpty()) {
            return false;
        }
        Set<String> fillerWords = Set.of(
                "嗯",
                "啊",
                "呃",
                "哦",
                "噢",
                "对",
                "好",
                "是",
                "嗯嗯",
                "啊啊",
                "呃呃",
                "对对",
                "好好"
        );
        return fillerWords.contains(normalized);
    }
}