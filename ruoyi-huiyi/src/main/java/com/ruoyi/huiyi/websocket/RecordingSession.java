package com.ruoyi.huiyi.websocket;

import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.domain.Meeting;
import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;
import com.ruoyi.huiyi.util.WavUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单场会议录制的内存态会话（每个 meetingId 对应一个实例）。
 *
 * 线程安全说明：
 *  - appendAudio() 由 WebSocket IO 线程调用（音频帧到达时）
 *  - flushChunk()  由定时调度线程调用（周期性切片）
 *  - pause/resume/stop 由 REST 请求线程调用
 * 三者都会触碰 buffer / 文件句柄，统一用 ReentrantLock 保护。
 *
 * 不同会议室（不同 meetingId）各自持有独立的 RecordingSession 实例，
 * 互不加锁、互不阻塞 —— 这就是“不同用户录音并发”的实现方式，
 * 全程不依赖消息队列。
 */
public class RecordingSession {

    private static final Logger log = LoggerFactory.getLogger(RecordingSession.class);

    private final Long meetingId;
    private final MeetingRecordProperties recordConfig;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile MeetingRecordStatus status = MeetingRecordStatus.NOT_STARTED;
    private volatile WebSocketSession wsSession;
    private volatile ScheduledFuture<?> flushTask;

    private ByteArrayOutputStream currentChunkBuffer = new ByteArrayOutputStream();
    private RandomAccessFile fullAudioRaf;
    private File fullAudioFile;

    private final AtomicInteger seqNo = new AtomicInteger(0);
    private volatile long accumulatedOffsetMs = 0;

    /** 本场会议已提交给ASR线程池、尚未纳入统计的异步任务，stop时用来等待全部完成 */
    private final List<CompletableFuture<Void>> pendingAsrFutures = new CopyOnWriteArrayList<>();

    public RecordingSession(Long meetingId, MeetingRecordProperties recordConfig) {
        this.meetingId = meetingId;
        this.recordConfig = recordConfig;
    }

    public void open(File audioFile) throws IOException {
        lock.lock();
        try{
            this.fullAudioFile = audioFile;
            File parent = audioFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            this.fullAudioRaf = new RandomAccessFile(audioFile, "rw");
            this.fullAudioRaf.setLength(0);
            WavUtils.writePlaceholderHeader(fullAudioRaf, recordConfig.getSampleRate(),
                    recordConfig.getChannels(), recordConfig.getBitDepth());
            this.status = MeetingRecordStatus.RECORDING;
        } finally {
            lock.unlock();
        }
    }

    /** 音频帧写入：仅在 RECORDING 状态下生效，暂停/结束期间静默丢弃 */
    public void appendAudio(byte[] data) {
        if (status != MeetingRecordStatus.RECORDING) {
            return;
        }
        lock.lock();
        try {
            if (status != MeetingRecordStatus.RECORDING) {
                return;
            }
            currentChunkBuffer.write(data);
            fullAudioRaf.write(data);
        } catch (IOException e) {
            log.error("会议[{}]写入音频数据失败", meetingId, e);
        } finally {
            lock.unlock();
        }
    }

    /** 切片：取出当前缓冲区数据并重置；无数据时返回 null（正常现象） */
    public ChunkResult flushChunk() {
        lock.lock();
        try{
            if (currentChunkBuffer.size() == 0) {
                return null;
            }
            byte[] pcmBytes = currentChunkBuffer.toByteArray();
            currentChunkBuffer.reset();

            long durationMs = WavUtils.calcDurationMs(pcmBytes.length,
                    recordConfig.getSampleRate(), recordConfig.getChannels(), recordConfig.getBitDepth());
            long startOffset = accumulatedOffsetMs;
            long endOffset = startOffset + durationMs;
            accumulatedOffsetMs = endOffset;

            int currentSeq = seqNo.getAndIncrement();
            return new ChunkResult(currentSeq, pcmBytes, startOffset, endOffset);
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        lock.lock();
        try {
            status = MeetingRecordStatus.PAUSED;
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            status = MeetingRecordStatus.RECORDING;
        } finally {
            lock.unlock();
        }
    }

    /** 结束录制：关闭文件句柄并回填WAV头 */
    public void close() {
        lock.lock();
        try {
            status = MeetingRecordStatus.STOP_PENDING;
            if (fullAudioRaf != null) {
                fullAudioRaf.close();
            }
            if (fullAudioFile != null) {
                WavUtils.patchHeader(fullAudioFile);
            }
        } catch (IOException e) {
            log.error("会议[{}]关闭录音文件/回填WAV头失败", meetingId, e);
            status = MeetingRecordStatus.FAILED;
        } finally {
            lock.unlock();
        }
    }

    public void registerAsrFuture(CompletableFuture<Void> future) {
        pendingAsrFutures.add(future);
    }

    public List<CompletableFuture<Void>> getPendingAsrFutures() {
        return pendingAsrFutures;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public MeetingRecordProperties getRecordConfig() {
        return recordConfig;
    }

    public MeetingRecordStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingRecordStatus status) {
        this.status = status;
    }

    public WebSocketSession getWsSession() {
        return wsSession;
    }

    public void setWsSession(WebSocketSession wsSession) {
        this.wsSession = wsSession;
    }

    public ScheduledFuture<?> getFlushTask() {
        return flushTask;
    }

    public void setFlushTask(ScheduledFuture<?> flushTask) {
        this.flushTask = flushTask;
    }

    public File getFullAudioFile() {
        return fullAudioFile;
    }

    public long getAccumulatedOffsetMs() {
        return accumulatedOffsetMs;
    }

    /** 切片结果载体 */
    public static class ChunkResult {
        public final int seqNo;
        public final byte[] pcmByte;
        public final long startOffsetMs;
        public final long endOffsetMs;

        public ChunkResult(int seqNo, byte[] pcmByte, long startOffsetMs, long endOffsetMs) {
            this.seqNo = seqNo;
            this.pcmByte = pcmByte;
            this.startOffsetMs = startOffsetMs;
            this.endOffsetMs = endOffsetMs;
        }
    }
}
