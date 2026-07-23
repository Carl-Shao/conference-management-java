package com.ruoyi.huiyi.websocket;

import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.domain.MeetingTranscriptSegment;
import com.ruoyi.huiyi.mapper.MeetingTranscriptSegmentMapper;
import com.ruoyi.huiyi.service.impl.MeetingAsrServiceImpl;
import com.ruoyi.huiyi.util.WavUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.*;

@Component
public class MeetingSessionManager {

    private static final Logger log = LoggerFactory.getLogger(MeetingSessionManager.class);

    private final Map<Long, RecordingSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MeetingRecordProperties properties;

    @Autowired
    private MeetingAsrServiceImpl asrService;

    @Autowired
    private MeetingTranscriptSegmentMapper transcriptSegmentMapper;

    /** 切片定时调度线程池(每个会议室共用即可，任务很轻量) */
    private ThreadPoolTaskScheduler flushScheduler;

    /** ASR调用并发线程池：所有会议室共享，大小即为"允许同时调用ASR的最大并发数" */
    private ExecutorService asrExecutor;

    /** 结束录制后"等待转写完成再触发纪要生成"的调度线程池，不与ASR池抢资源 */
    private ExecutorService finalizeDispatchExecutor;

    @PostConstruct
    public void init() {
        flushScheduler = new ThreadPoolTaskScheduler();
        flushScheduler.setPoolSize(4);
        flushScheduler.setThreadNamePrefix("huiyi-record-flush-");
        flushScheduler.initialize();

        asrExecutor = new ThreadPoolExecutor(
                properties.getAsrConcurrency(), properties.getAsrConcurrency(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> new Thread(r, "huiyi-asr-worker"));

        finalizeDispatchExecutor = Executors.newCachedThreadPool(
                r -> new Thread(r, "huiyi-finalize-dispatch"));

        log.info("会议录制会话管理器初始化完成: asrConcurrency={}", properties.getAsrConcurrency());
    }

    @PreDestroy
    public void destroy() {
        if (flushScheduler != null) {
            flushScheduler.shutdown();
        }
        if (asrExecutor != null) {
            asrExecutor.shutdown();
        }
        if (finalizeDispatchExecutor != null) {
            finalizeDispatchExecutor.shutdown();
        }
    }

    /** 开始录制：创建内存会话 + 完整录音文件，并启动周期性切片任务 */
    public RecordingSession startSession(Long meetingId) throws IOException {
        if (sessions.containsKey(meetingId)) {
            throw new IllegalStateException("会议[" + meetingId + "]已存在进行中的录制会话");
        }
        RecordingSession session = new RecordingSession(meetingId, properties);
        File audioFile = buildAudioFile(meetingId);
        session.open(audioFile);

        ScheduledFuture<?> future = flushScheduler.scheduleAtFixedRate(
                () -> doFlushAndSubmitAsr(session),
                properties.getChunkIntervalSeconds() * 1000L);
        session.setFlushTask(future);

        sessions.put(meetingId, session);
        log.info("会议[{}]录制会话已启动，录音文件: {}", meetingId, audioFile.getAbsolutePath());
        return session;
    }

    /** 切片 + 提交异步ASR任务（在定时调度线程/REST线程里被同步调用，本身很快返回） */
    private void doFlushAndSubmitAsr(RecordingSession session){
        RecordingSession.ChunkResult chunk;
        try {
            chunk = session.flushChunk();
        } catch (Exception e) {
            log.error("会议[{}]音频切片失败", session.getMeetingId(), e);
            return;
        }

        if (chunk == null) {
            return;
        }
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> processChunk(session, chunk), asrExecutor);
        session.registerAsrFuture(future);
    }

    /** 真正在 asrExecutor 线程池里执行的任务体：写chunk文件 -> 调ASR -> 落库 -> 推送前端 */
    public void processChunk(RecordingSession session, RecordingSession.ChunkResult chunk) {
        Long meetingId = session.getMeetingId();
        File chunkFile = buildChunkFile(meetingId, chunk.seqNo);
        try {
            WavUtils.pcmBytesToWavFile(chunk.pcmBytes, chunkFile,
                    session.getRecordConfig().getSampleRate(),
                    session.getRecordConfig().getChannels(),
                    session.getRecordConfig().getBitDepth());

            String text = asrService.asrTranslateService(chunkFile.getAbsolutePath());
        } catch (Exception e) {
            // 单个分片转写失败不影响整场会议，记录空文本占位，保证seqNo连续、后续拼接不缺段
            log.error("会议[{}]分片[{}]转写失败", meetingId, chunk.seqNo, e);
            MeetingTranscriptSegment segment = new MeetingTranscriptSegment();
            segment.setMeetingId(meetingId);
            segment.setSeqNo(chunk.seqNo);
            segment.setStartOffsetMs(chunk.startOffsetMs);
            segment.setEndOffsetMs(chunk.endOffsetMs);
            segment.setText("");
            transcriptSegmentMapper.insertSegment(segment);
        } finally {
            // chunk临时文件用完即删，避免磁盘堆积；完整录音文件fullAudioFile不受影响
            if (chunkFile.exists() && !chunkFile.delete()) {
                log.warn("会议[{}]分片临时文件删除失败: {}", meetingId, chunkFile.getAbsolutePath());
            }
        }
    }

    public RecordingSession getSession(Long meetingId) {
        return sessions.get(meetingId);
    }

    public boolean hasSession(Long meetingId) {
        return sessions.containsKey(meetingId);
    }

    private File buildAudioFile(Long meetingId) {
        String dateDir = java.time.LocalDate.now().toString();
        File dir = new File(properties.getAudioBasePath(), dateDir);
        return new File(dir, "meeting_" + meetingId + "_full.wav");
    }

    private File buildChunkFile(Long meetingId, int seqNo) {
        String dateDir = java.time.LocalDate.now().toString();
        File dir = new File(properties.getAudioBasePath(), dateDir + "/chunks/" + meetingId);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "chunk_" + seqNo + ".wav");
    }
}
