package com.ruoyi.huiyi.util;

/**
 * 解析 HTTP Range 请求头，纯字符串/数值处理，不涉及任何IO，可以单独写单元测试。
 * 音频/视频这类支持拖进度条的流式接口都能复用这个工具类，不用每个接口自己重写一遍。
 */
public final class HttpRangeUtils {

    private HttpRangeUtils() {}

    public static class RangeResult {
        public final boolean isRangeRequest;
        public final boolean satisfiable;
        public final long start;
        public final long end;

        public RangeResult(boolean isRangeRequest, boolean satisfiable, long start, long end) {
            this.isRangeRequest = isRangeRequest;
            this.satisfiable = satisfiable;
            this.start = start;
            this.end = end;
        }
    }

    public static RangeResult parse(String rangeHeader, long fileLength) {
        if(rangeHeader == null || rangeHeader.isEmpty() || !rangeHeader.startsWith("bytes=")) {
            return new RangeResult(false,true, 0, fileLength - 1);
        }

        long start = 0;
        long end = fileLength - 1;
        String[] parts = rangeHeader.substring(6).split("-");
        try {
            if (parts.length > 0 && !parts[0].isEmpty()) start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
        } catch(NumberFormatException ignored) {}
        if (end >= fileLength) end = fileLength - 1;
        boolean satisfiable = start >= 0 && start <= end;

        return new RangeResult(true, satisfiable, start, end);
    }
}
