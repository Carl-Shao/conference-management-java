<template>
  <div class="captions-list-container">
    <div class="captions-header">
      <h3>实时字幕</h3>
      <div class="controls">
        <el-button size="mini" icon="el-icon-refresh" @click="clearCaptions">清空</el-button>
      </div>
    </div>
    <div class="captions-list" ref="captionsList">
      <div
        v-for="caption in sortedCaptions"
        :key="caption.seqNo"
        :class="['caption-item', { 'latest': caption.seqNo === latestSeq, 'highlight': caption.highlighted }]"
        @mouseenter="highlightCaption(caption)"
        @mouseleave="unhighlightCaption(caption)"
      >
        <div class="caption-content">
          <span class="timestamp">{{ formatTimestamp(caption.startOffsetMs) }}</span>
          <span class="caption-text">{{ caption.text }}</span>
        </div>
        <div v-if="caption.confidence !== undefined" class="confidence" :style="{ width: caption.confidence * 100 + '%' }">
          <i class="el-icon-star-on"></i>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CaptionsList',
  props: {
    captions: {
      type: Array,
      required: true,
      default: () => []
    },
    latestSeq: {
      type: Number,
      default: -1
    }
  },
  data() {
    return {
      highlightedSeq: -1
    }
  },
  computed: {
    sortedCaptions() {
      // 按照seqNo排序
      return [...this.captions].sort((a, b) => a.seqNo - b.seqNo)
    }
  },
  watch: {
    sortedCaptions: {
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
  methods: {
    formatTimestamp(milliseconds) {
      const seconds = Math.floor(milliseconds / 1000)
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    },
    highlightCaption(caption) {
      this.highlightedSeq = caption.seqNo
      this.$emit('caption-hover', caption)
    },
    unhighlightCaption(caption) {
      this.highlightedSeq = -1
      this.$emit('caption-leave', caption)
    },
    clearCaptions() {
      this.$emit('clear')
    }
  }
}
</script>

<style lang="scss" scoped>
.captions-list-container {
  .captions-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    h3 {
      margin: 0;
      color: #303133;
    }

    .controls {
      .el-button {
        padding: 7px 9px;
      }
    }
  }

  .captions-list {
    height: 200px;
    overflow-y: auto;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 15px;
    background-color: #fafafa;

    .caption-item {
      padding: 10px;
      margin-bottom: 8px;
      border-radius: 4px;
      position: relative;
      transition: all 0.3s ease;

      &.latest {
        background-color: rgba(64, 158, 255, 0.1);
        border-left: 3px solid #409EFF;
      }

      &.highlight {
        background-color: rgba(255, 165, 0, 0.1);
      }

      .caption-content {
        display: flex;
        align-items: flex-start;

        .timestamp {
          color: #909399;
          font-size: 12px;
          min-width: 40px;
          margin-right: 10px;
          flex-shrink: 0;
        }

        .caption-text {
          color: #606266;
          flex: 1;
          line-height: 1.5;
        }
      }

      .confidence {
        position: absolute;
        bottom: 0;
        left: 0;
        height: 2px;
        background-color: #67c23a;
        opacity: 0.7;
      }
    }

    .caption-item:last-child {
      margin-bottom: 0;
    }
  }
}
</style>