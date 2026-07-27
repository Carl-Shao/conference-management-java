<template>
  <div class="meeting-summary-container">
    <div class="summary-header">
      <h3>会议纪要</h3>
    </div>

    <div class="summary-content">
      <!-- 会议基本信息 -->
      <div class="meeting-info">
        <h4>会议信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <label>会议主题:</label>
            <span>{{ meetingInfo.topic || '暂无主题' }}</span>
          </div>
          <div class="info-item">
            <label>会议时间:</label>
            <span>{{ meetingInfo.time || '未知时间' }}</span>
          </div>
          <div class="info-item">
            <label>参会人数:</label>
            <span>{{ meetingInfo.participants || '未知' }}</span>
          </div>
          <div class="info-item">
            <label>会议时长:</label>
            <span>{{ meetingInfo.duration || '未知' }}</span>
          </div>
        </div>
      </div>

      <!-- 完整转写内容 -->
      <div class="transcription-section">
        <h4>完整转写内容</h4>
        <el-collapse v-model="activeNames">
          <el-collapse-item name="transcription" title="点击查看完整转写">
            <div class="transcription-content">
              <pre>{{ transcription }}</pre>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 会议摘要 -->
      <div class="abstract-section">
        <h4>会议摘要</h4>
        <div class="abstract-content" v-html="safeAbstract"></div>
      </div>

      <!-- 行动项 -->
      <div class="actions-section">
        <h4>行动项</h4>
        <div class="actions-list">
          <div
            v-for="(action, index) in actions"
            :key="index"
            class="action-item"
          >
            <div class="action-content">
              <el-checkbox v-model="action.completed" @change="onActionChange(action)">
                <span :class="{ 'completed': action.completed }">{{ action.content }}</span>
              </el-checkbox>
            </div>
            <div class="action-meta">
              <span class="assignee">负责人: {{ action.assignee || '未指定' }}</span>
              <span class="deadline">截止日期: {{ action.deadline || '未设定' }}</span>
            </div>
          </div>
          <div v-if="!actions || actions.length === 0" class="no-actions">
            暂无行动项
          </div>
        </div>
      </div>

      <!-- 会议要点 -->
      <div class="keypoints-section">
        <h4>会议要点</h4>
        <div class="keypoints-content">
          <ul>
            <li
              v-for="(point, index) in keypoints"
              :key="index"
              class="keypoint-item"
            >
              {{ point }}
            </li>
            <li v-if="!keypoints || keypoints.length === 0" class="no-keypoints">
              暂无会议要点
            </li>
          </ul>
        </div>
      </div>

      <!-- 附件 -->
      <div class="attachments-section">
        <h4>会议附件</h4>
        <div class="attachments-list">
          <div
            v-for="(attachment, index) in attachments"
            :key="index"
            class="attachment-item"
          >
            <i class="el-icon-document"></i>
            <span class="filename">{{ attachment.name }}</span>
            <span class="filesize">{{ attachment.size }}</span>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-download"
              @click="downloadAttachment(attachment)"
            >
              下载
            </el-button>
          </div>
          <div v-if="!attachments || attachments.length === 0" class="no-attachments">
            暂无附件
          </div>
        </div>
      </div>

      <!-- 下载区域 -->
      <div class="download-section">
        <h4>下载选项</h4>
        <div class="download-buttons">
          <el-button
            type="primary"
            icon="el-icon-download"
            @click="downloadSummary"
          >
            下载纪要 (PDF)
          </el-button>
          <el-button
            icon="el-icon-video-camera"
            @click="downloadRecording"
          >
            下载录音
          </el-button>
          <el-button
            icon="el-icon-document"
            @click="downloadTranscript"
          >
            下载转写 (TXT)
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MeetingSummary',
  props: {
    summaryData: {
      type: Object,
      default: () => ({
        meetingInfo: {},
        transcription: '',
        abstract: '',
        actions: [],
        keypoints: [],
        attachments: []
      })
    },
    recordingUrl: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      activeNames: [],
      meetingInfo: {},
      transcription: '',
      abstract: '',
      actions: [],
      keypoints: [],
      attachments: []
    }
  },
  computed: {
    safeAbstract() {
      // 确保摘要内容安全，过滤可能的恶意HTML
      return this.abstract.replace(/<(script|iframe)[^>]*>.*?<\/\1>/gi, '');
    }
  },
  watch: {
    summaryData: {
      handler(newVal) {
        this.updateSummaryData(newVal);
      },
      immediate: true
    }
  },
  methods: {
    updateSummaryData(data) {
      this.meetingInfo = data.meetingInfo || {};
      this.transcription = data.transcription || '';
      this.abstract = data.abstract || '';
      this.actions = data.actions || [];
      this.keypoints = data.keypoints || [];
      this.attachments = data.attachments || [];
    },

    onActionChange(action) {
      // 触发行动项变更事件
      this.$emit('action-change', action);
    },

    downloadSummary() {
      // 下载PDF纪要的逻辑
      this.$emit('download-summary');
    },

    downloadRecording() {
      // 下载录音的逻辑
      if (!this.recordingUrl) {
        this.$message.warning('录音文件不可用');
        return;
      }

      const link = document.createElement('a');
      link.href = this.recordingUrl;
      link.download = `meeting-recording-${Date.now()}.wav`;
      link.target = '_blank';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      this.$emit('download-recording');
    },

    downloadTranscript() {
      // 下载转写内容的逻辑
      const element = document.createElement('a');
      const file = new Blob([this.transcription], { type: 'text/plain' });
      element.href = URL.createObjectURL(file);
      element.download = `meeting-transcript-${Date.now()}.txt`;
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);

      this.$emit('download-transcript');
    },

    downloadAttachment(attachment) {
      // 下载附件的逻辑
      this.$emit('download-attachment', attachment);
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-summary-container {
  .summary-header {
    h3 {
      margin: 0 0 20px 0;
      color: #303133;
      border-bottom: 1px solid #ebeef5;
      padding-bottom: 10px;
    }
  }

  .summary-content {
    .meeting-info {
      margin-bottom: 30px;

      h4 {
        margin: 0 0 15px 0;
        color: #303133;
      }

      .info-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 15px;

        .info-item {
          display: flex;
          flex-direction: column;

          label {
            font-weight: bold;
            color: #606266;
            margin-bottom: 5px;
            font-size: 14px;
          }

          span {
            color: #909399;
            padding: 5px 0;
          }
        }
      }
    }

    .transcription-section,
    .abstract-section,
    .actions-section,
    .keypoints-section,
    .attachments-section,
    .download-section {
      margin-bottom: 30px;

      h4 {
        margin: 0 0 15px 0;
        color: #303133;
        padding-bottom: 8px;
        border-bottom: 1px solid #ebeef5;
      }

      .transcription-content {
        pre {
          white-space: pre-wrap;
          word-wrap: break-word;
          padding: 15px;
          background-color: #f4f4f5;
          border-radius: 4px;
          max-height: 300px;
          overflow-y: auto;
          font-family: inherit;
          color: #606266;
          line-height: 1.6;
          margin: 0;
        }
      }

      .abstract-content {
        padding: 15px;
        background-color: #f4f4f5;
        border-radius: 4px;
        line-height: 1.8;
        color: #606266;

        ::v-deep p {
          margin: 10px 0;
        }

        ::v-deep ul, ::v-deep ol {
          padding-left: 20px;
          margin: 10px 0;
        }

        ::v-deep li {
          margin: 5px 0;
        }
      }

      .actions-list {
        .action-item {
          padding: 15px;
          border: 1px solid #ebeef5;
          border-radius: 4px;
          margin-bottom: 10px;
          background-color: #fff;

          .action-content {
            margin-bottom: 10px;

            ::v-deep .el-checkbox {
              width: 100%;

              span:not(.el-checkbox__input) {
                flex: 1;
              }

              .completed {
                text-decoration: line-through;
                color: #c0c4cc;
              }
            }
          }

          .action-meta {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #909399;

            .assignee, .deadline {
              margin-right: 15px;
            }
          }
        }

        .no-actions {
          padding: 20px;
          text-align: center;
          color: #909399;
          background-color: #f8f9fa;
          border-radius: 4px;
        }
      }

      .keypoints-content {
        ul {
          padding: 0;
          margin: 0;

          .keypoint-item {
            list-style: none;
            padding: 8px 0;
            border-bottom: 1px dashed #ebeef5;
            position: relative;
            padding-left: 20px;

            &::before {
              content: '•';
              position: absolute;
              left: 0;
              color: #409EFF;
              font-weight: bold;
            }

            &:last-child {
              border-bottom: none;
            }
          }

          .no-keypoints {
            padding: 20px;
            text-align: center;
            color: #909399;
            background-color: #f8f9fa;
            border-radius: 4px;
          }
        }
      }

      .attachments-list {
        .attachment-item {
          display: flex;
          align-items: center;
          padding: 10px;
          border: 1px solid #ebeef5;
          border-radius: 4px;
          margin-bottom: 10px;
          background-color: #fff;

          i {
            margin-right: 10px;
            color: #409EFF;
          }

          .filename {
            flex: 1;
            color: #606266;
          }

          .filesize {
            margin: 0 15px;
            color: #909399;
            font-size: 12px;
          }
        }

        .no-attachments {
          padding: 20px;
          text-align: center;
          color: #909399;
          background-color: #f8f9fa;
          border-radius: 4px;
        }
      }

      .download-buttons {
        display: flex;
        gap: 15px;
        flex-wrap: wrap;

        .el-button {
          margin: 0;
        }
      }
    }
  }
}
</style>