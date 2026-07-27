<template>
  <div class="recording-page">
    <div class="container">
      <!-- 录制状态信息栏 -->
      <div v-if="currentStatus !== NOT_STARTED" class="status-bar">
        <div class="timer">{{ formattedTime }}</div>
        <div class="status-text">{{ statusText }}</div>
      </div>

      <!-- 音量波形可视化区域 -->
      <div v-if="showVisualizer" class="visualizer-container">
        <div class="visualizer" :style="{ height: visualizerHeight + 'px' }">
          <div
            v-for="n in 20"
            :key="n"
            class="bar"
            :style="{
              height: barHeights[n-1] + 'px',
              backgroundColor: barColors[n-1],
              marginLeft: barSpacing + 'px',
              marginRight: barSpacing + 'px'
            }"
          ></div>
        </div>
      </div>

      <!-- 录制控制按钮 -->
      <div class="control-section">
        <div v-if="currentStatus === NOT_STARTED" class="center-start-btn">
          <el-button
            type="primary"
            icon="el-icon-video-play"
            class="start-record-btn"
            :loading="startLoading"
            @click="startRecording"
          >
            开始录制
          </el-button>
        </div>

        <div v-else-if="currentStatus === RECORDING" class="control-buttons">
          <el-button
            type="warning"
            icon="el-icon-video-pause"
            @click="pauseRecording"
          >
            暂停
          </el-button>
          <el-button
            type="danger"
            icon="el-icon-switch-button"
            @click="stopRecording"
          >
            结束
          </el-button>
        </div>

        <div v-else-if="currentStatus === PAUSED" class="control-buttons">
          <el-button
            type="success"
            icon="el-icon-video-play"
            @click="resumeRecording"
          >
            继续
          </el-button>
          <el-button
            type="danger"
            icon="el-icon-switch-button"
            @click="stopRecording"
          >
            结束
          </el-button>
        </div>

        <div v-else-if="currentStatus === PROCESSING" class="processing-state">
          <div class="loading-spinner">
            <i class="el-icon-loading"></i>
          </div>
          <div class="processing-text">正在生成会议纪要...</div>
        </div>
      </div>

      <!-- 实时字幕区域 -->
      <div v-if="showCaptions" class="captions-container">
        <captions-list
          :captions="captions"
          :latest-seq="latestCaptionSeq"
          @clear="clearCaptions"
          @caption-hover="onCaptionHover"
          @caption-leave="onCaptionLeave"
        />
      </div>

      <!-- 会议纪要结果展示 -->
      <div v-if="currentStatus === COMPLETED" class="result-container">
        <meeting-summary
          :summary-data="summaryData"
          :recording-url="audioFilePath"
          @download-summary="downloadSummary"
          @download-recording="downloadAudio"
          @download-transcript="downloadTranscript"
          @download-attachment="downloadAttachment"
          @action-change="onActionChange"
        />
      </div>

      <!-- 权限提示对话框 -->
      <el-dialog
        title="麦克风权限"
        :visible.sync="permissionDialogVisible"
        width="30%"
      >
        <p>需要获取麦克风权限才能开始录制，请允许浏览器访问您的麦克风设备。</p>
        <span slot="footer" class="dialog-footer">
          <el-button @click="permissionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="retryPermission">重试</el-button>
        </span>
      </el-dialog>

      <!-- 网络错误提示 -->
      <el-dialog
        title="网络异常"
        :visible.sync="networkErrorDialogVisible"
        width="30%"
      >
        <p>网络连接异常，请检查网络后重试。</p>
        <span slot="footer" class="dialog-footer">
          <el-button @click="networkErrorDialogVisible = false">确定</el-button>
        </span>
      </el-dialog>

      <!-- 会议选择对话框 -->
      <el-dialog
        title="选择会议"
        :visible.sync="showMeetingSelection"
        width="40%"
        :before-close="handleMeetingSelectionClose"
      >
        <div class="meeting-selection-content">
          <p class="selection-description" v-if="availableMeetings.length > 0">请选择要录制的会议：</p>
          <p class="selection-description" v-else>当前没有可选的会议，请新建会议：</p>
          <el-radio-group v-model="selectedMeetingId" class="meeting-options">
            <el-radio
              v-for="meeting in availableMeetings"
              :key="meeting.id"
              :label="meeting.id"
              class="meeting-option"
            >
              {{ meeting.title }} ({{ meeting.startTime }})
            </el-radio>
            <el-radio :label="'new'" class="meeting-option">
              <i class="el-icon-plus"></i> 新建会议
            </el-radio>
          </el-radio-group>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="cancelMeetingSelection">取消</el-button>
          <el-button type="primary" @click="confirmMeetingSelection" :disabled="!selectedMeetingId">确定</el-button>
        </span>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { startRecord, pauseRecord, resumeRecord, stopRecord, getRecordStatus } from '@/api/huiyi/record'
import { listMeeting } from '@/api/huiyi/meeting'  // 导入会议API
import AudioCapture from '@/utils/huiyi/audioCapture'
import CaptionsList from '@/components/CaptionsList'
import MeetingSummary from '@/components/MeetingSummary'

// 录制状态枚举
const STATUS = {
  NOT_STARTED: 0,    // 未开始
  RECORDING: 1,      // 录制中
  PAUSED: 2,         // 已暂停
  STOP_PENDING: 3, // 已停止待处理
  PROCESSING: 4,     // 处理中
  COMPLETED: 5,      // 已完成
  FAILED: 6           // 异常
}

export default {
  name: 'MeetingRecord',
  components: {
    CaptionsList,
    MeetingSummary
  },
  data() {
    return {
      // 状态枚举
      NOT_STARTED: STATUS.NOT_STARTED,
      RECORDING: STATUS.RECORDING,
      PAUSED: STATUS.PAUSED,
      STOP_PENDING: STATUS.STOP_PENDING,
      PROCESSING: STATUS.PROCESSING,
      COMPLETED: STATUS.COMPLETED,
      FAILED: STATUS.FAILED,

      // 当前状态
      currentStatus: STATUS.NOT_STARTED,

      // 控制加载状态
      startLoading: false,

      // 定时器
      timerInterval: null,
      startTime: 0,
      elapsedTime: 0,

      // 音频采集实例
      audioCapture: null,

      // WebSocket实例
      ws: null,

      // 字幕数据
      captions: [],
      latestCaptionSeq: -1,

      // 状态轮询定时器
      statusPollingTimer: null,

      // 麦克风权限对话框
      permissionDialogVisible: false,

      // 网络错误对话框
      networkErrorDialogVisible: false,

      // WebSocket重连次数
      reconnectAttempts: 0,
      maxReconnectAttempts: 3,

      // 波形可视化数据
      barHeights: Array(20).fill(5),
      barColors: Array(20).fill('#409EFF'),
      barSpacing: 2,
      visualizerHeight: 100,

      // 会议结果数据
      meetingTopic: '',
      fullTranscription: '',
      summaryContent: '',
      audioFilePath: '',

      // 会议纪要数据
      summaryData: {
        meetingInfo: {},
        transcription: '',
        abstract: '',
        actions: [],
        keypoints: [],
        attachments: []
      },

      // 会议选择相关
      showMeetingSelection: false,
      selectedMeetingId: null,
      availableMeetings: [],

      // 折叠面板控制
      activeNames: []
    }
  },

  computed: {
    // 格式化的计时时间
    formattedTime() {
      const totalSeconds = Math.floor(this.elapsedTime / 1000)
      const hours = Math.floor(totalSeconds / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60

      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    },

    // 状态文字
    statusText() {
      const statusMap = {
        [STATUS.RECORDING]: '录制中',
        [STATUS.PAUSED]: '已暂停',
        [STATUS.STOP_PENDING]: '处理中',
        [STATUS.PROCESSING]: '生成纪要中',
        [STATUS.COMPLETED]: '已完成',
        [STATUS.FAILED]: '异常'
      }
      return statusMap[this.currentStatus] || '就绪'
    },

    // 是否显示可视化波形
    showVisualizer() {
      return [STATUS.RECORDING, STATUS.PAUSED].includes(this.currentStatus)
    },

    // 是否显示字幕区域
    showCaptions() {
      return [STATUS.RECORDING, STATUS.PAUSED, STATUS.PROCESSING, STATUS.COMPLETED].includes(this.currentStatus)
    }
  },

  watch: {
    // 监听字幕变化，自动滚动到底部
    captions: {
      handler() {
        this.$nextTick(() => {
          if (this.$refs.captionsList) {
            this.$refs.captionsList.scrollTop = this.$refs.captionsList.scrollHeight
          }
        })
      },
      deep: true
    }
  },

  created() {
    // 初始化前先重置状态
    this.initialize()

    // 初始化音频采集器
    this.audioCapture = new AudioCapture()

    // 获取会议列表并初始化
    this.loadAvailableMeetings()
  },

  mounted() {
    // 开始波形动画
    this.startVisualization()
  },

  beforeDestroy() {
    // 销毁前清理资源
    this.cleanup()
  },

  methods: {
    // 初始化方法
    initialize() {
      // 重置状态
      this.currentStatus = STATUS.NOT_STARTED
      this.captions = []
      this.latestCaptionSeq = -1
      this.meetingId = null
      this.selectedMeetingId = null
      this.availableMeetings = []
      this.elapsedTime = 0
      this.meetingTopic = ''
      this.fullTranscription = ''
      this.summaryContent = ''
      this.audioFilePath = ''

      // 重置会议纪要数据
      this.summaryData = {
        meetingInfo: {},
        transcription: '',
        abstract: '',
        actions: [],
        keypoints: [],
        attachments: []
      }

      // 清理资源
      this.cleanup()
    },

    // 加载可用会议列表
    async loadAvailableMeetings() {
      try {
        // 先尝试获取所有会议
        const response = await listMeeting({
          pageNum: 1,
          pageSize: 100  // 增加页面大小，获取更多结果
        })

        console.log('API Response:', response) // 调试信息

        // 检查返回的数据结构
        if (response && response.rows) {
          this.availableMeetings = response.rows.map(meeting => ({
            id: meeting.id,  // 会议实体的主键ID
            title: meeting.title || meeting.meetingName || '未命名会议',
            startTime: meeting.meetingDate ? `${meeting.meetingDate} ${meeting.startTime || ''}` : (meeting.createTime || '未知时间')
          }))

          console.log('Mapped meetings:', this.availableMeetings) // 调试信息

          // 如果有会议列表，显示会议选择对话框
          if (this.availableMeetings.length > 0) {
            this.showMeetingSelection = true
            this.selectedMeetingId = this.availableMeetings[0].id  // 默认选择第一个会议
          } else {
            // 如果没有现有会议，直接创建新会议
            this.createNewMeeting()
          }
        } else {
          console.error('Unexpected API response format:', response)
          this.availableMeetings = []
          // 即使没有现有会议也允许创建新会议
          this.createNewMeeting()
        }
      } catch (error) {
        console.error('加载会议列表失败:', error)
        // 检查具体的错误信息
        if (error.response) {
          console.error('API Error:', error.response.status, error.response.data)

          // 根据错误类型给出具体提示
          if (error.response.status === 401 || error.response.status === 403) {
            this.$message.error('没有权限获取会议列表，请检查用户权限')
          } else if (error.response.status === 404) {
            this.$message.error('会议列表API未找到，请检查后端服务')
          } else {
            this.$message.error(`获取会议列表失败: ${error.response.data.msg || error.response.statusText}`)
          }
        } else if (error.request) {
          // 请求已发出但没有收到响应
          console.error('Network Error:', error.request)
          this.$message.error('网络错误，请检查后端服务是否运行')
        } else {
          // 其他错误
          console.error('Request Error:', error.message)
          this.$message.error(`请求错误: ${error.message}`)
        }

        // 即使加载失败，也创建新会议让录制功能可用
        this.availableMeetings = []
        this.createNewMeeting()
      }
    },

    // 确认会议选择
    confirmMeetingSelection() {
      if (this.selectedMeetingId === 'new') {
        // 创建新会议的逻辑
        this.createNewMeeting()
        this.showMeetingSelection = false
      } else {
        // 使用现有的会议ID
        this.meetingId = parseInt(this.selectedMeetingId)
        this.showMeetingSelection = false
        this.$message.success(`已选择会议，ID: ${this.meetingId}`)
        console.log('已选择会议ID:', this.meetingId) // 调试信息
      }
    },

    // 创建新会议
    async createNewMeeting() {
      this.$message.warning('此功能需要跳转到会议管理页面创建会议')
      // 提示用户前往会议管理页面创建会议
      this.$confirm('您需要先在会议管理页面创建会议，然后再回到这里开始录制。是否现在跳转？', '提示', {
        confirmButtonText: '跳转',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 跳转到会议管理页面
        this.$router.push('/huiyi/meeting')
      }).catch(() => {
        // 如果用户取消，则显示会议选择
        this.showMeetingSelection = true
        this.$message.info('请先创建会议或选择已有会议')
      })
    },

    // 取消会议选择
    cancelMeetingSelection() {
      this.showMeetingSelection = false
      this.$message.info('已取消会议选择，无法开始录制')
    },

    // 处理会议选择对话框关闭
    handleMeetingSelectionClose() {
      this.cancelMeetingSelection()
    },

    // 开始录制
    async startRecording() {
      // 检查是否选择了会议
      if (!this.meetingId) {
        this.$message.warning('请先选择会议')
        // 显示会议选择对话框
        this.showMeetingSelection = true
        return
      }

      this.startLoading = true

      try {
        console.log('正在启动录制，会议ID:', this.meetingId) // 调试信息

        // 调用开始录制接口
        const response = await startRecord(this.meetingId)
        console.log('开始录制响应:', response) // 调试信息

        // 检查响应结构，兼容不同的返回格式
        let wsPath;
        if (response.data && response.data.wsPath) {
          // 标准格式：{ code: 200, msg: "success", data: { meetingId, recordStatus, wsPath } }
          wsPath = response.data.wsPath;
        } else if (response.wsPath) {
          // 直接返回格式：{ meetingId, recordStatus, wsPath }
          wsPath = response.wsPath;
        } else {
          throw new Error('API响应格式不正确，未找到wsPath');
        }

        // 连接WebSocket
        this.connectWebSocket(wsPath)

        // 等待WebSocket连接建立后再启动音频采集
        // 等待WebSocket连接状态变为OPEN（最多等待5秒）
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('WebSocket连接超时')), 5000)
        })

        // 等待WebSocket连接成功或超时
        await Promise.race([
          new Promise((resolve) => {
            const checkConnection = () => {
              if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                resolve()
              } else if (this.ws && this.ws.readyState === WebSocket.CLOSED) {
                throw new Error('WebSocket连接已关闭')
              } else {
                setTimeout(checkConnection, 100) // 每100ms检查一次
              }
            }
            checkConnection()
          }),
          timeoutPromise
        ])

        // WebSocket连接成功后，启动音频采集，这会触发麦克风权限请求
        try {
          await this.audioCapture.start(this.handleAudioData.bind(this))
          console.log('音频采集启动成功')
        } catch (audioError) {
          console.error('音频采集启动失败:', audioError)

          // 如果是权限问题，显示权限对话框
          if (audioError.name === 'NotAllowedError' ||
              audioError.message.includes('permission') ||
              audioError.message.toLowerCase().includes('permission')) {
            this.permissionDialogVisible = true
            this.$message.error('需要麦克风权限才能开始录制，请允许浏览器访问麦克风')
            return
          } else {
            throw audioError // 其他错误抛出给外层处理
          }
        }

        // 更新状态
        this.currentStatus = STATUS.RECORDING
        this.startTime = Date.now()
        this.startTimer()

        console.log('录制已开始，会议ID:', this.meetingId)

        // 隐藏会议选择对话框（如果仍可见）
        this.showMeetingSelection = false

        this.$message.success(`录制已开始，会议ID: ${this.meetingId}`)
      } catch (error) {
        console.error('开始录制失败:', error)

        // 检查错误类型
        if (error.response) {
          if (error.response.status === 404 || error.response.status === 400) {
            this.$message.error(`会议ID ${this.meetingId} 不存在或无法使用，请选择其他会议`)
            this.showMeetingSelection = true // 显示会议选择对话框让用户重新选择
          } else {
            this.$message.error(`API错误: ${error.response.data.msg || error.response.statusText}`)
          }
        } else if (error.message && error.message.includes('WebSocket连接超时')) {
          this.$message.error('WebSocket连接超时，请检查网络连接或后端服务')
        } else {
          // 如果不是API错误，可能是其他类型的错误
          this.$message.error(error.message || '开始录制失败')
        }

        // 关闭WebSocket连接（如果已建立）
        if (this.ws) {
          this.ws.close()
          this.ws = null
        }

        // 如果失败，确保状态被重置
        this.currentStatus = STATUS.NOT_STARTED
      } finally {
        this.startLoading = false
      }
    },

    // 暂停录制
    async pauseRecording() {
      if (!this.meetingId) {
        this.$message.warning('无效的会议ID，无法暂停录制')
        return
      }

      try {
        await pauseRecord(this.meetingId)

        // 同时暂停音频采集
        if (this.audioCapture && typeof this.audioCapture.pause === 'function') {
          this.audioCapture.pause()
        }

        this.stopTimer()
        this.currentStatus = STATUS.PAUSED

        this.$message.success('录制已暂停')
      } catch (error) {
        console.error('暂停录制失败:', error)
        this.$message.error('暂停录制失败')
      }
    },

    // 继续录制
    async resumeRecording() {
      if (!this.meetingId) {
        this.$message.warning('无效的会议ID，无法继续录制')
        return
      }

      try {
        await resumeRecord(this.meetingId)

        // 同时恢复音频采集
        if (this.audioCapture && typeof this.audioCapture.resume === 'function') {
          this.audioCapture.resume()
        }

        this.startTime = Date.now() - this.elapsedTime // 从上次暂停的时间继续计时
        this.startTimer()
        this.currentStatus = STATUS.RECORDING

        this.$message.success('录制已继续')
      } catch (error) {
        console.error('继续录制失败:', error)
        this.$message.error('继续录制失败')
      }
    },

    // 结束录制
    async stopRecording() {
      if (!this.meetingId) {
        this.$message.warning('无效的会议ID，无法结束录制')
        return
      }

      try {
        await stopRecord(this.meetingId)

        // 关闭WebSocket连接
        this.closeWebSocket()

        // 停止音频采集
        if (this.audioCapture && typeof this.audioCapture.stop === 'function') {
          try {
            await this.audioCapture.stop()
          } catch (error) {
            console.error('停止音频采集失败:', error)
          }
        }

        // 更新状态并开始轮询
        this.currentStatus = STATUS.STOP_PENDING
        this.stopTimer()

        // 开始轮询状态
        this.pollStatus()

        this.$message.success('录制已结束，正在处理...')
      } catch (error) {
        console.error('结束录制失败:', error)
        this.$message.error('结束录制失败')
      }
    },

    // 开始计时器
    startTimer() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval)
      }

      this.timerInterval = setInterval(() => {
        if (this.currentStatus === STATUS.RECORDING) {
          this.elapsedTime = Date.now() - this.startTime
        }
      }, 1000)
    },

    // 停止计时器
    stopTimer() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval)
        this.timerInterval = null
      }
    },

    // 处理音频数据
    handleAudioData(audioBuffer) {
      if (this.ws && this.ws.readyState === WebSocket.OPEN && this.currentStatus === STATUS.RECORDING) {
        // 发送音频数据到WebSocket服务器
        this.ws.send(audioBuffer)

        // 调试信息
        console.log('发送音频数据:', audioBuffer.byteLength, 'bytes')
      } else {
        // 如果WebSocket未连接或非录制状态，输出警告
        if (!this.ws) {
          console.warn('WebSocket未初始化')
        } else if (this.ws.readyState !== WebSocket.OPEN) {
          console.warn('WebSocket未处于连接状态，当前状态:', this.ws.readyState)
        } else {
          console.warn('非录制状态，当前状态:', this.currentStatus)
        }
      }
    },

    // 连接WebSocket
    connectWebSocket(wsPath) {
      return new Promise((resolve, reject) => {
        // 确保会议ID有效
        if (!this.meetingId) {
          console.error('无效的会议ID，无法连接WebSocket')
          this.$message.error('会议ID无效，无法连接WebSocket')
          reject(new Error('会议ID无效'))
          return
        }

        // 构建完整的WebSocket URL
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const wsUrl = `ws://localhost:8080${wsPath}`

        console.log('正在连接WebSocket:', wsUrl) // 调试信息

        this.ws = new WebSocket(wsUrl)

        this.ws.onopen = () => {
          console.log('WebSocket连接已建立')
          this.reconnectAttempts = 0 // 重置重连次数
          this.$message.success('WebSocket连接已建立，可以开始录制')
          resolve() // 连接成功时resolve
        }

        this.ws.onmessage = (event) => {
          // 处理服务端返回的消息
          try {
            if (typeof event.data === 'string') {
              const data = JSON.parse(event.data)

              // 如果是心跳响应，忽略
              if (data === 'pong') {
                return
              }

              console.log('收到WebSocket消息:', data) // 调试信息

              // 添加字幕到列表
              this.addCaption(data)
            }
          } catch (error) {
            console.error('解析WebSocket消息失败:', error)
            console.error('原始消息:', event.data) // 调试信息
          }
        }

        this.ws.onclose = (event) => {
          console.log('WebSocket连接已关闭', event)
          this.$message.warning('WebSocket连接已断开')

          // 如果连接从未建立就关闭了，应该拒绝Promise
          if (this.ws.readyState !== WebSocket.OPEN) {
            console.error('WebSocket连接未能建立就已关闭')
            reject(new Error('WebSocket连接已关闭'))
          }

          // 如果仍在录制状态，尝试重连
          if ([STATUS.RECORDING, STATUS.PAUSED].includes(this.currentStatus) && this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
              this.reconnectAttempts++
              console.log(`尝试重连WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
              this.connectWebSocket(wsPath)
            }, 2000)
          } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            this.networkErrorDialogVisible = true
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket错误:', error)
          this.$message.error('WebSocket连接发生错误')
          reject(error) // 连接错误时reject
        }

        // 设置超时处理 - 10秒内如果连接状态还不是OPEN，则认为连接失败
        setTimeout(() => {
          if (this.ws && this.ws.readyState !== WebSocket.OPEN) {
            console.error('WebSocket连接超时，当前状态:', this.ws.readyState)
            const states = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED']
            console.error(`WebSocket状态: ${states[this.ws.readyState]} (${this.ws.readyState})`)

            if (this.ws.readyState !== WebSocket.OPEN) {
              reject(new Error('WebSocket连接超时'))

              // 如果仍在CONNECTING状态，手动关闭连接
              if (this.ws.readyState === WebSocket.CONNECTING) {
                this.ws.close()
              }
            }
          }
        }, 10000) // 10秒超时

        // 开始心跳
        this.startHeartbeat()
      })
    },

    // 开始WebSocket心跳
    startHeartbeat() {
      if (!this.ws) return

      // 发送心跳的定时器
      this.heartbeatInterval = setInterval(() => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send('ping')
        }
      }, 20000) // 每20秒发送一次心跳
    },

    // 关闭WebSocket连接
    closeWebSocket() {
      if (this.heartbeatInterval) {
        clearInterval(this.heartbeatInterval)
        this.heartbeatInterval = null
      }

      if (this.ws) {
        this.ws.close()
        this.ws = null
      }
    },

    // 添加字幕
    addCaption(captionData) {
      // 添加到字幕列表
      this.captions.push({
        seqNo: captionData.seqNo,
        startOffsetMs: captionData.startOffsetMs,
        endOffsetMs: captionData.endOffsetMs,
        text: captionData.text,
        timestamp: Date.now()
      })

      // 更新最新字幕序列号
      this.latestCaptionSeq = Math.max(this.latestCaptionSeq, captionData.seqNo)

      // 限制字幕数量，防止内存占用过多
      if (this.captions.length > 100) {
        this.captions = this.captions.slice(-50) // 保留最新的50条
      }
    },

    // 格式化时间戳
    formatTimestamp(milliseconds) {
      const seconds = Math.floor(milliseconds / 1000)
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    },

    // 轮询录制状态
    pollStatus() {
      // 确保会议ID有效才开始轮询
      if (!this.meetingId) {
        console.error('无效的会议ID，无法轮询状态')
        return
      }

      this.statusPollingTimer = setInterval(async () => {
        try {
          const response = await getRecordStatus(this.meetingId)

          // 检查响应结构，兼容不同的返回格式
          let recordStatus, recordStatusDesc, summaryText, audioFilePath, recordDurationMs, meetingTopic;
          if (response.data) {
            // 标准格式：{ code: 200, msg: "success", data: { ... } }
            ({ recordStatus, recordStatusDesc, summaryText, audioFilePath,
               recordDurationMs, meetingTopic } = response.data)
          } else {
            // 直接返回格式：{ ... }
            ({ recordStatus, recordStatusDesc, summaryText, audioFilePath,
               recordDurationMs, meetingTopic } = response)
          }

          this.currentStatus = recordStatus

          // 如果状态变为处理中，更新界面
          if (recordStatus === STATUS.PROCESSING) {
            this.currentStatus = STATUS.PROCESSING
          }
          // 如果状态变为完成，显示结果
          else if (recordStatus === STATUS.COMPLETED) {
            this.currentStatus = STATUS.COMPLETED

            // 更新会议纪要数据
            this.audioFilePath = audioFilePath || ''

            // 组织会议纪要数据
            this.summaryData = {
              meetingInfo: {
                topic: meetingTopic || '未识别主题',
                duration: this.formatMilliseconds(recordDurationMs || 0),
                participants: '未知', // 后续可通过接口获取
                time: new Date().toLocaleString()
              },
              transcription: this.captions.map(c => `${this.formatTimestamp(c.startOffsetMs)} ${c.text}`).join('\n') || '',
              abstract: summaryText || '暂无摘要',
              actions: [], // 后续可从接口获取行动项
              keypoints: [], // 后续可从接口获取要点
              attachments: [] // 后续可从接口获取附件
            }

            // 停止轮询
            this.stopPolling()
          }
          // 如果状态变为异常，显示错误
          else if (recordStatus === STATUS.FAILED) {
            this.currentStatus = STATUS.FAILED
            this.$message.error(recordStatusDesc || '处理过程中出现错误')
            this.stopPolling()
          }
        } catch (error) {
          console.error('获取录制状态失败:', error)
          this.$message.error('获取录制状态失败')

          // 如果是404或类似的错误，可能会议ID无效，停止轮询
          if (error.response && (error.response.status === 404 || error.response.status === 400)) {
            this.stopPolling()
            this.currentStatus = STATUS.FAILED
            this.$message.error('会议不存在或参数错误')
          }
        }
      }, 2000) // 每2秒轮询一次
    },

    // 将毫秒格式化为 HH:mm:ss
    formatMilliseconds(ms) {
      const totalSeconds = Math.floor(ms / 1000)
      const hours = Math.floor(totalSeconds / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60

      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
    },

    // 停止轮询
    stopPolling() {
      if (this.statusPollingTimer) {
        clearInterval(this.statusPollingTimer)
        this.statusPollingTimer = null
      }
    },

    // 重试获取麦克风权限
    retryPermission() {
      this.permissionDialogVisible = false
      this.startRecording()
    },

    // 下载音频文件
    downloadAudio() {
      if (!this.audioFilePath) {
        this.$message.warning('没有找到录音文件')
        return
      }

      // 构造下载链接
      const downloadUrl = `${process.env.VUE_APP_BASE_API}${this.audioFilePath}`

      // 创建隐藏的下载链接
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `meeting-recording-${this.meetingId}.wav`
      link.target = '_blank'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },

    // 下载会议纪要
    downloadSummary() {
      // 这里可以实现PDF下载逻辑
      this.$message.info('会议纪要PDF下载功能开发中')
    },

    // 下载转写内容
    downloadTranscript() {
      // 创建并下载转写内容
      const content = this.captions.map(c => `${this.formatTimestamp(c.startOffsetMs)} ${c.text}`).join('\n')
      const element = document.createElement('a')
      const file = new Blob([content], { type: 'text/plain' })
      element.href = URL.createObjectURL(file)
      element.download = `meeting-transcript-${this.meetingId}-${Date.now()}.txt`
      document.body.appendChild(element)
      element.click()
      document.body.removeChild(element)
    },

    // 下载附件
    downloadAttachment(attachment) {
      // 实现附件下载逻辑
      this.$message.info('附件下载功能开发中')
    },

    // 行动项变更处理
    onActionChange(action) {
      // 处理行动项状态变更
      console.log('Action changed:', action)
    },

    // 开始波形可视化
    startVisualization() {
      this.visualizationInterval = setInterval(() => {
        if (this.audioCapture && this.showVisualizer) {
          // 获取音量等级
          const volumeLevel = this.audioCapture.getVolumeLevel()

          // 动态更新波形高度
          this.barHeights = this.barHeights.map((_, index) => {
            // 基础高度 + 随机波动 + 音量影响
            const baseHeight = 5
            const fluctuation = Math.sin(Date.now() / 200 + index) * 5
            const volumeEffect = volumeLevel * 40 // 音量影响最大40px
            return Math.max(baseHeight, baseHeight + fluctuation + volumeEffect)
          })

          // 动态更新颜色
          this.barColors = this.barHeights.map(height => {
            const intensity = Math.min(1, height / 50)
            return `rgb(${Math.floor(64 + 100 * intensity)}, ${Math.floor(158 + 50 * intensity)}, ${Math.floor(255 * intensity)})`
          })
        }
      }, 100) // 每100ms更新一次
    },

    // 清空字幕
    clearCaptions() {
      this.captions = []
      this.$message.success('字幕已清空')
    },

    // 页面销毁前清理
    async beforeDestroy() {
      // 销毁前清理资源
      await this.cleanup()
    },

    // 字幕悬停事件
    onCaptionHover(caption) {
      console.log('Caption hovered:', caption)
    },

    // 字幕离开事件
    onCaptionLeave(caption) {
      console.log('Caption left:', caption)
    },

    // 清理资源
    async cleanup() {
      // 停止计时器
      this.stopTimer()
      this.stopPolling()

      // 停止可视化
      if (this.visualizationInterval) {
        clearInterval(this.visualizationInterval)
        this.visualizationInterval = null
      }

      // 停止音频采集
      if (this.audioCapture && typeof this.audioCapture.stop === 'function') {
        try {
          await this.audioCapture.stop()
        } catch (error) {
          console.error('停止音频采集时出错:', error)
        }
      }

      // 关闭WebSocket
      this.closeWebSocket()
    }
  },

  // 页面卸载前的处理
  beforeRouteLeave(to, from, next) {
    // 如果还在录制中，尝试停止录制
    if ([STATUS.RECORDING, STATUS.PAUSED].includes(this.currentStatus)) {
      // 使用navigator.sendBeacon尽力保存录制内容
      if (navigator.sendBeacon && this.meetingId) {
        try {
          const stopUrl = `${process.env.VUE_APP_BASE_API}/huiyi/record/${this.meetingId}/stop`
          navigator.sendBeacon(stopUrl, '')
        } catch (error) {
          console.error('尝试停止录制时出错:', error)
        }
      }
    }

    this.cleanup()
    next()
  }
}
</script>

<style lang="scss" scoped>
.recording-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;

  .container {
    max-width: 1200px;
    margin: 0 auto;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    padding: 30px;
    min-height: 600px;
  }

  .status-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 0;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 30px;

    .timer {
      font-size: 24px;
      font-weight: bold;
      color: #606266;
    }

    .status-text {
      font-size: 16px;
      color: #909399;
    }
  }

  .visualizer-container {
    margin: 30px auto;
    text-align: center;

    .visualizer {
      display: flex;
      align-items: flex-end;
      justify-content: center;
      padding: 20px;
      min-height: 120px;
      background: linear-gradient(to bottom, #f8f9fa, #ffffff);
      border-radius: 8px;
      border: 1px solid #ebeef5;
    }

    .bar {
      width: 8px;
      border-radius: 4px;
      transition: height 0.1s ease;
    }
  }

  .control-section {
    margin: 40px 0;
    text-align: center;

    .center-start-btn {
      .start-record-btn {
        width: 120px;
        height: 120px;
        border-radius: 50%;
        font-size: 16px;
        padding: 0;
      }
    }

    .control-buttons {
      display: flex;
      justify-content: center;
      gap: 20px;

      ::v-deep .el-button {
        min-width: 100px;
        font-size: 16px;
      }
    }

    .processing-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .loading-spinner {
        font-size: 48px;
        color: #409EFF;
        margin-bottom: 15px;
      }

      .processing-text {
        font-size: 18px;
        color: #606266;
      }
    }
  }

  .captions-container {
    margin-top: 30px;
  }

  .result-container {
    margin-top: 30px;
  }

  .meeting-selection-content {
    .selection-description {
      margin-bottom: 20px;
      color: #606266;
      font-size: 14px;
    }

    .meeting-options {
      width: 100%;
      display: block;

      .meeting-option {
        display: block;
        margin-bottom: 10px;
        padding: 10px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;

        ::v-deep .el-radio__input {
          margin-top: 6px;
        }

        ::v-deep .el-radio__label {
          width: calc(100% - 24px);
          padding-left: 10px;
        }
      }
    }
  }
}

@keyframes highlight {
  0% { background-color: rgba(64, 158, 255, 0.2); }
  100% { background-color: transparent; }
}
</style>