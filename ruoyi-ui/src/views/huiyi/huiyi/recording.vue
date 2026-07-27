<template>
  <div class="recording-page">
    <!-- 顶部信息栏 -->
    <div class="recording-header">
      <div class="header-left">
        <el-button icon="el-icon-arrow-left" circle size="small" @click="handleBack" />
        <div class="meeting-info">
          <div class="meeting-title">{{ meeting.title || '会议进行中' }}</div>
          <div class="meeting-meta">
            <span class="timer">{{ formattedTimer }}</span>
            <span class="rec-status" :class="{ 'rec-paused': recordStatus === 'paused' }">
              <span class="rec-dot" :class="{ 'rec-paused': recordStatus === 'paused' }"></span>
              {{ recordStatus === 'paused' ? '已暂停' : '录制中' }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-tag v-if="wsConnected" type="success" size="small" effect="plain">实时转写中</el-tag>
        <el-tag v-else type="info" size="small" effect="plain">连接中...</el-tag>
      </div>
    </div>

    <!-- 音量可视化区域 -->
    <div class="audio-visualizer">
      <div class="wave-container">
        <div class="wave-bar" v-for="n in 30" :key="n" :style="{ height: waveHeights[n-1] + 'px' }"></div>
      </div>
      <div class="visualizer-label">麦克风声音</div>
    </div>

    <!-- 主体内容：左右分栏 -->
    <div class="recording-body">
      <!-- 左侧：实时转写 -->
      <div class="transcript-panel">
        <div class="panel-header">
          <i class="el-icon-microphone"></i>
          <span class="panel-title">实时转写</span>
        </div>
        <div class="transcript-content" ref="transcriptScroll">
          <div v-if="transcripts.length === 0" class="empty-transcript">
            <i class="el-icon-microphone-off"></i>
            <p>等待发言中，转写内容将实时显示</p>
          </div>
          <div
            v-for="line in transcripts"
            :key="line.id"
            class="transcript-item"
            :class="{ marked: markedIds.has(line.id) }"
          >
            <div class="speaker-info">
              <span class="speaker-name">{{ line.speaker || '发言人' }}</span>
              <span class="timestamp">{{ formatOffset(line.startMs) }}</span>
            </div>
            <div class="transcript-text">{{ line.text }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧：笔记 -->
      <div class="notes-panel">
        <div class="panel-header">
          <i class="el-icon-edit"></i>
          <span class="panel-title">我的笔记</span>
          <span class="save-status">{{ saveStatus }}</span>
        </div>
        <div class="notes-content">
          <el-input
            type="textarea"
            v-model="notesContent"
            class="notes-textarea"
            :rows="20"
            placeholder="记录会议要点、待办事项..."
            @input="debouncedSaveNotes"
          />
        </div>
      </div>
    </div>

    <!-- 底部控制栏 -->
    <div class="controls-bar">
      <!-- 标记按钮 -->
      <div class="control-item mark-btn" @click="handleMark">
        <i class="el-icon-bookmark"></i>
        <span>标记</span>
      </div>

      <!-- 暂停/继续按钮 -->
      <div class="control-item main-control" @click="handleToggleRecord">
        <i
          :class="recordStatus === 'paused' ? 'el-icon-video-play' : 'el-icon-pause'"
          :style="{ fontSize: '24px' }"
        ></i>
        <span>{{ recordStatus === 'paused' ? '继续' : '暂停' }}</span>
      </div>

      <!-- 结束会议按钮 -->
      <div class="control-item end-btn" @click="handleEnd">
        <i class="el-icon-switch-button"></i>
        <span>结束会议</span>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getMeeting,
  getTranscript,
  pauseMeeting,
  resumeMeeting,
  endMeeting,
  markMeeting,
  saveNote,
  getNote,
  buildMeetingWsUrl
} from '@/api/huiyi/meeting'

export default {
  name: 'RecordingPage',
  data() {
    return {
      meetingId: null,
      meeting: {},
      recordStatus: 'ongoing', // ongoing | paused
      elapsedSeconds: 0,
      timerInterval: null,

      // 音频可视化
      waveHeights: Array(30).fill(4), // 初始高度数组
      waveAnimation: null,

      // 转写数据
      transcripts: [],
      markedIds: new Set(),
      lastTranscriptId: 0,
      transcriptPoll: null,

      // 笔记数据
      notesContent: '',
      saveStatus: '',
      saveTimeout: null,

      // WebSocket 和音频
      ws: null,
      wsConnected: false,
      audioContext: null,
      mediaStream: null,
      analyser: null,
      animationFrame: null
    }
  },
  computed: {
    formattedTimer() {
      const h = String(Math.floor(this.elapsedSeconds / 3600)).padStart(2, '0')
      const m = String(Math.floor((this.elapsedSeconds % 3600) / 60)).padStart(2, '0')
      const s = String(this.elapsedSeconds % 60).padStart(2, '0')
      return `${h}:${m}:${s}`
    }
  },
  created() {
    this.meetingId = this.$route.params.meetingId
    this.initMeeting()
  },
  mounted() {
    this.animateWave()
  },
  beforeDestroy() {
    this.cleanup()
  },
  methods: {
    initMeeting() {
      // 获取会议信息
      getMeeting(this.meetingId).then(res => {
        this.meeting = res.data || {}
        this.recordStatus = this.meeting.status === 'paused' ? 'paused' : 'ongoing'
        this.elapsedSeconds = this.meeting.elapsedSeconds || 0
        this.startTimer()
      })

      // 获取笔记
      getNote(this.meetingId).then(res => {
        this.notesContent = (res.data && res.data.content) || ''
      })

      // 初始化转写
      this.fetchTranscript()
      this.startTranscriptPolling()

      // 连接WebSocket
      this.connectWebSocket()

      // 启动音频采集
      this.startAudioCapture()
    },

    // 启动计时器
    startTimer() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval)
      }
      this.timerInterval = setInterval(() => {
        if (this.recordStatus === 'ongoing') {
          this.elapsedSeconds++
        }
      }, 1000)
    },

    // 获取转写内容
    fetchTranscript() {
      getTranscript(this.meetingId, this.lastTranscriptId).then(res => {
        const list = res.rows || res.data || []
        if (list.length) {
          this.appendTranscripts(list)
        }
      })
    },

    // 添加转写内容
    appendTranscripts(list) {
      list.forEach(item => {
        this.transcripts.push(item)
        this.lastTranscriptId = Math.max(this.lastTranscriptId, item.id)
      })
      this.$nextTick(() => {
        const el = this.$refs.transcriptScroll
        if (el) el.scrollTop = el.scrollHeight
      })
    },

    // 开始转写轮询
    startTranscriptPolling() {
      this.transcriptPoll = setInterval(() => {
        if (!this.wsConnected) {
          this.fetchTranscript()
        }
      }, 5000)
    },

    // 连接WebSocket
    connectWebSocket() {
      const url = buildMeetingWsUrl(this.meetingId)
      this.ws = new WebSocket(url)
      this.ws.binaryType = 'arraybuffer'

      this.ws.onopen = () => {
        this.wsConnected = true
      }

      this.ws.onclose = () => {
        this.wsConnected = false
      }

      this.ws.onerror = () => {
        this.wsConnected = false
      }

      this.ws.onmessage = evt => {
        if (typeof evt.data !== 'string') return
        try {
          const msg = JSON.parse(evt.data)
          if (msg.type === 'transcript') {
            this.appendTranscripts([msg])
          }
        } catch (e) {
          console.error('解析转写消息失败:', e)
        }
      }
    },

    // 启动音频采集
    async startAudioCapture() {
      try {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })

        // 创建音频上下文和分析器
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
        const source = this.audioContext.createMediaStreamSource(this.mediaStream)
        this.analyser = this.audioContext.createAnalyser()
        this.analyser.fftSize = 256

        source.connect(this.analyser)

        // 开始音频可视化动画
        this.updateAudioVisualizer()
      } catch (err) {
        console.error('无法访问麦克风:', err)
        this.$message.warning('无法访问麦克风，音量可视化将不可用')
      }
    },

    // 更新音频可视化
    updateAudioVisualizer() {
      if (!this.analyser) return

      const dataArray = new Uint8Array(this.analyser.frequencyBinCount)
      const bufferLength = dataArray.length

      const update = () => {
        if (this.recordStatus === 'paused') {
          // 暂停时显示低幅度波动
          this.waveHeights = this.waveHeights.map(() => Math.random() * 2 + 2)
        } else {
          this.analyser.getByteFrequencyData(dataArray)

          // 将音频数据映射到波形高度
          for (let i = 0; i < 30; i++) {
            const value = dataArray[Math.floor(i * bufferLength / 30)] || 0
            const height = Math.max(4, Math.min(40, value / 3 + 4)) // 限制在4-40px之间
            this.$set(this.waveHeights, i, height)
          }
        }

        this.animationFrame = requestAnimationFrame(update)
      }

      update()
    },

    // 波形动画效果
    animateWave() {
      // 定期调整波形以保持动态效果
      setInterval(() => {
        if (this.recordStatus === 'paused') {
          // 暂停状态时轻微变化
          this.waveHeights = this.waveHeights.map(h => h > 4 ? h - 0.5 : h + 0.5)
        }
      }, 100)
    },

    // 格式化时间偏移
    formatOffset(ms) {
      if (!ms && ms !== 0) return ''
      const totalSec = Math.floor(ms / 1000)
      const m = String(Math.floor(totalSec / 60)).padStart(2, '0')
      const s = String(totalSec % 60).padStart(2, '0')
      return `${m}:${s}`
    },

    // 暂停/继续会议
    handleToggleRecord() {
      if (this.recordStatus === 'ongoing') {
        pauseMeeting(this.meetingId).then(() => {
          this.recordStatus = 'paused'
          this.$message.success('会议已暂停')
        }).catch(err => {
          console.error('暂停会议失败:', err)
          this.$message.error('暂停会议失败')
        })
      } else {
        resumeMeeting(this.meetingId).then(() => {
          this.recordStatus = 'ongoing'
          this.$message.success('会议已继续')
        }).catch(err => {
          console.error('继续会议失败:', err)
          this.$message.error('继续会议失败')
        })
      }
    },

    // 添加标记
    handleMark() {
      const pointMs = this.elapsedSeconds * 1000
      markMeeting(this.meetingId, { pointMs }).then(res => {
        this.$message.success(`已在 ${this.formattedTimer} 处添加标记`)

        // 如果返回了相关的转写记录ID，则标记该行
        if (res.data && res.data.transcriptId) {
          this.markedIds.add(res.data.transcriptId)
        }
      }).catch(err => {
        console.error('添加标记失败:', err)
        this.$message.error('添加标记失败')
      })
    },

    // 结束会议
    handleEnd() {
      this.$confirm('确认结束当前会议吗？结束后系统将自动生成 AI 会议纪要。', '结束会议', {
        confirmButtonText: '确认结束',
        cancelButtonText: '继续录制',
        type: 'warning'
      }).then(() => {
        return endMeeting(this.meetingId)
      }).then(() => {
        this.$message.success('会议已结束，正在生成会议纪要')
        this.cleanup()
        this.$router.replace({ path: '/huiyi/meeting/detail/' + this.meetingId })
      }).catch(() => {
        // 用户取消
      })
    },

    // 返回上一页
    handleBack() {
      this.$confirm('返回首页不会结束会议，会议将继续在后台录制，确认返回吗？', '返回首页', {
        confirmButtonText: '确认返回',
        cancelButtonText: '继续录制',
        type: 'info'
      }).then(() => {
        this.$router.push({ path: '/huiyi/meeting' })
      }).catch(() => {
        // 用户取消
      })
    },

    // 防抖保存笔记
    debouncedSaveNotes() {
      this.saveStatus = '编辑中...'

      if (this.saveTimeout) {
        clearTimeout(this.saveTimeout)
      }

      this.saveTimeout = setTimeout(() => {
        this.saveNotes()
      }, 1000)
    },

    // 保存笔记
    async saveNotes() {
      try {
        await saveNote(this.meetingId, { content: this.notesContent })
        this.saveStatus = '已保存'
        setTimeout(() => {
          this.saveStatus = ''
        }, 2000)
      } catch (error) {
        console.error('保存笔记失败:', error)
        this.saveStatus = '保存失败'
      }
    },

    // 清理资源
    cleanup() {
      if (this.timerInterval) clearInterval(this.timerInterval)
      if (this.transcriptPoll) clearInterval(this.transcriptPoll)
      if (this.saveTimeout) clearTimeout(this.saveTimeout)
      if (this.animationFrame) cancelAnimationFrame(this.animationFrame)

      if (this.ws) {
        this.ws.onmessage = null
        this.ws.close()
      }

      if (this.mediaStream) {
        this.mediaStream.getTracks().forEach(track => track.stop())
      }

      if (this.audioContext) {
        this.audioContext.close()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.recording-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafd;
}

.recording-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 10;

  .header-left {
    display: flex;
    align-items: center;

    .meeting-info {
      margin-left: 16px;

      .meeting-title {
        font-size: 16px;
        font-weight: 600;
        color: #1d2129;
        margin-bottom: 4px;
      }

      .meeting-meta {
        display: flex;
        align-items: center;
        gap: 16px;

        .timer {
          font-family: 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
          font-size: 14px;
          font-weight: 600;
          color: #1d2129;
        }

        .rec-status {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: #52c41a;

          &.rec-paused {
            color: #faad14;
          }
        }

        .rec-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #52c41a;
          animation: blink 1.2s infinite;

          &.rec-paused {
            background: #faad14;
            animation: none;
          }
        }
      }
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.audio-visualizer {
  padding: 24px 24px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;

  .wave-container {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 2px;
    height: 50px;
    margin-bottom: 8px;

    .wave-bar {
      width: 4px;
      background: linear-gradient(to top, #1890ff, #40a9ff);
      border-radius: 2px;
      transition: height 0.1s ease;
      min-height: 4px;
    }
  }

  .visualizer-label {
    text-align: center;
    font-size: 12px;
    color: #86909c;
  }
}

.recording-body {
  flex: 1;
  display: flex;
  padding: 16px 16px 0;
  gap: 16px;
  overflow: hidden;

  .transcript-panel, .notes-panel {
    flex: 1;
    background: #fff;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

    .panel-header {
      display: flex;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid #f0f0f0;

      i {
        font-size: 16px;
        color: #1890ff;
        margin-right: 8px;
      }

      .panel-title {
        font-size: 15px;
        font-weight: 600;
        color: #1d2129;
      }

      .save-status {
        margin-left: auto;
        font-size: 12px;
        color: #86909c;
      }
    }
  }

  .transcript-panel {
    .transcript-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px 20px;

      .empty-transcript {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: #c0c4cc;

        i {
          font-size: 48px;
          margin-bottom: 16px;
        }

        p {
          font-size: 14px;
        }
      }

      .transcript-item {
        padding: 12px 0;
        border-bottom: 1px solid #f5f5f5;

        &.marked {
          background: #f0f9ff;
          border-left: 3px solid #1890ff;
          padding-left: 16px;
        }

        .speaker-info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .speaker-name {
            font-size: 13px;
            font-weight: 600;
            color: #1890ff;
          }

          .timestamp {
            font-size: 12px;
            color: #c0c4cc;
          }
        }

        .transcript-text {
          font-size: 14px;
          line-height: 1.6;
          color: #1d2129;
        }
      }
    }
  }

  .notes-panel {
    .notes-content {
      flex: 1;
      padding: 0 20px 20px;

      .notes-textarea {
        height: 100%;

        ::v-deep textarea {
          resize: none;
          border: none;
          height: 100% !important;
          padding: 0;
          font-size: 14px;
          line-height: 1.6;
        }
      }
    }
  }
}

.controls-bar {
  height: 80px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
  padding: 0 20px;

  .control-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    padding: 8px 12px;
    border-radius: 24px;
    transition: all 0.2s;
    color: #4e5969;

    i {
      font-size: 20px;
      margin-bottom: 4px;
    }

    span {
      font-size: 12px;
    }

    &:hover {
      background: #f0f9ff;
      color: #1890ff;
    }

    &.main-control {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: #1890ff;
      color: #fff;

      i {
        font-size: 28px;
        margin-bottom: 0;
      }

      span {
        display: none;
      }

      &:hover {
        background: #40a9ff;
        color: #fff;
      }
    }

    &.mark-btn:hover {
      background: #fffbe6;
      color: #faad14;
    }

    &.end-btn:hover {
      background: #fff1f0;
      color: #f5222d;
    }
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .recording-body {
    flex-direction: column;
  }

  .controls-bar {
    gap: 30px;
  }
}
</style>