<template>
  <div class="meeting-detail" v-loading="loading">
    <!-- 顶部信息栏 -->
    <div class="detail-header">
      <el-button icon="el-icon-arrow-left" circle size="small" @click="handleBack" />
      <div class="header-info">
        <div class="title-row">
          <span class="title">{{ meeting.title }}</span>
          <el-tag size="small" type="info" effect="plain">已结束</el-tag>
        </div>
        <div class="meta-row">
          <span><i class="el-icon-time" /> {{ meeting.startTime }} - {{ meeting.endTime }}</span>
          <span><i class="el-icon-stopwatch" /> 时长 {{ meeting.duration }}</span>
          <span v-if="meeting.roomName"><i class="el-icon-office-building" /> {{ meeting.roomName }}</span>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="detail-body">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane name="summary">
          <span slot="label"><i class="el-icon-magic-stick" /> AI 会议纪要</span>
          <div class="tab-content summary-content">
            <div class="summary-toolbar">
              <el-button
                size="small"
                icon="el-icon-refresh"
                :loading="regenerating"
                @click="handleRegenerate"
              >重新生成</el-button>
            </div>

            <div v-if="!summary || !summary.overview" class="empty-tip">
              <i class="el-icon-loading" v-if="summaryPending"></i>
              <p>{{ summaryPending ? 'AI 正在生成会议纪要，请稍候…' : '暂无 AI 纪要' }}</p>
            </div>

            <div v-else class="summary-body">
              <div class="summary-section">
                <div class="section-title"><i class="el-icon-document"></i> 会议概述</div>
                <p class="section-text">{{ summary.overview }}</p>
              </div>

              <div class="summary-section" v-if="summary.keyPoints && summary.keyPoints.length">
                <div class="section-title"><i class="el-icon-star-on"></i> 关键要点</div>
                <ul class="section-list">
                  <li v-for="(point, idx) in summary.keyPoints" :key="idx">{{ point }}</li>
                </ul>
              </div>

              <div class="summary-section" v-if="summary.decisions && summary.decisions.length">
                <div class="section-title"><i class="el-icon-circle-check"></i> 决策事项</div>
                <ul class="section-list">
                  <li v-for="(item, idx) in summary.decisions" :key="idx">{{ item }}</li>
                </ul>
              </div>

              <div class="summary-section" v-if="summary.todos && summary.todos.length">
                <div class="section-title"><i class="el-icon-tickets"></i> 待办事项</div>
                <ul class="section-list todo-list">
                  <li v-for="(todo, idx) in summary.todos" :key="idx">
                    <el-checkbox disabled :value="false">{{ todo.content || todo }}</el-checkbox>
                    <span v-if="todo.owner" class="todo-owner">@{{ todo.owner }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="transcript">
          <span slot="label"><i class="el-icon-mic" /> 转写记录</span>
          <div class="tab-content transcript-content">
            <div v-if="transcripts.length === 0" class="empty-tip">
              <p>暂无转写记录</p>
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
                <i v-if="markedIds.has(line.id)" class="el-icon-collection-tag mark-icon"></i>
              </div>
              <div class="line-text">{{ line.text }}</div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="note">
          <span slot="label"><i class="el-icon-edit-outline" /> 我的笔记</span>
          <div class="tab-content note-content">
            <div v-if="!note" class="empty-tip">
              <p>本次会议未记录笔记</p>
            </div>
            <pre v-else class="note-text">{{ note }}</pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import {
  getMeeting,
  getFullTranscript,
  getNote,
  getMeetingSummary,
  regenerateSummary,
  listMarks
} from '@/api/huiyi/meeting'

export default {
  name: 'MeetingDetail',
  data() {
    return {
      meetingId: null,
      loading: false,
      activeTab: 'summary',

      meeting: {},
      transcripts: [],
      markedIds: new Set(),
      note: '',
      summary: null,
      summaryPending: false,
      regenerating: false,
      summaryPollHandle: null
    }
  },
  created() {
    this.meetingId = this.$route.params.meetingId
    this.loadAll()
  },
  beforeDestroy() {
    clearInterval(this.summaryPollHandle)
  },
  methods: {
    loadAll() {
      this.loading = true
      Promise.all([
        getMeeting(this.meetingId),
        getFullTranscript(this.meetingId),
        getNote(this.meetingId),
        listMarks(this.meetingId)
      ]).then(([meetingRes, transcriptRes, noteRes, markRes]) => {
        this.meeting = meetingRes.data || {}
        this.transcripts = transcriptRes.data || transcriptRes.rows || []
        this.note = (noteRes.data && noteRes.data.content) || ''
        const marks = markRes.data || markRes.rows || []
        this.markedIds = new Set(marks.map(m => m.transcriptId).filter(Boolean))
        this.loading = false
        this.loadSummary()
      }).catch(() => {
        this.loading = false
      })
    },
    loadSummary() {
      getMeetingSummary(this.meetingId).then(res => {
        this.summary = res.data
        if (!this.summary || !this.summary.overview) {
          this.summaryPending = true
          this.pollSummary()
        }
      })
    },
    pollSummary() {
      // AI 纪要为异步生成，会议刚结束时轮询等待结果
      this.summaryPollHandle = setInterval(() => {
        getMeetingSummary(this.meetingId).then(res => {
          if (res.data && res.data.overview) {
            this.summary = res.data
            this.summaryPending = false
            clearInterval(this.summaryPollHandle)
          }
        })
      }, 4000)
    },
    handleRegenerate() {
      this.regenerating = true
      regenerateSummary(this.meetingId).then(() => {
        this.$modal.msgSuccess('已提交重新生成请求')
        this.summaryPending = true
        this.regenerating = false
        this.pollSummary()
      }).catch(() => {
        this.regenerating = false
      })
    },
    formatOffset(ms) {
      if (!ms && ms !== 0) return ''
      const totalSec = Math.floor(ms / 1000)
      const m = String(Math.floor(totalSec / 60)).padStart(2, '0')
      const s = String(totalSec % 60).padStart(2, '0')
      return `${m}:${s}`
    },
    handleBack() {
      this.$router.push({ path: '/huiyi/meeting' })
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-detail {
  min-height: 100%;
  background: #f5f6f8;
  padding-bottom: 24px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

  .header-info {
    .title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      .title {
        font-size: 17px;
        font-weight: 600;
        color: #1d2129;
      }
    }
    .meta-row {
      margin-top: 6px;
      font-size: 12px;
      color: #86909c;
      span {
        margin-right: 18px;
      }
      i { margin-right: 3px; }
    }
  }
}

.detail-body {
  padding: 20px 24px;
}

.detail-tabs {
  background: #fff;
  border-radius: 10px;
  padding: 4px 20px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

  ::v-deep .el-tabs__item {
    font-size: 14px;
  }
}

.tab-content {
  min-height: 320px;
  padding-top: 8px;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 60px 0;
  font-size: 13px;
}

/* AI 纪要 */
.summary-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.summary-section {
  margin-bottom: 22px;
  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 8px;
    i { color: #1890ff; margin-right: 6px; }
  }
  .section-text {
    font-size: 14px;
    color: #4e5969;
    line-height: 1.8;
    margin: 0;
  }
  .section-list {
    margin: 0;
    padding-left: 20px;
    li {
      font-size: 14px;
      color: #4e5969;
      line-height: 1.9;
    }
  }
  .todo-list li {
    display: flex;
    align-items: center;
    list-style: none;
    margin-left: -20px;
    .todo-owner {
      margin-left: 8px;
      font-size: 12px;
      color: #1890ff;
    }
  }
}

/* 转写 */
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
      margin-right: 8px;
    }
    .mark-icon {
      color: #faad14;
    }
  }
  .line-text {
    font-size: 14px;
    color: #1d2129;
    line-height: 1.6;
  }
}

/* 笔记 */
.note-text {
  font-family: inherit;
  font-size: 14px;
  color: #1d2129;
  line-height: 1.8;
  white-space: pre-wrap;
  margin: 0;
}
</style>