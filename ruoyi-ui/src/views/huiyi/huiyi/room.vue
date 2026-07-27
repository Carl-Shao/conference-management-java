<template>
  <div class="meeting-room">
    <!-- 顶部信息栏 -->
    <div class="room-header">
      <div class="header-left">
        <el-button icon="el-icon-arrow-left" circle size="small" @click="handleBack" />
        <div class="meeting-info">
          <div class="meeting-title">{{ meeting.title || '会议进行中' }}</div>
          <div class="meeting-sub">{{ meeting.roomName }}</div>
        </div>
      </div>
      <div class="header-center">
        <span class="rec-dot" :class="{ 'rec-paused': recordStatus === 'paused' }"></span>
        <span class="timer">{{ formattedTimer }}</span>
        <span class="rec-status-text">{{ recordStatus === 'paused' ? '已暂停' : '录制中' }}</span>
      </div>
      <div class="header-right">
        <el-tag v-if="wsConnected" type="success" size="small" effect="plain">已连接</el-tag>
        <el-tag v-else type="info" size="small" effect="plain">连接中...</el-tag>
      </div>
    </div>

    <!-- 主体：左转写 右笔记 -->
    <div class="room-body">
      <div class="transcript-panel">
        <div class="panel-title">
          <i class="el-icon-mic"></i> 实时转写
        </div>
        <div class="transcript-scroll" ref="transcriptScroll">
          <div v-if="transcripts.length === 0" class="transcript-empty">
            等待发言中，转写内容将实时显示在这里...
          </div>
          <div
            v-for="line in transcripts"
            :key="line.id"
            class="transcript-line"
            :class="{ marked: markedIds.has(line.id) }"
          >
            <div class="line-meta">
              <span class="speaker">{{ line.speaker || '发言人' }}</span>
              <span class="line-time">{{ formatOffset(line.startMs) }}</span>
            </div>
            <div class="line-text">{{ line.text }}</div>
          </div>
        </div>
      </div>

      <div class="note-panel">
        <div class="panel-title">
          <i class="el-icon-edit-outline"></i> 我的笔记
          <span class="save-hint">{{ saveHint }}</span>
        </div>
        <el-input
          type="textarea"
          v-model="noteContent"
          class="note-textarea"
          placeholder="在此记录会议要点、待办事项……"
          :autosize="false"
          resize="none"
          @input="onNoteInput"
        />
      </div>
    </div>

    <!-- 底部控制栏 -->
    <div class="room-controls">
      <div class="control-btn" @click="handleMark">
        <i class="el-icon-collection-tag"></i>
        <span>标记</span>
      </div>

      <div class="control-btn control-btn-main" @click="handleToggleRecord">
        <i :class="recordStatus === 'paused' ? 'el-icon-video-play' : 'el-icon-video-pause'"></i>
        <span>{{ recordStatus === 'paused' ? '继续' : '暂停' }}</span>
      </div>

      <div class="control-btn control-btn-danger" @click="handleEnd">
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
  name: 'MeetingRoom',
  data() {
    return {
      meetingId: null,
      meeting: {},
      recordStatus: 'ongoing', // ongoing | paused
      elapsedSeconds: 0,
      timerHandle: null,

      transcripts: [],
      markedIds: new Set(),
      lastTranscriptId: 0,
      pollHandle: null,

      noteContent: '',
      saveHint: '',
      noteSaveTimer: null,

      ws: null,
      wsConnected: false,
      audioCtx: null,
      audioStream: null,
      audioProcessor: null
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
  beforeDestroy() {
    this.cleanup()
  },
  methods: {
    initMeeting() {
      getMeeting(this.meetingId).then(res => {
        this.meeting = res.data || {}
        this.recordStatus = this.meeting.status === 'paused' ? 'paused' : 'ongoing'
        this.elapsedSeconds = this.meeting.elapsedSeconds || 0
        this.startTimer()
      })
      getNote(this.meetingId).then(res => {
        this.noteContent = (res.data && res.data.content) || ''
      })
      this.fetchTranscript()
      this.startPolling()
      this.connectWebSocket()
      this.startAudioCapture()
    },

    // ---------- 计时器 ----------
    startTimer() {
      this.timerHandle = setInterval(() => {
        if (this.recordStatus === 'ongoing') {
          this.elapsedSeconds++
        }
      }, 1000)
    },

    // ---------- 转写：轮询兜底 + WebSocket 实时推送 ----------
    startPolling() {
      // 作为 WebSocket 断线时的兜底方案，定期增量拉取一次
      this.pollHandle = setInterval(() => {
        if (!this.wsConnected) {
          this.fetchTranscript()
        }
      }, 5000)
    },
    fetchTranscript() {
      getTranscript(this.meetingId, this.lastTranscriptId).then(res => {
        const list = res.rows || res.data || []
        if (list.length) {
          this.appendTranscripts(list)
        }
      })
    },
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
          // 忽略非 JSON 消息
        }
      }
    },

    // ---------- 音频采集：麦克风 PCM16 -> WebSocket 二进制帧 ----------
    async startAudioCapture() {
      try {
        this.audioStream = await navigator.mediaDevices.getUserMedia({ audio: true })
        this.audioCtx = new (window.AudioContext || window.webkitAudioContext)()
        const source = this.audioCtx.createMediaStreamSource(this.audioStream)
        this.audioProcessor = this.audioCtx.createScriptProcessor(4096, 1, 1)
        source.connect(this.audioProcessor)
        this.audioProcessor.connect(this.audioCtx.destination)

        this.audioProcessor.onaudioprocess = e => {
          if (this.recordStatus !== 'ongoing') return
          if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
          const input = e.inputBuffer.getChannelData(0)
          const pcm16 = this.floatTo16BitPCM(input)
          this.ws.send(pcm16)
        }
      } catch (err) {
        this.$modal.msgWarning('无法访问麦克风，实时转写将不可用')
      }
    },
    floatTo16BitPCM(float32Array) {
      const buffer = new ArrayBuffer(float32Array.length * 2)
      const view = new DataView(buffer)
      for (let i = 0; i < float32Array.length; i++) {
        let s = Math.max(-1, Math.min(1, float32Array[i]))
        s = s < 0 ? s * 0x8000 : s * 0x7fff
        view.setInt16(i * 2, s, true)
      }
      return buffer
    },

    // ---------- 笔记：输入防抖自动保存 ----------
    onNoteInput() {
      this.saveHint = '编辑中...'
      clearTimeout(this.noteSaveTimer)
      this.noteSaveTimer = setTimeout(() => {
        saveNote(this.meetingId, { content: this.noteContent }).then(() => {
          this.saveHint = '已自动保存'
          setTimeout(() => { this.saveHint = '' }, 2000)
        })
      }, 1000)
    },

    // ---------- 控制按钮 ----------
    formatOffset(ms) {
      if (!ms && ms !== 0) return ''
      const totalSec = Math.floor(ms / 1000)
      const m = String(Math.floor(totalSec / 60)).padStart(2, '0')
      const s = String(totalSec % 60).padStart(2, '0')
      return `${m}:${s}`
    },
    handleToggleRecord() {
      if (this.recordStatus === 'ongoing') {
        pauseMeeting(this.meetingId).then(() => {
          this.recordStatus = 'paused'
          this.$modal.msgSuccess('已暂停会议')
        })
      } else {
        resumeMeeting(this.meetingId).then(() => {
          this.recordStatus = 'ongoing'
          this.$modal.msgSuccess('已继续会议')
        })
      }
    },
    handleMark() {
      const pointMs = this.elapsedSeconds * 1000
      markMeeting(this.meetingId, { pointMs }).then(res => {
        this.$modal.msgSuccess('已在 ' + this.formattedTimer + ' 处添加标记')
        // 若返回了关联的转写记录 id，则高亮对应行
        if (res.data && res.data.transcriptId) {
          this.markedIds.add(res.data.transcriptId)
        }
      })
    },
    handleEnd() {
      this.$modal.confirm('确认结束当前会议吗？结束后系统将自动生成 AI 会议纪要。').then(() => {
        return endMeeting(this.meetingId)
      }).then(() => {
        this.$modal.msgSuccess('会议已结束，正在生成会议纪要')
        this.cleanup()
        this.$router.replace({ path: '/huiyi/meeting/detail/' + this.meetingId })
      }).catch(() => {})
    },
    handleBack() {
      this.$modal.confirm('返回首页不会结束会议，会议将继续在后台录制，确认返回吗？').then(() => {
        this.$router.push({ path: '/huiyi/meeting' })
      }).catch(() => {})
    },

    cleanup() {
      clearInterval(this.timerHandle)
      clearInterval(this.pollHandle)
      clearTimeout(this.noteSaveTimer)
      if (this.ws) {
        this.ws.onmessage = null
        this.ws.close()
      }
      if (this.audioProcessor) this.audioProcessor.disconnect()
      if (this.audioStream) this.audioStream.getTracks().forEach(t => t.stop())
      if (this.audioCtx) this.audioCtx.close()
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-room {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}

.room-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    .meeting-info {
      margin-left: 12px;
      .meeting-title {
        font-size: 15px;
        font-weight: 600;
        color: #1d2129;
      }
      .meeting-sub {
        font-size: 12px;
        color: #86909c;
      }
    }
  }

  .header-center {
    display: flex;
    align-items: center;
    .rec-dot {
      width: 9px;
      height: 9px;
      border-radius: 50%;
      background: #f5222d;
      margin-right: 8px;
      animation: blink 1.2s infinite;
    }
    .rec-paused {
      background: #faad14;
      animation: none;
    }
    .timer {
      font-family: 'Courier New', monospace;
      font-size: 16px;
      font-weight: 600;
      color: #1d2129;
      margin-right: 8px;
    }
    .rec-status-text {
      font-size: 12px;
      color: #86909c;
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.room-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  i { margin-right: 6px; color: #1890ff; }
  .save-hint {
    margin-left: auto;
    font-size: 12px;
    font-weight: 400;
    color: #86909c;
  }
}

.transcript-panel {
  flex: 1.6;
  background: #fff;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.transcript-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
}

.transcript-empty {
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  margin-top: 60px;
}

.transcript-line {
  margin-bottom: 16px;
  padding: 8px 10px;
  border-radius: 6px;

  &.marked {
    background: #fffbe6;
    box-shadow: inset 3px 0 0 #faad14;
  }

  .line-meta {
    display: flex;
    align-items: center;
    margin-bottom: 4px;
    .speaker {
      font-size: 12px;
      font-weight: 600;
      color: #1890ff;
      margin-right: 8px;
    }
    .line-time {
      font-size: 11px;
      color: #c0c4cc;
    }
  }
  .line-text {
    font-size: 14px;
    color: #1d2129;
    line-height: 1.6;
  }
}

.note-panel {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .note-textarea {
    flex: 1;
    padding: 12px 18px 18px;
    display: flex;

    ::v-deep .el-textarea__inner {
      flex: 1;
      height: 100% !important;
      border: none;
      resize: none;
      font-size: 14px;
      line-height: 1.7;
      padding: 0;
    }
  }
}

.room-controls {
  flex-shrink: 0;
  height: 88px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;

  .control-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #4e5969;
    width: 64px;

    i {
      font-size: 22px;
      width: 48px;
      height: 48px;
      line-height: 48px;
      border-radius: 50%;
      background: #f2f3f5;
      margin-bottom: 4px;
      transition: background 0.2s;
    }
    span {
      font-size: 12px;
    }
    &:hover i {
      background: #e8f4ff;
      color: #1890ff;
    }
  }

  .control-btn-main {
    i {
      width: 60px;
      height: 60px;
      line-height: 60px;
      font-size: 26px;
      background: #1890ff;
      color: #fff;
    }
    &:hover i {
      background: #40a9ff;
      color: #fff;
    }
  }

  .control-btn-danger {
    &:hover i {
      background: #fff1f0;
      color: #f5222d;
    }
  }
}
</style>