<template>
  <div class="summary-result">
    <div class="summary-section">
      <h3>会议主题</h3>
      <div class="topic-content">{{ topic }}</div>
    </div>

    <div class="summary-section">
      <el-collapse v-model="activeNames">
        <el-collapse-item title="完整转写全文" name="transcript">
          <div class="transcript-content">
            <pre>{{ transcript }}</pre>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="summary-section">
      <h3>会议纪要</h3>
      <div class="summary-content" v-html="formattedSummary"></div>
    </div>

    <div class="action-buttons">
      <el-button type="primary" @click="downloadAudio" icon="el-icon-download">下载完整录音</el-button>
      <el-button @click="copySummary" icon="el-icon-document-copy">复制纪要</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SummaryResult',
  props: {
    summaryData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      activeNames: [] // 默认展开的折叠面板
    }
  },
  computed: {
    topic() {
      // 从summaryText中提取主题（通常是第一行或以"主题："开头的部分）
      if (this.summaryData.summaryText) {
        const lines = this.summaryData.summaryText.split('\n');
        for (let line of lines) {
          line = line.trim();
          if (line.startsWith('主题：')) {
            return line.substring(3).trim();
          }
          if (line.startsWith('会议主题：')) {
            return line.substring(5).trim();
          }
          // 如果第一行看起来像是标题（较短且不含句号）
          if (lines.indexOf(line) === 0 && line.length < 50 && !line.includes('。')) {
            return line;
          }
        }
      }
      return '未识别会议主题';
    },
    transcript() {
      // 假设转写内容在summaryText中的某个部分
      if (this.summaryData.summaryText) {
        const lines = this.summaryData.summaryText.split('\n');
        let transcriptStart = -1;

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim();
          if (line.includes('转写') || line.includes('原文') || line.includes('发言')) {
            transcriptStart = i + 1;
            break;
          }
        }

        if (transcriptStart !== -1) {
          return lines.slice(transcriptStart).join('\n').trim();
        }
        return this.summaryData.summaryText;
      }
      return '';
    },
    formattedSummary() {
      // 格式化summaryText，将要点转换为列表
      if (this.summaryData.summaryText) {
        let html = this.summaryData.summaryText;
        // 简单的格式化：将"•"或"-"开头的行转换为列表项
        html = html.replace(/^(\s*[•\-]\s+.*)$/gm, '<li>$1</li>');
        html = html.replace(/(<li>.*<\/li>)+/g, '<ul>$&</ul>');

        // 替换换行符为<br>
        html = html.replace(/\n/g, '<br>');

        return html;
      }
      return '';
    }
  },
  methods: {
    downloadAudio() {
      if (this.summaryData.audioFilePath) {
        // 构造完整的下载URL
        const downloadUrl = process.env.VUE_APP_BASE_API + this.summaryData.audioFilePath;
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `meeting_${this.summaryData.meetingId}_recording.wav`;
        link.click();
      } else {
        this.$message.warning('暂无录音文件可供下载');
      }
    },
    copySummary() {
      const summaryText = this.summaryData.summaryText || '';
      if (summaryText) {
        navigator.clipboard.writeText(summaryText).then(() => {
          this.$message.success('纪要已复制到剪贴板');
        }).catch(() => {
          // 降级方案
          const textArea = document.createElement('textarea');
          textArea.value = summaryText;
          document.body.appendChild(textArea);
          textArea.select();
          document.execCommand('copy');
          document.body.removeChild(textArea);
          this.$message.success('纪要已复制到剪贴板');
        });
      } else {
        this.$message.warning('暂无纪要内容可复制');
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.summary-result {
  .summary-section {
    margin-bottom: 24px;

    h3 {
      margin: 0 0 12px 0;
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }

    .topic-content {
      padding: 12px;
      background-color: #f5f7fa;
      border-radius: 4px;
      border-left: 4px solid #409eff;
      font-size: 14px;
      color: #606266;
    }

    .transcript-content {
      pre {
        white-space: pre-wrap;
        word-wrap: break-word;
        font-family: 'Courier New', Courier, monospace;
        font-size: 13px;
        color: #606266;
        line-height: 1.6;
        margin: 0;
        padding: 12px;
        background-color: #f8f9fa;
        border-radius: 4px;
        border: 1px solid #ebeef5;
        max-height: 400px;
        overflow-y: auto;
      }
    }

    .summary-content {
      padding: 16px;
      background-color: #ffffff;
      border: 1px solid #ebeef5;
      border-radius: 4px;
      line-height: 1.8;
      color: #606266;

      ul {
        padding-left: 20px;
        margin: 10px 0;
      }

      li {
        margin-bottom: 8px;
      }
    }
  }

  .action-buttons {
    display: flex;
    gap: 12px;
    margin-top: 24px;
  }
}
</style>