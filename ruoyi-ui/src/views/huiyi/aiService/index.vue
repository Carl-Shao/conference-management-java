<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="24">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-microphone"></i> AI会议服务控制台</span>
          </div>

          <!-- 健康检查和状态区域 -->
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="info-section">
                <h4><i class="el-icon-info"></i> 服务状态</h4>
                <div class="status-info">
                  <el-tag :type="healthStatusTag" size="medium">{{ healthStatusText }}</el-tag>
                  <el-button
                    type="primary"
                    size="mini"
                    @click="checkHealth"
                    :loading="checkingHealth"
                    style="margin-left: 10px;">
                    {{ checkingHealth ? '检查中...' : '刷新状态' }}
                  </el-button>
                </div>

                <!-- 服务详细状态 -->
                <div v-if="serviceStatus" class="status-details">
                  <p><strong>ASR运行状态:</strong>
                    <el-tag :type="serviceStatus.asr_status.is_running ? 'success' : 'danger'">
                      {{ serviceStatus.asr_status.is_running ? '运行中' : '已停止' }}
                    </el-tag>
                  </p>
                  <p><strong>活跃房间:</strong> {{ serviceStatus.asr_status.active_rooms.join(', ') || '无' }}</p>
                  <p><strong>ASR模型:</strong> {{ serviceStatus.service_info.asr_model }}</p>
                  <p><strong>LLM模型:</strong> {{ serviceStatus.service_info.llm_model }}</p>
                </div>
              </div>
            </el-col>

            <el-col :span="12">
              <div class="info-section">
                <h4><i class="el-icon-setting"></i> 控制面板</h4>
                <div class="control-panel">
                  <el-form :model="controlForm" label-width="100px">
                    <el-form-item label="房间ID:">
                      <el-input v-model="controlForm.roomId" placeholder="请输入房间ID" />
                    </el-form-item>
                    <el-form-item label="RTSP地址:">
                      <el-input v-model="controlForm.rtspUrl" placeholder="请输入RTSP流地址" />
                    </el-form-item>
                    <el-form-item>
                      <el-button-group>
                        <el-button
                          type="success"
                          icon="el-icon-video-play"
                          @click="startAsrRecording"
                          :disabled="!controlForm.roomId || !controlForm.rtspUrl || asrRecordingStatus === 'recording'"
                          :loading="startingAsrRecording">
                          {{ asrRecordingStatus === 'recording' ? 'ASR录音中...' : '开始ASR录音' }}
                        </el-button>
                        <el-button
                          type="warning"
                          icon="el-icon-video-pause"
                          @click="stopAsrRecording"
                          :disabled="asrRecordingStatus !== 'recording'"
                          :loading="stoppingAsrRecording">
                          停止ASR录音
                        </el-button>
                      </el-button-group>

                      <br/><br/>

                      <el-button-group>
                        <el-button
                          type="primary"
                          icon="el-icon-circle-plus-outline"
                          @click="startFullRecording"
                          :disabled="!controlForm.roomId || !controlForm.rtspUrl || fullRecordingStatus === 'recording'"
                          :loading="startingFullRecording">
                          {{ fullRecordingStatus === 'recording' ? '完整录音中...' : '开始完整录音' }}
                        </el-button>
                        <el-button
                          type="info"
                          icon="el-icon-circle-close"
                          @click="stopFullRecording"
                          :disabled="fullRecordingStatus !== 'recording'"
                          :loading="stoppingFullRecording">
                          停止完整录音
                        </el-button>
                      </el-button-group>
                    </el-form-item>
                  </el-form>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 转录文本显示区域 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="24">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-chat-line-square"></i> 实时转录文本</span>
            <el-button
              style="float: right; padding: 3px 0"
              type="text"
              icon="el-icon-refresh"
              @click="fetchTranscript"
              :loading="fetchingTranscript">
              刷新转录
            </el-button>
          </div>

          <div class="transcript-container">
            <el-input
              type="textarea"
              :rows="8"
              placeholder="实时转录文本将在此显示..."
              v-model="transcriptText"
              readonly>
            </el-input>
            <div style="margin-top: 10px;">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-download"
                @click="downloadTranscript"
                :disabled="!transcriptText">
                下载转录文本
              </el-button>
              <el-button
                type="success"
                size="small"
                icon="el-icon-document"
                @click="generateMinutesFromTranscript"
                :disabled="!transcriptText">
                生成会议纪要
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 录音路径显示 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="12">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-folder-opened"></i> 录音文件路径</span>
            <el-button
              style="float: right; padding: 3px 0"
              type="text"
              icon="el-icon-refresh"
              @click="fetchRecordingPath"
              :loading="fetchingRecordingPath">
              刷新路径
            </el-button>
          </div>

          <div class="path-container">
            <p v-if="recordingPath"><strong>录音文件路径:</strong></p>
            <p v-if="recordingPath" class="path-text">{{ recordingPath }}</p>
            <p v-else class="no-data">暂无录音文件</p>
          </div>
        </el-card>
      </el-col>

      <!-- 会议纪要区域 -->
      <el-col :span="12">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-document-checked"></i> 会议纪要</span>
            <el-button
              style="float: right; padding: 3px 0"
              type="text"
              icon="el-icon-refresh"
              @click="fetchMinutes"
              :loading="fetchingMinutes">
              刷新纪要
            </el-button>
          </div>

          <div class="minutes-container">
            <el-input
              type="textarea"
              :rows="8"
              placeholder="会议纪要将在此显示..."
              v-model="minutesText"
              readonly>
            </el-input>
            <div style="margin-top: 10px;">
              <el-button
                type="primary"
                size="small"
                icon="el-icon-document-add"
                @click="generateMinutes"
                :disabled="!controlForm.roomId">
                重新生成纪要
              </el-button>
              <el-button
                type="success"
                size="small"
                icon="el-icon-download"
                @click="downloadMinutes"
                :disabled="!minutesText">
                下载纪要
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 自动刷新定时器 -->
    <el-dialog title="自动刷新设置" :visible.sync="refreshDialogVisible">
      <el-form :model="refreshSettings">
        <el-form-item label="刷新间隔(秒)">
          <el-slider
            v-model="refreshSettings.interval"
            :min="5"
            :max="60"
            show-input>
          </el-slider>
        </el-form-item>
        <el-form-item label="启用自动刷新">
          <el-switch v-model="refreshSettings.enabled"></el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="refreshDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRefreshSettings">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  healthCheck,
  startAsrRecording,
  startFullRecording,
  stopAsrRecording,
  stopFullRecording,
  getRecordingPath,
  getTranscript,
  generateMinutes,
  getMinutes,
  getStatus
} from "@/api/huiyi/aiService";

export default {
  name: "AIService",
  data() {
    return {
      // 健康状态
      healthStatus: null,
      healthStatusTag: 'info',
      healthStatusText: '未知',
      checkingHealth: false,
      serviceStatus: null,

      // 控制表单
      controlForm: {
        roomId: '',
        rtspUrl: ''
      },

      // 录音状态
      asrRecordingStatus: '', // 'recording' 或空
      fullRecordingStatus: '', // 'recording' 或空
      startingAsrRecording: false,
      stoppingAsrRecording: false,
      startingFullRecording: false,
      stoppingFullRecording: false,

      // 转录文本
      transcriptText: '',
      fetchingTranscript: false,

      // 录音路径
      recordingPath: '',
      fetchingRecordingPath: false,

      // 会议纪要
      minutesText: '',
      fetchingMinutes: false,

      // 自动刷新设置
      refreshDialogVisible: false,
      refreshSettings: {
        interval: 10,
        enabled: false
      },
      refreshTimer: null
    };
  },

  created() {
    // 页面加载时检查一次健康状态
    this.checkHealth();

    // 设置自动刷新
    this.setupAutoRefresh();
  },

  destroyed() {
    // 清理定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  },

  methods: {
    // 检查健康状态
    async checkHealth() {
      this.checkingHealth = true;
      try {
        const response = await healthCheck();
        this.healthStatus = response.data;

        if (response.code === 200) {
          this.healthStatusTag = 'success';
          this.healthStatusText = '服务正常';
        } else {
          this.healthStatusTag = 'warning';
          this.healthStatusText = '服务异常';
        }

        // 同时获取服务状态
        this.fetchStatus();
      } catch (error) {
        console.error('健康检查失败:', error);
        this.healthStatusTag = 'danger';
        this.healthStatusText = '服务不可达';
      } finally {
        this.checkingHealth = false;
      }
    },

    // 获取服务状态
    async fetchStatus() {
      try {
        const response = await getStatus();
        if (response.code === 200) {
          this.serviceStatus = response.data;

          // 更新录音状态
          this.asrRecordingStatus = this.serviceStatus.asr_status.is_running ? 'recording' : '';
        }
      } catch (error) {
        console.error('获取服务状态失败:', error);
      }
    },

    // 开始ASR录音
    async startAsrRecording() {
      if (!this.controlForm.roomId || !this.controlForm.rtspUrl) {
        this.$message.warning('请填写房间ID和RTSP地址');
        return;
      }

      this.startingAsrRecording = true;
      try {
        const response = await startAsrRecording({
          roomId: this.controlForm.roomId,
          rtspUrl: this.controlForm.rtspUrl
        });

        if (response.code === 200) {
          this.$message.success('ASR录音已开始');
          this.asrRecordingStatus = 'recording';
          // 刷新状态
          setTimeout(() => {
            this.checkHealth();
          }, 1000);
        } else {
          this.$message.error(response.msg || '开始ASR录音失败');
        }
      } catch (error) {
        console.error('开始ASR录音失败:', error);
        this.$message.error('开始ASR录音失败: ' + (error.message || '网络错误'));
      } finally {
        this.startingAsrRecording = false;
      }
    },

    // 停止ASR录音
    async stopAsrRecording() {
      if (!this.controlForm.roomId) {
        this.$message.warning('请填写房间ID');
        return;
      }

      this.stoppingAsrRecording = true;
      try {
        const response = await stopAsrRecording(this.controlForm.roomId);

        if (response.code === 200) {
          this.$message.success('ASR录音已停止');
          this.asrRecordingStatus = '';
          // 刷新状态
          setTimeout(() => {
            this.checkHealth();
          }, 1000);
        } else {
          this.$message.error(response.msg || '停止ASR录音失败');
        }
      } catch (error) {
        console.error('停止ASR录音失败:', error);
        this.$message.error('停止ASR录音失败: ' + (error.message || '网络错误'));
      } finally {
        this.stoppingAsrRecording = false;
      }
    },

    // 开始完整录音
    async startFullRecording() {
      if (!this.controlForm.roomId || !this.controlForm.rtspUrl) {
        this.$message.warning('请填写房间ID和RTSP地址');
        return;
      }

      this.startingFullRecording = true;
      try {
        const response = await startFullRecording({
          roomId: this.controlForm.roomId,
          rtspUrl: this.controlForm.rtspUrl
        });

        if (response.code === 200) {
          this.$message.success('完整录音已开始');
          this.fullRecordingStatus = 'recording';
        } else {
          this.$message.error(response.msg || '开始完整录音失败');
        }
      } catch (error) {
        console.error('开始完整录音失败:', error);
        this.$message.error('开始完整录音失败: ' + (error.message || '网络错误'));
      } finally {
        this.startingFullRecording = false;
      }
    },

    // 停止完整录音
    async stopFullRecording() {
      this.stoppingFullRecording = true;
      try {
        const response = await stopFullRecording();

        if (response.code === 200) {
          this.$message.success('完整录音已停止');
          this.fullRecordingStatus = '';
        } else {
          this.$message.error(response.msg || '停止完整录音失败');
        }
      } catch (error) {
        console.error('停止完整录音失败:', error);
        this.$message.error('停止完整录音失败: ' + (error.message || '网络错误'));
      } finally {
        this.stoppingFullRecording = false;
      }
    },

    // 获取转录文本
    async fetchTranscript() {
      if (!this.controlForm.roomId) {
        this.$message.warning('请填写房间ID');
        return;
      }

      this.fetchingTranscript = true;
      try {
        const response = await getTranscript(this.controlForm.roomId);

        if (response.code === 200) {
          if (response.data && response.data.transcript) {
            // 将转录数组转换为文本
            if (Array.isArray(response.data.transcript)) {
              this.transcriptText = response.data.transcript.join('\n');
            } else {
              this.transcriptText = response.data.transcript;
            }
            this.$message.success(`获取到 ${response.data.count || 0} 条转录`);
          } else {
            this.transcriptText = '';
            this.$message.info('暂无转录文本');
          }
        } else {
          this.$message.error(response.msg || '获取转录文本失败');
        }
      } catch (error) {
        console.error('获取转录文本失败:', error);
        this.$message.error('获取转录文本失败: ' + (error.message || '网络错误'));
      } finally {
        this.fetchingTranscript = false;
      }
    },

    // 获取录音路径
    async fetchRecordingPath() {
      if (!this.controlForm.roomId) {
        this.$message.warning('请填写房间ID');
        return;
      }

      this.fetchingRecordingPath = true;
      try {
        const response = await getRecordingPath(this.controlForm.roomId);

        if (response.code === 200) {
          if (response.data && response.data.recording_path) {
            this.recordingPath = response.data.recording_path;
            this.$message.success('录音路径获取成功');
          } else {
            this.recordingPath = '';
            this.$message.info('暂无录音文件');
          }
        } else {
          this.$message.error(response.msg || '获取录音路径失败');
        }
      } catch (error) {
        console.error('获取录音路径失败:', error);
        this.$message.error('获取录音路径失败: ' + (error.message || '网络错误'));
      } finally {
        this.fetchingRecordingPath = false;
      }
    },

    // 获取会议纪要
    async fetchMinutes() {
      if (!this.controlForm.roomId) {
        this.$message.warning('请填写房间ID');
        return;
      }

      this.fetchingMinutes = true;
      try {
        const response = await getMinutes(this.controlForm.roomId);

        if (response.code === 200) {
          if (response.data && response.data.minutes) {
            this.minutesText = response.data.minutes;
            this.$message.success('会议纪要获取成功');
          } else {
            this.minutesText = '';
            this.$message.info('暂无会议纪要');
          }
        } else {
          this.$message.error(response.msg || '获取会议纪要失败');
        }
      } catch (error) {
        console.error('获取会议纪要失败:', error);
        this.$message.error('获取会议纪要失败: ' + (error.message || '网络错误'));
      } finally {
        this.fetchingMinutes = false;
      }
    },

    // 生成会议纪要
    async generateMinutes() {
      if (!this.controlForm.roomId) {
        this.$message.warning('请填写房间ID');
        return;
      }

      try {
        const response = await generateMinutes({
          roomId: this.controlForm.roomId,
          transcript: this.transcriptText
        });

        if (response.code === 200) {
          if (response.data && response.data.minutes) {
            this.minutesText = response.data.minutes;
            this.$message.success('会议纪要生成成功');
          } else {
            this.$message.warning('会议纪要生成中，请稍后再试');
          }
        } else {
          this.$message.error(response.msg || '生成会议纪要失败');
        }
      } catch (error) {
        console.error('生成会议纪要失败:', error);
        this.$message.error('生成会议纪要失败: ' + (error.message || '网络错误'));
      }
    },

    // 从当前转录文本生成会议纪要
    async generateMinutesFromTranscript() {
      if (!this.controlForm.roomId) {
        this.$message.warning('请填写房间ID');
        return;
      }

      try {
        const response = await generateMinutes({
          roomId: this.controlForm.roomId,
          transcript: this.transcriptText
        });

        if (response.code === 200) {
          if (response.data && response.data.minutes) {
            this.minutesText = response.data.minutes;
            this.$message.success('会议纪要生成成功');
          } else {
            this.$message.warning('会议纪要生成中，请稍后再试');
          }
        } else {
          this.$message.error(response.msg || '生成会议纪要失败');
        }
      } catch (error) {
        console.error('生成会议纪要失败:', error);
        this.$message.error('生成会议纪要失败: ' + (error.message || '网络错误'));
      }
    },

    // 下载转录文本
    downloadTranscript() {
      if (!this.transcriptText) {
        this.$message.warning('没有可下载的转录文本');
        return;
      }

      const blob = new Blob([this.transcriptText], { type: 'text/plain;charset=utf-8' });
      const filename = `transcript_${this.controlForm.roomId}_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.txt`;
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = filename;
      link.click();
    },

    // 下载会议纪要
    downloadMinutes() {
      if (!this.minutesText) {
        this.$message.warning('没有可下载的会议纪要');
        return;
      }

      const blob = new Blob([this.minutesText], { type: 'text/plain;charset=utf-8' });
      const filename = `minutes_${this.controlForm.roomId}_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.txt`;
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = filename;
      link.click();
    },

    // 设置自动刷新
    setupAutoRefresh() {
      if (this.refreshSettings.enabled && this.refreshSettings.interval > 0) {
        this.refreshTimer = setInterval(() => {
          // 刷新转录文本
          if (this.controlForm.roomId) {
            this.fetchTranscript();
            this.fetchMinutes();
            this.fetchRecordingPath();
          }
        }, this.refreshSettings.interval * 1000);
      }
    },

    // 保存刷新设置
    saveRefreshSettings() {
      this.refreshDialogVisible = false;

      // 清除旧的定时器
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }

      // 根据设置重新设置定时器
      this.setupAutoRefresh();
    }
  }
};
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 8px;
}

.box-card {
  width: 100%;
}

.info-section h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #606266;
}

.status-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.status-details p {
  margin: 5px 0;
  font-size: 14px;
  color: #606266;
}

.control-panel {
  padding: 10px 0;
}

.transcript-container,
.minutes-container {
  width: 100%;
}

.path-container {
  padding: 10px 0;
}

.path-text {
  word-break: break-all;
  color: #409EFF;
  font-family: monospace;
}

.no-data {
  color: #909399;
  font-style: italic;
}

.el-button-group {
  margin-right: 10px;
}
</style>