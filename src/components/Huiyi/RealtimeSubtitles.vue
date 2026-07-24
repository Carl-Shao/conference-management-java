<template>
  <div class="realtime-subtitles">
    <div class="subtitle-item"
         v-for="subtitle in sortedSubtitles"
         :key="subtitle.seqNo"
         :class="{ 'current-speaking': subtitle.seqNo === latestSeqNo }">
      <div class="timestamp">{{ formatTime(subtitle.startOffsetMs) }} - {{ formatTime(subtitle.endOffsetMs) }}</div>
      <div class="text">{{ subtitle.text }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RealtimeSubtitles',
  props: {
    subtitles: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      latestSeqNo: -1
    }
  },
  computed: {
    sortedSubtitles() {
      // 按seqNo排序，确保时间顺序正确
      return [...this.subtitles].sort((a, b) => a.seqNo - b.seqNo);
    }
  },
  watch: {
    subtitles: {
      handler(newVal) {
        if (newVal.length > 0) {
          // 更新最新的seqNo用于高亮
          const maxSeq = Math.max(...newVal.map(s => s.seqNo));
          if (maxSeq > this.latestSeqNo) {
            this.latestSeqNo = maxSeq;
            // 重置高亮状态，让动画重新播放
            this.$nextTick(() => {
              setTimeout(() => {
                this.latestSeqNo = -1;
              }, 2000); // 2秒后取消高亮
            });
          }
        }
      },
      deep: true
    }
  },
  methods: {
    formatTime(milliseconds) {
      const totalSeconds = Math.floor(milliseconds / 1000);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;

      if (hours > 0) {
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      } else {
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.realtime-subtitles {
  height: 300px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;

  .subtitle-item {
    margin-bottom: 10px;
    padding: 8px 12px;
    border-radius: 4px;
    transition: all 0.3s ease;

    &.current-speaking {
      background-color: #ecf5ff;
      border-left: 3px solid #409eff;
      animation: highlight 0.5s ease;
    }

    .timestamp {
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }

    .text {
      font-size: 14px;
      color: #303133;
      line-height: 1.5;
    }
  }
}

@keyframes highlight {
  0% { background-color: #fff4e6; }
  100% { background-color: #ecf5ff; }
}
</style>