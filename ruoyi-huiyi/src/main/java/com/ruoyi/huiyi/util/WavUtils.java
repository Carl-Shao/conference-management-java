package com.ruoyi.huiyi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WavUtils {

    private static final Logger log = LoggerFactory.getLogger(WavUtils.class);

    private static final int WAVE_HEADER_SIZE = 44;

    private WavUtils() {}

    public static long readDurationMs(File wavFile) {
        try {
            AudioFileFormat format = AudioSystem.getAudioFileFormat(wavFile);
            long frameLength = format.getFrameLength();
            float frameRate = format.getFormat().getFrameRate();
            if (frameLength <= 0 || frameRate <= 0) {
                return 0L;
            }
            return Math.round(frameLength * 1000.0 / frameRate);
        } catch (UnsupportedAudioFileException | IOException e) {
            log.warn("解析WAV文件时长失败: {}", wavFile.getAbsolutePath(), e);
            return 0L;
        }
    }

    public static void pcmBytesToWavFile(byte[] pcmData, File outputFile,
                                         int sampleRate, int channels, int bitDepth) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.setLength(0);
            raf.write(buildHeader(pcmData.length, sampleRate, channels, bitDepth));
            raf.write(pcmData);
        }
    }

    /**
     * 把一个wav文件按固定时长切成若干段，每一段都是独立、完整可播放的wav文件（自带wav头）。
     * 用 AudioInputStream 流式读取源文件、边读边写，不会把整个源文件（可能几百MB的长录音）
     * 一次性加载进内存，2小时的音频文件切片也不会占用过多内存。
     *
     * @param sourceFile      源wav文件
     * @param outputDir       切片输出目录，不存在会自动创建
     * @param chunkDurationMs 每段最大时长（毫秒）
     * @return 切出来的文件列表，按顺序排列（最后一段可能比 chunkDurationMs 短）
     */
    public static List<File> splitWavByDuration(File sourceFile, File outputDir, long chunkDurationMs)
            throws IOException, UnsupportedAudioFileException {
        List<File> result = new ArrayList<>();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(sourceFile)) {
            javax.sound.sampled.AudioFormat format = ais.getFormat();
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            int bitDepth = format.getSampleSizeInBits();
            int bytesPerFrame = format.getFrameSize(); // 一帧 = 每个采样点所有声道的字节数总和

            // 算出"chunkDurationMs对应多少字节"，按帧对齐（不能切在一帧中间，会导致噪音）
            long bytesPerChunk = (long) sampleRate * bytesPerFrame * chunkDurationMs / 1000;
            bytesPerChunk = (bytesPerChunk / bytesPerFrame) * bytesPerFrame;
            if (bytesPerChunk <= 0) {
                bytesPerChunk = bytesPerFrame; // 极端保护，理论上不会走到这里
            }

            byte[] readBuffer = new byte[8192];
            ByteArrayOutputStream currentChunk = new ByteArrayOutputStream();
            int chunkIndex = 0;
            int read;

            while ((read = ais.read(readBuffer)) != -1) {
                currentChunk.write(readBuffer, 0, read);

                // 攒够一整段就落盘，避免currentChunk本身无限增长占用内存
                while (currentChunk.size() >= bytesPerChunk) {
                    byte[] allBytes = currentChunk.toByteArray();
                    byte[] chunkData = Arrays.copyOfRange(allBytes, 0, (int) bytesPerChunk);
                    byte[] remaining = Arrays.copyOfRange(allBytes, (int) bytesPerChunk, allBytes.length);

                    File chunkFile = new File(outputDir, "chunk_" + chunkIndex + ".wav");
                    pcmBytesToWavFile(chunkData, chunkFile, sampleRate, channels, bitDepth);
                    result.add(chunkFile);
                    chunkIndex++;

                    currentChunk.reset();
                    currentChunk.write(remaining);
                }
            }

            // 收尾：最后剩下不足一整段的部分，也要单独写成一个文件，不能丢
            if (currentChunk.size() > 0) {
                File chunkFile = new File(outputDir, "chunk_" + chunkIndex + ".wav");
                pcmBytesToWavFile(currentChunk.toByteArray(), chunkFile, sampleRate, channels, bitDepth);
                result.add(chunkFile);
            }
        }

        return result;
    }

    public static void writePlaceholderHeader(RandomAccessFile raf, int sampleRate,
                                              int channels, int bitDepth) throws IOException {
        raf.write(buildHeader(0, sampleRate, channels, bitDepth));
    }

    /** 录制结束后，根据文件实际大小回填 RIFF chunk size 与 data chunk size 字段 */
    public static void patchHeader(File  wavFile) throws IOException {
        try(RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            long fileLength = raf.length();
            long dateLength = fileLength - WAVE_HEADER_SIZE;
            if (dateLength < 0) {
                throw new IOException("wav文件长度异常: " + wavFile.getAbsolutePath());
            }
            raf.seek(4);
            raf.write(intToLE((int) (fileLength - 8)));
            raf.seek(40);
            raf.write(intToLE((int) (dateLength)));
        }
    }

    /** 按实际采样数计算时长(ms)，不依赖挂钟时间，避免调度抖动导致偏移累积误差 */
    public static long calcDurationMs(int pcmByteLength, int sampleRate, int channels, int bitDepth) {
        int bytePerFrame = (bitDepth / 8) * channels;
        long totalFrames = pcmByteLength / bytePerFrame;
        return Math.round(totalFrames * 1000.0 / sampleRate);
    }

    private static byte[] buildHeader(int dataLength, int sampleRate, int channels, int bitDepth) {
        int byteRate = sampleRate * channels * (bitDepth / 8);
        short blockAlign = (short) (channels * (bitDepth / 8));

        ByteBuffer buffer = ByteBuffer.allocate(WAVE_HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put("RIFF".getBytes());
        buffer.putInt(36 + dataLength);
        buffer.put("WAVE".getBytes());

        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1); // PCM
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort(blockAlign);
        buffer.putShort((short) bitDepth);

        buffer.put("data".getBytes());
        buffer.putInt(dataLength);

        return buffer.array();
    }

    private static byte[] intToLE(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
}
