package com.ruoyi.huiyi.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public final class WavUtils {

    private static final int WAVE_HEADER_SIZE = 44;

    private WavUtils() {}

    public static void pcmBytesToWavFile(byte[] pcmData, File outputFile,
                                         int sampleRate, int channels, int bitDepth) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.setLength(0);
            raf.write(buildHeader(pcmData.length, sampleRate, channels, bitDepth));
            raf.write(pcmData);
        }
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
