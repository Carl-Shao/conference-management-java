/**
 * 音频采集工具类
 * 使用 AudioContext 和 AudioWorklet 进行音频采集和处理
 */

class AudioCapture {
  constructor() {
    this.audioContext = null;
    this.analyser = null;
    this.microphone = null;
    this.workletNode = null;
    this.stream = null;
    this.onAudioDataCallback = null;
    this.isActive = false;
    this.isPaused = false;

    // 采样率设置为16kHz
    this.sampleRate = 16000;
    this.channels = 1; // 单声道
  }

  /**
   * 初始化音频采集环境
   */
  async init() {
    try {
      // 检查浏览器是否支持AudioContext指定采样率
      if (window.AudioContext) {
        this.audioContext = new AudioContext({ sampleRate: this.sampleRate });
      } else if (window.webkitAudioContext) {
        // Safari兼容
        this.audioContext = new webkitAudioContext({ sampleRate: this.sampleRate });
      } else {
        throw new Error('浏览器不支持Web Audio API');
      }

      // 如果浏览器不支持指定采样率，则创建默认采样率再进行处理
      if (this.audioContext.sampleRate !== this.sampleRate) {
        console.warn(`浏览器不支持${this.sampleRate}Hz采样率，实际使用${this.audioContext.sampleRate}Hz`);
        // 这里可以添加重采样逻辑，但简化处理直接使用浏览器默认采样率
      }

      // 创建分析器节点
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 2048;

      return true;
    } catch (error) {
      console.error('初始化音频环境失败:', error);
      throw new Error('无法初始化音频环境: ' + error.message);
    }
  }

  /**
   * 请求麦克风权限并开始采集
   */
  async start(onAudioData) {
    if (!this.audioContext) {
      await this.init();
    }

    try {
      // 请求麦克风权限
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: false, // 关闭回声消除以保持原始音频
          noiseSuppression: false, // 关闭噪音抑制
          autoGainControl: false,  // 关闭自动增益控制
        }
      });

      this.stream = stream;
      this.microphone = this.audioContext.createMediaStreamSource(stream);

      // 设置回调函数
      this.onAudioDataCallback = onAudioData;

      // 连接音频节点
      this.microphone.connect(this.analyser);

      // 创建ScriptProcessor节点来处理音频数据
      this.scriptProcessor = this.audioContext.createScriptProcessor(4096, 1, 1);
      this.scriptProcessor.onaudioprocess = this._handleAudioProcess.bind(this);

      this.analyser.connect(this.scriptProcessor);
      this.scriptProcessor.connect(this.audioContext.destination);

      this.isActive = true;
      this.isPaused = false;

      console.log('音频采集已开始');
      return true;
    } catch (error) {
      console.error('获取麦克风权限失败:', error);
      throw new Error('无法获取麦克风权限: ' + error.message);
    }
  }

  /**
   * 处理音频数据
   */
  _handleAudioProcess(audioProcessingEvent) {
    if (!this.isActive || this.isPaused) {
      return;
    }

    // 获取输入缓冲区
    const inputData = audioProcessingEvent.inputBuffer.getChannelData(0);

    // 将浮点数[-1, 1]转换为16位整数PCM数据
    const pcmData = new Int16Array(inputData.length);
    for (let i = 0; i < inputData.length; i++) {
      // 将浮点数转换为16位整数 (-32768 到 32767)
      pcmData[i] = Math.max(-32768, Math.min(32767, Math.floor(inputData[i] * 32767)));
    }

    // 将PCM数据发送出去
    if (this.onAudioDataCallback) {
      // 将Int16Array转换为ArrayBuffer并发送
      this.onAudioDataCallback(pcmData.buffer);
    }
  }

  /**
   * 获取音量等级，用于可视化波形
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

    const average = sum / dataArray.length;
    return average / 255; // 返回0-1之间的值
  }

  /**
   * 暂停音频采集
   */
  pause() {
    this.isPaused = true;
    console.log('音频采集已暂停');
  }

  /**
   * 恢复音频采集
   */
  resume() {
    this.isPaused = false;
    console.log('音频采集已恢复');
  }

  /**
   * 停止音频采集
   */
  async stop() {
    this.isActive = false;

    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect();
      this.scriptProcessor.onaudioprocess = null;
      this.scriptProcessor = null;
    }

    if (this.analyser) {
      this.analyser.disconnect();
      this.analyser = null;
    }

    if (this.microphone) {
      this.microphone.disconnect();
      this.microphone = null;
    }

    if (this.stream) {
      // 停止所有轨道
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }

    if (this.audioContext) {
      try {
        await this.audioContext.close();
      } catch (error) {
        console.error('关闭AudioContext时出错:', error);
      }
      this.audioContext = null;
    }

    console.log('音频采集已停止');
  }

  /**
   * 检查是否正在录制
   */
  isRecording() {
    return this.isActive && !this.isPaused;
  }
}

export default AudioCapture;