package com.ruoyi.huiyi.websocket;

import com.ruoyi.huiyi.config.MeetingRecordProperties;
import com.ruoyi.huiyi.domain.enums.MeetingRecordStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
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
}
