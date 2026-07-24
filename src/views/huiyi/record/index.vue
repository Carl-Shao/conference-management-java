<template>
  <div class="meeting-record-page">
    <div class="page-header">
      <h2>会议录制</h2>
    </div>

    <!-- 录制状态指示 -->
    <div class="status-info" v-if="recordStatus !== 0 && recordStatus !== 5 && recordStatus !== 6">
      <div class="timer">{{ formattedTime }}</div>
    </div>

    <!-- 音量波形显示 -->
    <div class="volume-visualizer" v-if="recordStatus === 1 || recordStatus === 2">
      <div class="bars">
        <div
          v-for="n in 20"
          :key="n"
          class="bar"
          :style="{ height: getBarHeight(n) }"
        ></div>
      </div>
    </div>

    <!-- 录制控制按钮 -->
    <div class="control-section">
      <!-- 未开始状态 -->
      <div v-if="recordStatus === 0" class="initial-state">
        <el-button
          type="primary"
          size="large"
          class="record-btn"
          @click="startRecord"
          :loading="startLoading"
          :disabled="startLoading"
        >
          <i class="el-icon-video-play"></i>
          开始录制
        </el-button>
      </div>

      <!-- 录制中状态 -->
      <div v-else-if="recordStatus === 1" class="recording-state">
        <el-button-group>
          <el-button
            type="warning"
            @click="pauseRecord"
            :loading="pauseLoading"
            :disabled="pauseLoading"
          >
            <i class="el-icon-video-pause"></i>
            暂停
          </el-button>
          <el-button
            type="danger"
            @click="stopRecord"
            :loading="stopLoading"
            :disabled="stopLoading"
          >
            <i class="el-icon-switch-button"></i>
            结束
          </el-button>
        </el-button-group>
      </div>

      <!-- 暂停状态 -->
      <div v-else-if="recordStatus === 2" class="paused-state">
        <el-button-group>
          <el-button
            type="primary"
            @click="resumeRecord"
            :loading="resumeLoading"
            :disabled="resumeLoading"
          >
            <i class="el-icon-video-play"></i>
            继续
          </el-button>
          <el-button
            type="danger"
            @click="stopRecord"
            :loading="stopLoading"
            :disabled="stopLoading"
          >
            <i class="el-icon-switch-button"></i>
            结束
          </el-button>
        </el-button-group>
      </div>

      <!-- 处理中状态 -->
      <div v-else-if="recordStatus === 3 || recordStatus === 4" class="processing-state">
        <div class="loading-wrapper">
          <i class="el-icon-loading"></i>
          <span class="processing-text">正在生成会议纪要...</span>
        </div>
      </div>

      <!-- 异常状态 -->
      <div v-else-if="recordStatus === 6" class="error-state">
        <div class="error-message">{{ recordStatusDesc || '录制过程中出现异常' }}</div>
        <el-button
          type="primary"
          @click="retryRecord"
        >
          <i class="el-icon-refresh"></i>
          重试
        </el-button>
      </div>
    </div>

    <!-- 实时字幕区域 -->
    <div class="subtitles-section" v-if="recordStatus === 1 || recordStatus === 2">
      <h4>实时字幕</h4>
      <realtime-subtitles :subtitles="subtitles" />
    </div>

    <!-- 结果展示 -->
    <div class="result-section" v-if="recordStatus === 5">
      <summary-result :summary-data="summaryData" />
    </div>
  </div>
</template>

<script>
import { startRecord, pauseRecord, resumeRecord, stopRecord, getRecordStatus } from '@/api/huiyi/record'
import AudioCapture from '@/utils/huiyi/audioCapture'
import RealtimeSubtitles from '@/components/Huiyi/RealtimeSubtitles'
import SummaryResult from '@/components/Huiyi/SummaryResult'

// 状态常量
const STATUS = {
  NOT_STARTED: 0,
  RECORDING: 1,
  PAUSED: 2,
  STOPPED_PENDING: 3,
  PROCESSING: 4,
  COMPLETED: 5,
  ERROR: 6
}

export default {
  name: 'MeetingRecord',
  components: {
    RealtimeSubtitles,
    SummaryResult
  },
  data() {
    return {
      // 会议ID，这里暂时使用模拟ID
      meetingId: 'demo-meeting-123',

      // 录制状态
      recordStatus: STATUS.NOT_STARTED,
      recordStatusDesc: '',
      recordDurationMs: 0,

      // 控制按钮加载状态
      startLoading: false,
      pauseLoading: false,
      resumeLoading: false,
      stopLoading: false,

      // WebSocket连接
      ws: null,
      wsConnected: false,
      wsRetryCount: 0,
      maxWsRetry: 3,

      // 计时器相关
      startTime: null,
      currentTime: 0,
      timerInterval: null,

      // 音频采集相关
      audioCapture: null,
      volumeLevel: 0,
      volumeAnimation: [],

      // 实时字幕
      subtitles: [],

      // 结果数据
      summaryData: {},

      // 状态轮询定时器
      statusPollingTimer: null
    }
  },
  computed: {
    formattedTime() {
      const totalSeconds = Math.floor(this.currentTime / 1000);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;

      if (hours > 0) {
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      } else {
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      }
    }
  },
  watch: {
    recordStatus(newVal) {
      // 根据状态更新UI行为
      if (newVal === STATUS.RECORDING) {
        this.startTimer();
      } else {
        this.stopTimer();
      }
    }
  },
  created() {
    // 初始化音频采集器
    this.audioCapture = new AudioCapture();
    this.audioCapture.setOnVolumeChange((level) => {
      this.volumeLevel = level;
      this.updateVolumeAnimation();
    });

    // 设置音频数据回调，当采集到音频时发送到WebSocket
    this.audioCapture.setOnAudioData((audioData) => {
      if (this.ws && this.wsConnected && this.recordStatus === STATUS.RECORDING) {
        try {
          this.ws.send(audioData);
        } catch (error) {
          console.error('Failed to send audio data:', error);
        }
      }
    });
  },
  mounted() {
    // 监听页面卸载事件，确保在录制状态下能保存记录
    window.addEventListener('beforeunload', this.handleBeforeUnload);
  },
  beforeDestroy() {
    // 清理资源
    this.cleanup();
    window.removeEventListener('beforeunload', this.handleBeforeUnload);
  },
  methods: {
    // 开始录制
    async startRecord() {
      if (this.recordStatus !== STATUS.NOT_STARTED) {
        return;
      }

      this.startLoading = true;
      try {
        // 请求麦克风权限并初始化音频采集
        await this.audioCapture.init();

        // 调用开始录制接口
        const response = await startRecord(this.meetingId);
        const { meetingId, recordStatus, wsPath } = response.data;

        // 更新本地状态
        this.meetingId = meetingId;
        this.recordStatus = recordStatus;

        // 连接WebSocket
        await this.connectWebSocket(wsPath);

        // 开始音频采集
        this.audioCapture.start();

        // 开始计时
        this.startTime = Date.now();
        this.currentTime = 0;

        this.$message.success('录制已开始');
      } catch (error) {
        console.error('Failed to start recording:', error);

        // 权限被拒绝或其他错误
        if (error.name === 'NotAllowedError') {
          this.$message.error('麦克风权限被拒绝，请在浏览器设置中开启权限');
        } else if (error.message && error.message.includes('No AudioDevice')) {
          this.$message.error('未检测到麦克风设备，请检查硬件连接');
        } else {
          this.$message.error(error.message || '开始录制失败，请重试');
        }

        // 确保清理资源
        await this.cleanup();
        this.recordStatus = STATUS.NOT_STARTED;
      } finally {
        this.startLoading = false;
      }
    },

    // 暂停录制
    async pauseRecord() {
      if (this.recordStatus !== STATUS.RECORDING) {
        return;
      }

      this.pauseLoading = true;
      try {
        await pauseRecord(this.meetingId);
        this.recordStatus = STATUS.PAUSED;

        // 暂停音频采集（但不关闭流）
        this.audioCapture.pause();

        this.$message.info('录制已暂停');
      } catch (error) {
        console.error('Failed to pause recording:', error);
        this.$message.error('暂停录制失败，请重试');
      } finally {
        this.pauseLoading = false;
      }
    },

    // 继续录制
    async resumeRecord() {
      if (this.recordStatus !== STATUS.PAUSED) {
        return;
      }

      this.resumeLoading = true;
      try {
        await resumeRecord(this.meetingId);
        this.recordStatus = STATUS.RECORDING;

        // 恢复音频采集
        this.audioCapture.resume();

        // 重新开始计时
        this.startTime = Date.now() - this.currentTime; // 调整起始时间

        this.$message.success('录制已继续');
      } catch (error) {
        console.error('Failed to resume recording:', error);
        this.$message.error('继续录制失败，请重试');
      } finally {
        this.resumeLoading = false;
      }
    },

    // 结束录制
    async stopRecord() {
      if (this.recordStatus !== STATUS.RECORDING && this.recordStatus !== STATUS.PAUSED) {
        return;
      }

      this.stopLoading = true;
      try {
        await stopRecord(this.meetingId);
        this.recordStatus = STATUS.STOPPED_PENDING;

        // 停止所有相关操作
        this.stopTimer();
        this.audioCapture.stop();
        this.closeWebSocket();

        // 开始轮询状态
        this.pollStatus();

        this.$message.info('正在处理录制内容...');
      } catch (error) {
        console.error('Failed to stop recording:', error);
        this.$message.error('结束录制失败，请重试');
      } finally {
        this.stopLoading = false;
      }
    },

    // 重试录制
    retryRecord() {
      this.cleanup();
      this.recordStatus = STATUS.NOT_STARTED;
      this.subtitles = [];
      this.summaryData = {};
      this.currentTime = 0;
    },

    // 连接WebSocket
    async connectWebSocket(wsPath) {
      return new Promise((resolve, reject) => {
        try {
          // 构造WebSocket地址
          const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
          const wsUrl = `${protocol}//${window.location.host}${wsPath}`;

          this.ws = new WebSocket(wsUrl);

          this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.wsConnected = true;
            this.wsRetryCount = 0; // 重置重试次数
            resolve();

            // 启动心跳
            this.startHeartbeat();
          };

          this.ws.onmessage = (event) => {
            // 处理服务端消息
            if (typeof event.data === 'string') {
              try {
                const data = JSON.parse(event.data);

                // 如果是服务端返回的实时字幕
                if (data.hasOwnProperty('seqNo') && data.hasOwnProperty('text')) {
                  // 添加到字幕列表
                  this.subtitles.push(data);

                  // 限制字幕数量，避免过多占用内存
                  if (this.subtitles.length > 100) {
                    this.subtitles = this.subtitles.slice(-100);
                  }
                }
                // 如果是心跳响应
                else if (event.data === 'pong') {
                  // 心跳响应，无需处理
                }
              } catch (parseError) {
                console.error('Failed to parse WebSocket message:', parseError);
              }
            }
          };

          this.ws.onclose = (event) => {
            console.log('WebSocket closed:', event.code, event.reason);
            this.wsConnected = false;

            // 如果当前仍处于录制状态，尝试重连
            if ((this.recordStatus === STATUS.RECORDING || this.recordStatus === STATUS.PAUSED) &&
                this.wsRetryCount < this.maxWsRetry) {
              this.wsRetryCount++;
              console.log(`Attempting to reconnect WebSocket (${this.wsRetryCount}/${this.maxWsRetry})`);

              setTimeout(() => {
                this.connectWebSocket(wsPath).catch(err => {
                  console.error('Failed to reconnect WebSocket:', err);
                  if (this.wsRetryCount >= this.maxWsRetry) {
                    this.$message.error('网络异常，请检查网络后重试');
                    // 在这种情况下，可能需要结束录制流程
                    this.recordStatus = STATUS.ERROR;
                    this.recordStatusDesc = '网络连接异常，请检查网络后重试';
                  }
                });
              }, 2000 * this.wsRetryCount); // 递增延迟重连
            }
          };

          this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.wsConnected = false;
          };
        } catch (error) {
          console.error('Failed to create WebSocket connection:', error);
          reject(error);
        }
      });
    },

    // 关闭WebSocket连接
    closeWebSocket() {
      if (this.ws) {
        this.ws.close();
        this.ws = null;
        this.wsConnected = false;
      }
    },

    // 启动心跳机制
    startHeartbeat() {
      // 清除旧的心跳定时器
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer);
      }

      // 每20秒发送一次心跳
      this.heartbeatTimer = setInterval(() => {
        if (this.ws && this.wsConnected) {
          this.ws.send('ping');
        }
      }, 20000);
    },

    // 开始计时器
    startTimer() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval);
      }

      this.timerInterval = setInterval(() => {
        if (this.startTime) {
          this.currentTime = Date.now() - this.startTime;
        }
      }, 100);
    },

    // 停止计时器
    stopTimer() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval);
        this.timerInterval = null;
      }
    },

    // 更新音量动画
    updateVolumeAnimation() {
      // 生成随机高度值来模拟动画效果
      const newHeights = [];
      for (let i = 0; i < 20; i++) {
        // 基础高度加上随机波动，与当前音量水平相关
        const baseHeight = 10 + Math.random() * 20;
        const volumeFactor = this.volumeLevel * 40; // 根据音量调整高度
        const fluctuation = Math.sin(Date.now() / 200 + i) * 10; // 时间波动
        newHeights.push(baseHeight + volumeFactor + fluctuation);
      }
      this.volumeAnimation = newHeights;
    },

    // 获取柱状图高度
    getBarHeight(index) {
      if (this.volumeAnimation[index] !== undefined) {
        return this.volumeAnimation[index] + 'px';
      }
      return (10 + Math.random() * 20) + 'px'; // 初始高度
    },

    // 轮询录制状态
    pollStatus() {
      if (this.statusPollingTimer) {
        clearInterval(this.statusPollingTimer);
      }

      this.statusPollingTimer = setInterval(async () => {
        try {
          const response = await getRecordStatus(this.meetingId);
          const { recordStatus, recordStatusDesc, recordDurationMs, audioFilePath, summaryText } = response.data;

          this.recordStatus = recordStatus;
          this.recordStatusDesc = recordStatusDesc;
          this.recordDurationMs = recordDurationMs;

          if (recordStatus === STATUS.COMPLETED) {
            // 获取完整结果数据
            this.summaryData = {
              ...response.data
            };

            // 停止轮询
            this.stopPolling();
            this.$message.success('会议纪要生成完成！');
          } else if (recordStatus === STATUS.ERROR) {
            // 录制异常
            this.stopPolling();
            this.$message.error(recordStatusDesc || '录制处理异常');
          }
          // 其他状态继续轮询
        } catch (error) {
          console.error('Failed to poll status:', error);
          this.stopPolling();
          this.recordStatus = STATUS.ERROR;
          this.recordStatusDesc = '获取状态失败';
          this.$message.error('获取录制状态失败');
        }
      }, 2000); // 每2秒轮询一次
    },

    // 停止轮询
    stopPolling() {
      if (this.statusPollingTimer) {
        clearInterval(this.statusPollingTimer);
        this.statusPollingTimer = null;
      }
    },

    // 页面卸载前处理
    handleBeforeUnload(event) {
      // 如果仍在录制中，尝试停止录制
      if (this.recordStatus === STATUS.RECORDING || this.recordStatus === STATUS.PAUSED) {
        // 使用navigator.sendBeacon尽力保存录制状态
        try {
          const url = `${process.env.VUE_APP_BASE_API}/huiyi/record/${this.meetingId}/stop`;
          navigator.sendBeacon(url, '');
        } catch (error) {
          console.error('Failed to send stop request on unload:', error);
        }
      }
    },

    // 清理资源
    async cleanup() {
      // 停止所有定时器
      this.stopTimer();
      this.stopPolling();

      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer);
      }

      // 关闭WebSocket
      this.closeWebSocket();

      // 销毁音频采集器
      if (this.audioCapture) {
        this.audioCapture.destroy();
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-record-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;

  .page-header {
    text-align: center;
    margin-bottom: 30px;

    h2 {
      margin: 0;
      color: #303133;
      font-size: 24px;
    }
  }

  .status-info {
    text-align: center;
    margin-bottom: 20px;

    .timer {
      font-size: 28px;
      font-weight: bold;
      color: #606266;
      font-family: 'Courier New', monospace;
    }
  }

  .volume-visualizer {
    width: 100%;
    height: 100px;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 2px;
    margin: 30px 0;
    padding: 10px;
    background-color: #f5f7fa;
    border-radius: 8px;

    .bars {
      display: flex;
      align-items: flex-end;
      height: 80px;

      .bar {
        width: 6px;
        background: linear-gradient(to top, #409eff, #66b1ff);
        border-radius: 2px;
        transition: height 0.1s ease;
        min-height: 2px;
      }
    }
  }

  .control-section {
    text-align: center;
    margin: 30px 0;

    .record-btn {
      width: 180px;
      height: 180px;
      border-radius: 50%;
      font-size: 18px;
      padding: 0;
    }

    .el-button-group {
      .el-button {
        min-width: 100px;
      }
    }

    .loading-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;

      .el-icon-loading {
        font-size: 24px;
      }

      .processing-text {
        font-size: 16px;
        color: #606266;
      }
    }

    .error-message {
      color: #f56c6c;
      margin-bottom: 15px;
      font-size: 16px;
    }
  }

  .subtitles-section {
    margin-top: 30px;

    h4 {
      margin: 0 0 10px 0;
      color: #303133;
      font-size: 16px;
    }
  }

  .result-section {
    margin-top: 30px;
  }
}
</style>