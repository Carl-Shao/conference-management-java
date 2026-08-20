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
import com.ruoyi.huiyi.util.WavUtils;
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
    /** 单次ASR调用允许的最大音频时长，超过这个阈值会先切片再逐段转写，避免长音频占用过多显存 */
    private static final long MAX_SINGLE_CALL_DURATION_MS = 4 * 60 * 1000L;

    private static final int MIN_SEGMENT_LENGTH = 60;
    private static final int NORMAL_SEGMENT_LENGTH = 80;

    private static final long PAUSE_THRESHOLD = 1800;
    private static final long MAX_SEGMENT_DURATION = 60_000;

    @Autowired
    private AsrProperties asrProperties;

    @Autowired
    private MeetingRecordProperties recordProperties;

    @Override
    public AsrResultVO asrTranslateService(String audioPath) {
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

    @Override
    public AsrResultVO transcribeAudio(String audioPath) {
        File resolvedFile = resolveAudioFile(audioPath);
        if (!resolvedFile.exists() || !resolvedFile.isFile()) {
            throw new RuntimeException("音频文件不存在: " + resolvedFile.getAbsolutePath());
        }

        long durationMs = WavUtils.readDurationMs(resolvedFile);
        if (durationMs <= 0 || durationMs <= MAX_SINGLE_CALL_DURATION_MS) {
            // 读不出时长（保守起见按不切处理）或者本来就没超阈值，直接单次调用
            return asrTranslateService(resolvedFile.getAbsolutePath());
        }

        log.info("音频时长{}ms超过单次调用上限{}ms，切成{}分钟一段分批转写: {}",
                durationMs, MAX_SINGLE_CALL_DURATION_MS, MAX_SINGLE_CALL_DURATION_MS / 60000,
                resolvedFile.getAbsolutePath());

        File chunkDir = new File(resolvedFile.getParentFile(), "asr_chunks_" + System.currentTimeMillis());
        List<File> chunkFiles;
        try {
            chunkFiles = WavUtils.splitWavByDuration(resolvedFile, chunkDir, MAX_SINGLE_CALL_DURATION_MS);
        } catch (Exception e) {
            throw new RuntimeException("音频切片失败: " + e.getMessage(), e);
        }

        try {
            StringBuilder fullText = new StringBuilder();
            StringBuilder fullRawText = new StringBuilder();
            List<List<Long>> allTimestamps = new ArrayList<>();

            for (int i = 0; i < chunkFiles.size(); i++) {
                File chunkFile = chunkFiles.get(i);
                long chunkOffsetMs = (long) i * MAX_SINGLE_CALL_DURATION_MS;

                log.info("开始转写第{}/{}段: {}", i + 1, chunkFiles.size(), chunkFile.getName());
                AsrResultVO chunkResult = asrTranslateService(chunkFile.getAbsolutePath());

                // 拼接 text
                if (fullText.length() > 0) {
                    fullText.append("\n");
                }
                fullText.append(chunkResult.getText());

                // 拼接 rawText
                if (chunkResult.getRawText() != null && !chunkResult.getRawText().isEmpty()) {
                    if (fullRawText.length() > 0) {
                        fullRawText.append(" ");
                    }
                    fullRawText.append(chunkResult.getRawText());
                }

                // 拼接 timestamp，加上偏移量
                if (chunkResult.getTimestamp() != null) {
                    for (List<Long> ts : chunkResult.getTimestamp()) {
                        if (ts != null && ts.size() >= 2) {
                            List<Long> adjusted = new ArrayList<>(2);
                            adjusted.add(ts.get(0) + chunkOffsetMs);
                            adjusted.add(ts.get(1) + chunkOffsetMs);
                            allTimestamps.add(adjusted);
                        }
                    }
                }
            }

            AsrResultVO result = new AsrResultVO();
            result.setText(fullText.toString().trim());
            result.setRawText(fullRawText.toString().trim());
            result.setTimestamp(allTimestamps);

            log.info("========== 长音频分段转写合并结果 ==========");
            log.info("text={}", result.getText());
            log.info("rawText长度={}", result.getRawText() == null ? 0 : result.getRawText().length());
            log.info("timestamp数量={}", result.getTimestamp() == null ? 0 : result.getTimestamp().size());
            log.info("============================================");

            return result;
        } finally {
            // 切片是临时文件，转写完就没用了，用完必须删，不然长音频反复上传会在磁盘上堆一堆碎文件
            for (File f : chunkFiles) {
                if (f.exists() && !f.delete()) {
                    log.warn("ASR切片临时文件删除失败: {}", f.getAbsolutePath());
                }
            }
            if (chunkDir.exists() && !chunkDir.delete()) {
                log.warn("ASR切片临时目录删除失败（可能不为空）: {}", chunkDir.getAbsolutePath());
            }
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