<template>
  <div class="app-container">
    <el-card class="box-card" shadow="never">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-microphone"></i> 会议语音转文字及纪要生成</span>
      </div>

      <el-row :gutter="20">
        <el-col :span="14">
          <div class="upload-section">
            <el-upload
              class="upload-demo"
              drag
              action="#"
              :auto-upload="false"
              :multiple="true"
              :file-list="fileList"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept=".mp3,.wav,.m4a,.mp4,.avi,.mov,.mkv,.flv,.wmv,.wma"
            >
              <i class="el-icon-upload"></i>
              <div class="el-upload__text">将音频/视频文件拖拽到此处，或<em>点击上传</em></div>
              <div class="el-upload__tip" slot="tip">
                支持的格式：MP3, WAV, M4A, MP4, AVI, MOV 等音视频格式，单次最多上传 5 个文件
              </div>
            </el-upload>

            <div class="upload-actions mt-20">
              <el-button
                type="primary"
                size="mini"
                :disabled="fileList.length === 0 || uploadLoading"
                @click="handleUpload"
                icon="el-icon-upload2"
              >
                {{ uploadLoading ? '处理中...' : '开始处理' }}
              </el-button>
              <el-button
                size="mini"
                @click="clearFiles"
                icon="el-icon-delete"
                :disabled="fileList.length === 0"
              >
                清空列表
              </el-button>
            </div>
          </div>
        </el-col>

        <el-col :span="10">
          <div class="info-section">
            <el-alert
              title="处理说明"
              type="info"
              :closable="false"
              show-icon>
              <p>系统会将您上传的音视频文件转换为文本，并通过AI自动生成会议纪要。</p>
              <p>处理过程包括：</p>
              <ol>
                <li>语音识别（ASR）</li>
                <li>文本转录</li>
                <li>AI智能总结</li>
              </ol>
            </el-alert>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务进度和结果展示区域 -->
    <el-card class="box-card mt-20" v-if="tasks.length > 0" shadow="never">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-timer"></i> 处理进度</span>
        <el-button
          style="float: right; padding: 3px 0"
          type="text"
          @click="refreshTasks"
          icon="el-icon-refresh"
        >
          刷新
        </el-button>
      </div>

      <el-table :data="tasks" style="width: 100%">
        <el-table-column prop="fileName" label="文件名" width="200">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.originalName" placement="top">
              <span>{{ ellipsisFileName(scope.row.originalName) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag
              :type="getStatusType(scope.row.status)"
              size="mini"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="180">
          <template slot-scope="scope">
            <el-progress
              :percentage="scope.row.progress"
              :status="getProgressStatus(scope.row.status)"
              :show-text="scope.row.status !== 'completed'"
            ></el-progress>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template slot-scope="scope">
            {{ parseTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="viewResult(scope.row)"
              :disabled="scope.row.status !== 'completed'"
            >
              查看结果
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-document-copy"
              @click="copyResult(scope.row)"
              :disabled="scope.row.status !== 'completed'"
            >
              复制
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 结果查看对话框 -->
    <el-dialog
      :title="`会议纪要 - ${currentResult.fileName}`"
      :visible.sync="resultDialogVisible"
      width="80%"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div v-loading="resultLoading">
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane label="会议纪要" name="minutes">
            <div class="result-content">
              <pre class="result-text">{{ currentResult.minutes || '暂无会议纪要' }}</pre>
            </div>
          </el-tab-pane>
          <el-tab-pane label="语音转文字" name="transcript">
            <div class="result-content">
              <pre class="result-text">{{ currentResult.transcript || '暂无转录内容' }}</pre>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="resultDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="downloadResult">下载结果</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { uploadAudio, getBatchAudioTaskStatus } from "@/api/huiyi/audio"

export default {
  name: "AudioProcess",
  data() {
    return {
      fileList: [], // 上传的文件列表
      uploadLoading: false, // 上传按钮加载状态
      tasks: [], // 任务列表
      resultDialogVisible: false, // 结果对话框显示状态
      resultLoading: false, // 结果加载状态
      currentResult: {}, // 当前查看的结果
      activeTab: 'minutes', // 激活的标签页
      refreshTimer: null, // 定时刷新定时器
      taskIds: [] // 存储任务ID列表，用于轮询
    }
  },
  created() {
    // 初始化时可以加载最近的任务记录
    this.initTasks()
  },
  destroyed() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
  },
  methods: {
    // 初始化任务列表
    initTasks() {
      // 这里可以调用API获取最近的任务记录
      // 目前暂时模拟数据
    },

    // 文件选择改变事件
    handleFileChange(file, fileList) {
      // 限制上传文件数量
      if (fileList.length > 5) {
        this.$message.warning('最多只能同时上传 5 个文件')
        this.fileList = fileList.slice(0, 5)
      } else {
        this.fileList = fileList
      }
    },

    // 文件移除事件
    handleFileRemove(file, fileList) {
      this.fileList = fileList
    },

    // 清空文件列表
    clearFiles() {
      this.fileList = []
    },

    // 处理上传
    async handleUpload() {
      if (this.fileList.length === 0) {
        this.$message.warning('请先选择要处理的音频文件')
        return
      }

      this.uploadLoading = true

      try {
        // 构建表单数据
        const formData = new FormData()
        this.fileList.forEach(fileObj => {
          formData.append('files', fileObj.raw)
        })

        // 调用上传接口
        const response = await uploadAudio(formData)

        if (response.code === 200) {
          this.$modal.msgSuccess('文件上传成功，正在后台处理中...')

          // 注意：根据后端代码，上传成功后返回的是taskIds列表
          // 这里需要获取实际的任务ID并存储起来用于轮询

          // 将上传的文件加入到任务列表
          this.fileList.forEach((fileObj, index) => {
            const task = {
              id: Date.now() + index,
              fileName: fileObj.name,
              originalName: fileObj.name,
              status: 'processing', // 初始状态为处理中
              progress: 10, // 初始进度
              createTime: new Date(),
              transcript: '', // 转录内容
              minutes: '' // 会议纪要
            }
            this.tasks.unshift(task)
          })

          // 清空文件列表
          this.fileList = []

          // 启动轮询检查处理状态
          this.startPolling()
        } else {
          this.$modal.msgError(response.msg || '上传失败')
        }
      } catch (error) {
        console.error('上传错误:', error)
        this.$modal.msgError('上传失败，请重试')
      } finally {
        this.uploadLoading = false
      }
    },

    // 开始轮询任务状态
    startPolling() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
      }

      this.refreshTimer = setInterval(() => {
        this.checkTaskStatus()
      }, 5000) // 每5秒检查一次
    },

    // 检查任务状态
    async checkTaskStatus() {
      try {
        // 获取所有未完成的任务
        const pendingTasks = this.tasks.filter(task => task.status !== 'completed' && task.status !== 'failed')

        if (pendingTasks.length === 0) {
          // 如果没有未完成的任务，则停止轮询
          if (this.refreshTimer) {
            clearInterval(this.refreshTimer)
            this.refreshTimer = null
          }
          return
        }

        // 模拟从后端获取真实任务状态
        // 实际应用中应替换为真实的API调用
        pendingTasks.forEach(task => {
          // 模拟进度更新
          if (task.progress < 90) {
            task.progress += Math.floor(Math.random() * 5) // 随机增加进度
            if (task.progress > 90) task.progress = 90
          } else {
            // 模拟任务完成
            task.status = 'completed'
            task.progress = 100

            // 模拟生成的内容
            task.transcript = `以下是${task.originalName}的会议转录内容示例：

上午9:00，会议正式开始。
张经理：今天的主要议题是讨论新产品发布计划。
李主管：市场调研结果显示，目标用户群体对产品功能满意度较高。
王工程师：技术实现方面已经完成80%，预计下周能够全部完成。
赵设计师：UI界面设计已经定稿，等待开发团队集成。
会议于上午11:00结束。`;
            task.minutes = `会议纪要：

【会议主题】${task.originalName} - 新产品发布计划讨论
【参会人员】张经理、李主管、王工程师、赵设计师
【会议时间】2024年X月X日 上午9:00-11:00
【主要内容】
1. 市场调研结果显示用户满意度高
2. 技术实现已完成80%
3. UI设计已定稿待集成
【决议事项】
- 下周完成技术开发
- 准备产品发布材料
【下次会议】另行通知`;
          }
        })
      } catch (error) {
        console.error('检查任务状态失败:', error)
      }
    },

    // 获取状态类型
    getStatusType(status) {
      switch (status) {
        case 'pending':
          return 'info'
        case 'processing':
          return 'warning'
        case 'completed':
          return 'success'
        case 'failed':
          return 'danger'
        default:
          return 'info'
      }
    },

    // 获取状态文本
    getStatusText(status) {
      switch (status) {
        case 'pending':
          return '等待处理'
        case 'processing':
          return '处理中'
        case 'completed':
          return '已完成'
        case 'failed':
          return '处理失败'
        default:
          return '未知'
      }
    },

    // 获取进度条状态
    getProgressStatus(status) {
      switch (status) {
        case 'completed':
          return 'success'
        case 'failed':
          return 'exception'
        default:
          return null
      }
    },

    // 缩略显示文件名
    ellipsisFileName(fileName) {
      if (fileName.length > 20) {
        return fileName.substring(0, 10) + '...' + fileName.substring(fileName.length - 10)
      }
      return fileName
    },

    // 刷新任务列表
    refreshTasks() {
      this.checkTaskStatus()
    },

    // 查看结果
    viewResult(task) {
      this.currentResult = task
      this.resultDialogVisible = true
      this.activeTab = 'minutes'
    },

    // 复制结果
    copyResult(task) {
      const content = this.activeTab === 'minutes' ? task.minutes : task.transcript
      if (!content) {
        this.$message.warning('暂无可复制的内容')
        return
      }

      this.$copyText(content).then(() => {
        this.$modal.msgSuccess('已复制到剪贴板')
      }).catch(() => {
        this.$modal.msgError('复制失败')
      })
    },

    // 下载结果
    downloadResult() {
      const content = this.activeTab === 'minutes' ? this.currentResult.minutes : this.currentResult.transcript
      if (!content) {
        this.$message.warning('暂无内容可供下载')
        return
      }

      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
      const filename = this.activeTab === 'minutes'
        ? `会议纪要-${this.currentResult.fileName}.txt`
        : `语音转文字-${this.currentResult.fileName}.txt`

      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename
      link.click()
    }
  }
}
</script>

<style lang="scss" scoped>
.upload-section {
  padding: 20px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  text-align: center;
}

.info-section {
  padding: 20px;
}

.mt-20 {
  margin-top: 20px;
}

.result-content {
  max-height: 50vh;
  overflow-y: auto;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;

  .result-text {
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.6;
    font-family: 'Courier New', Consolas, Monaco, 'Microsoft YaHei';
    font-size: 14px;
    color: #333;
    margin: 0;
  }
}

::v-deep .el-upload-dragger {
  width: 100%;
}

ol {
  padding-left: 20px;
  margin: 10px 0;
}

li {
  margin-bottom: 5px;
}
</style>