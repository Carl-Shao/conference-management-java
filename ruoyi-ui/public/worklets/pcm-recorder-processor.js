// 作用：把麦克风采集到的 Float32 samples 转成后端要的 PCM16LE 格式，
// 通过 port.postMessage 传回主线程，主线程再用 ws.send() 发出去。
class PcmRecorderProcessor extends AudioWorkletProcessor {
    process(inputs) {
        const input = inputs[0];
        if (input && input[0] && input[0].length > 0) {
            const float32 = input[0];
            const int16 = new Int16Array(float32.length);
            for (let i = 0; i < float32.length; i++) {
                let s = Math.max(-1, Math.min(1, float32[i]));
                int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
            }
            // 第二个参数是 transferList，转移 buffer 所有权，避免拷贝
            this.port.postMessage(int16.buffer, [int16.buffer]);
        }
        return true; // 返回true保持处理器存活，不然浏览器会把它回收掉
    }
}

registerProcessor('pcm-recorder-processor', PcmRecorderProcessor);