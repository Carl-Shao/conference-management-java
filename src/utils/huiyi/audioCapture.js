/**
 * 音频采集工具类 - 封装AudioWorklet相关逻辑
 */
class AudioCapture {
  constructor() {
    this.audioContext = null;
    this.analyser = null;
    this.microphone = null;
    this.scriptProcessor = null;
    this.workletNode = null;
    this.mediaStream = null;
    this.isActive = false;
    this.sampleRate = 16000;
    this.onAudioDataCallback = null;
    this.onVolumeChange = null;
  }

  /**
   * 初始化音频上下文和相关节点
   */
  async init() {
    try {
      // 创建音频上下文，指定采样率为16000Hz
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)({
        sampleRate: this.sampleRate
      });

      // 如果浏览器不支持直接指定采样率，则使用默认采样率并在后续进行重采样
      if (this.audioContext.sampleRate !== this.sampleRate) {
        console.warn(`Browser does not support ${this.sampleRate}Hz sample rate, actual: ${this.audioContext.sampleRate}Hz`);
        // 注意：实际应用中需要实现重采样逻辑，这里仅作提醒
      }

      // 创建分析器
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.8;

      // 请求麦克风权限
      const constraints = {
        audio: {
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false
        }
      };

      this.mediaStream = await navigator.mediaDevices.getUserMedia(constraints);

      // 创建音频源
      this.microphone = this.audioContext.createMediaStreamSource(this.mediaStream);

      // 连接分析器
      this.microphone.connect(this.analyser);

      // 如果支持AudioWorklet，优先使用
      if (this.audioContext.audioWorklet) {
        await this._initAudioWorklet();
      } else {
        // 使用ScriptProcessor作为降级方案
        this._initScriptProcessor();
      }

      return true;
    } catch (error) {
      console.error('Failed to initialize audio capture:', error);
      throw error;
    }
  }

  /**
   * 初始化AudioWorklet（如果支持）
   */
  async _initAudioWorklet() {
    // 创建一个内联的AudioWorklet处理器代码
    const workletCode = `
      class PCMEncoder extends AudioWorkletProcessor {
        constructor() {
          super();
          this.buffer = new Float32Array();
        }

        process(inputs, outputs, parameters) {
          const input = inputs[0];
          if (input.length > 0) {
            const inputData = input[0]; // 单声道

            // 将Float32数组转换为Int16数组 (PCM16LE)
            const int16Buffer = new Int16Array(inputData.length);
            for (let i = 0; i < inputData.length; i++) {
              int16Buffer[i] = Math.max(-1, Math.min(1, inputData[i])) * 0x7FFF;
            }

            // 发送数据到主线程
            this.port.postMessage({
              type: 'audio_data',
              data: int16Buffer.buffer
            });
          }
          return true;
        }
      }

      registerProcessor('pcm-encoder', PCMEncoder);
    `;

    // 创建Blob URL
    const blob = new Blob([workletCode], { type: 'application/javascript' });
    const url = URL.createObjectURL(blob);

    try {
      await this.audioContext.audioWorklet.addModule(url);

      this.workletNode = new AudioWorkletNode(this.audioContext, 'pcm-encoder');
      this.workletNode.port.onmessage = (event) => {
        if (event.data.type === 'audio_data' && this.onAudioDataCallback) {
          this.onAudioDataCallback(event.data.data);
        }
      };

      // 连接音频源到worklet
      this.microphone.connect(this.workletNode);
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  /**
   * 初始化ScriptProcessor作为降级方案
   */
  _initScriptProcessor() {
    const bufferSize = 4096;
    this.scriptProcessor = this.audioContext.createScriptProcessor(bufferSize, 1, 1);

    this.scriptProcessor.onaudioprocess = (event) => {
      const inputData = event.inputBuffer.getChannelData(0); // 单声道

      // 将Float32数组转换为Int16数组 (PCM16LE)
      const outputBuffer = new Int16Array(inputData.length);
      for (let i = 0; i < inputData.length; i++) {
        outputBuffer[i] = Math.max(-1, Math.min(1, inputData[i])) * 0x7FFF;
      }

      // 触发音频数据回调
      if (this.onAudioDataCallback) {
        this.onAudioDataCallback(outputBuffer.buffer);
      }
    };

    // 连接音频源到ScriptProcessor
    this.microphone.connect(this.scriptProcessor);
  }

  /**
   * 开始采集音频
   */
  start() {
    if (!this.audioContext) {
      throw new Error('Audio context not initialized');
    }

    if (this.audioContext.state === 'suspended') {
      this.audioContext.resume();
    }

    this.isActive = true;
  }

  /**
   * 停止采集音频
   */
  stop() {
    this.isActive = false;
  }

  /**
   * 暂停采集音频（保持连接，但不发送数据）
   */
  pause() {
    this.isActive = false;
  }

  /**
   * 恢复采集音频
   */
  resume() {
    this.isActive = true;
  }

  /**
   * 获取当前音量级别（0-1之间）
   */
  getVolumeLevel() {
    if (!this.analyser) {
      return 0;
    }

    const dataArray = new Uint8Array(this.analyser.frequencyBinCount);
    this.analyser.getByteFrequencyData(dataArray);

    // 计算平均音量
    let sum = 0;
    for (let i = 0; i < dataArray.length; i++) {
      sum += dataArray[i];
    }

    return sum / dataArray.length / 255;
  }

  /**
   * 设置音频数据回调函数
   */
  setOnAudioData(callback) {
    this.onAudioDataCallback = callback;
  }

  /**
   * 设置音量变化回调函数
   */
  setOnVolumeChange(callback) {
    this.onVolumeChange = callback;
  }

  /**
   * 销毁资源
   */
  destroy() {
    this.isActive = false;

    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect();
      this.scriptProcessor = null;
    }

    if (this.workletNode) {
      this.workletNode.disconnect();
      this.workletNode.port.close();
      this.workletNode = null;
    }

    if (this.microphone) {
      this.microphone.disconnect();
      this.microphone = null;
    }

    if (this.analyser) {
      this.analyser.disconnect();
      this.analyser = null;
    }

    if (this.mediaStream) {
      const tracks = this.mediaStream.getTracks();
      tracks.forEach(track => track.stop());
      this.mediaStream = null;
    }

    if (this.audioContext && this.audioContext.state !== 'closed') {
      this.audioContext.close();
      this.audioContext = null;
    }
  }
}

export default AudioCapture;