<template>
  <div class="meeting-home">
    <!-- 顶部导航栏 -->
    <div class="home-header">
      <div class="header-left">
        <i class="el-icon-microphone header-logo"></i>
        <span class="header-title">智能会议助手</span>
      </div>
      <div class="header-right">
        <el-input
          v-model="queryParams.title"
          placeholder="搜索会议标题"
          prefix-icon="el-icon-search"
          clearable
          size="small"
          class="search-input"
          @keyup.enter.native="handleQuery"
          @clear="handleQuery"
        />
        <el-button
          type="primary"
          icon="el-icon-microphone"
          class="start-btn"
          @click="openStartDialog"
        >开始录音</el-button>
      </div>
    </div>

    <!-- 状态筛选 tab -->
    <div class="home-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleQuery">
        <el-tab-pane label="全部会议" name="all" />
        <el-tab-pane label="进行中" name="ongoing" />
        <el-tab-pane label="已结束" name="ended" />
      </el-tabs>
    </div>

    <!-- 会议列表 -->
    <div class="home-body" v-loading="loading">
      <div v-if="meetingList.length === 0 && !loading" class="empty-block">
        <i class="el-icon-microphone-off empty-icon"></i>
        <p>暂无会议记录，点击右上角「开始录音」创建您的第一个会议记录</p>
      </div>

      <div class="meeting-grid">
        <div
          v-for="item in meetingList"
          :key="item.meetingId"
          class="meeting-card"
          @click="handleEnterMeeting(item)"
        >
          <div class="card-top">
            <span
              class="status-dot"
              :class="{
                'dot-ongoing': item.status === 'ongoing',
                'dot-paused': item.status === 'paused',
                'dot-ended': item.status === 'ended'
              }"
            ></span>
            <span class="status-text">{{ statusText(item.status) }}</span>
            <el-dropdown trigger="click" @command="cmd => handleCommand(cmd, item)" class="card-more" @click.native.stop>
              <i class="el-icon-more"></i>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="detail">查看纪要</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除会议</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

          <div class="card-title">{{ item.title }}</div>

          <div class="card-meta">
            <i class="el-icon-user"></i>
            <span>{{ item.creator || '未知用户' }}</span>
          </div>

          <div class="card-meta">
            <i class="el-icon-time"></i>
            <span>{{ item.startTime }}</span>
          </div>

          <div class="card-meta" v-if="item.duration">
            <i class="el-icon-stopwatch"></i>
            <span>{{ formatDuration(item.duration) }}</span>
          </div>

          <div class="card-footer">
            <span v-if="item.status === 'ended'" class="footer-link">
              查看会议纪要 <i class="el-icon-arrow-right"></i>
            </span>
            <span v-else class="footer-link footer-link-live">
              进入会议 <i class="el-icon-arrow-right"></i>
            </span>
          </div>
        </div>
      </div>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 开始录音弹窗 -->
    <el-dialog
      title="开始录音"
      :visible.sync="startDialogVisible"
      width="480px"
      custom-class="start-recording-dialog"
    >
      <el-form :model="startForm" :rules="startRules" ref="startForm" label-width="80px">
        <el-form-item label="录音标题" prop="title">
          <el-input v-model="startForm.title" placeholder="请输入录音标题，例如：周例会" maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="startDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="starting" @click="confirmStart">开始录音</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMeeting, startMeeting, delMeeting } from '@/api/huiyi/meeting'

export default {
  name: 'MeetingHome',
  data() {
    return {
      loading: false,
      activeTab: 'all',
      total: 0,
      meetingList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        title: undefined,
        status: undefined
      },
      startDialogVisible: false,
      starting: false,
      startForm: {
        title: ''
      },
      startRules: {
        title: [{ required: true, message: '请输入录音标题', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatDuration(durationStr) {
      // 将HH:mm:ss格式转换为更友好的显示格式
      if (!durationStr) return ''

      const parts = durationStr.split(':').map(Number)
      const hours = parts[0]
      const minutes = parts[1]
      const seconds = parts[2]

      if (hours > 0) {
        return `${hours}小时${minutes}分钟`
      } else if (minutes > 0) {
        return `${minutes}分钟${seconds}秒`
      } else {
        return `${seconds}秒`
      }
    },
    statusText(status) {
      const map = { ongoing: '进行中', paused: '已暂停', ended: '已结束' }
      return map[status] || '未知'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.queryParams.status = this.activeTab === 'all' ? undefined : this.activeTab
      this.getList()
    },
    getList() {
      this.loading = true
      listMeeting(this.queryParams).then(res => {
        this.meetingList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    openStartDialog() {
      this.startForm = { title: '' }
      this.startDialogVisible = true
      this.$nextTick(() => {
        this.$refs.startForm && this.$refs.startForm.clearValidate()
      })
    },
    confirmStart() {
      this.$refs.startForm.validate(valid => {
        if (!valid) return
        this.starting = true
        startMeeting(this.startForm).then(res => {
          this.starting = false
          this.startDialogVisible = false
          const meetingId = res.data.meetingId
          // 直接跳转到录音界面，不再需要选择会议室
          this.$router.push({ path: '/huiyi/meeting/recording/' + meetingId })
        }).catch(() => {
          this.starting = false
        })
      })
    },
    handleEnterMeeting(item) {
      if (item.status === 'ended') {
        this.$router.push({ path: '/huiyi/meeting/detail/' + item.meetingId })
      } else {
        // 如果会议正在进行或暂停，进入录音界面
        this.$router.push({ path: '/huiyi/meeting/recording/' + item.meetingId })
      }
    },
    handleCommand(command, item) {
      if (command === 'detail') {
        this.$router.push({ path: '/huiyi/meeting/detail/' + item.meetingId })
      } else if (command === 'delete') {
        this.$modal.confirm('确认删除录音「' + item.title + '」吗？删除后无法恢复。').then(() => {
          return delMeeting(item.meetingId)
        }).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.getList()
        }).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-home {
  min-height: 100%;
  background: #f8fafd;
  padding-bottom: 24px;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid #f0f0f0;

  .header-left {
    display: flex;
    align-items: center;
    .header-logo {
      font-size: 26px;
      color: #1890ff;
      margin-right: 10px;
    }
    .header-title {
      font-size: 18px;
      font-weight: 600;
      color: #1d2129;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    .search-input {
      width: 240px;
      margin-right: 16px;

      ::v-deep .el-input__inner {
        border-radius: 20px;
        border: 1px solid #e5e5e5;
      }
    }
    .start-btn {
      background: linear-gradient(135deg, #1890ff, #40a9ff);
      border: none;
      border-radius: 24px;
      font-weight: 500;
      padding: 10px 20px;
      height: 40px;
      display: flex;
      align-items: center;

      i {
        margin-right: 6px;
      }
    }
  }
}

.home-tabs {
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;

  ::v-deep .el-tabs__header {
    margin: 0;
  }
  ::v-deep .el-tabs__nav-wrap::after {
    display: none;
  }

  ::v-deep .el-tabs__item {
    font-size: 14px;
    padding: 0 20px !important;
  }
}

.home-body {
  padding: 20px 24px;
}

.empty-block {
  text-align: center;
  padding: 100px 0;
  color: #909399;
  .empty-icon {
    font-size: 64px;
    color: #c0c4cc;
    margin-bottom: 16px;
    display: block;
  }
}

.meeting-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.meeting-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.3s, transform 0.3s, border-color 0.3s;
  border: 1px solid #f0f0f0;

  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    transform: translateY(-4px);
    border-color: #1890ff;
  }

  .card-top {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    position: relative;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #c0c4cc;
      margin-right: 8px;
    }
    .dot-ongoing {
      background: #52c41a;
      box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.2);
      animation: pulse 1.5s infinite;
    }
    .dot-paused {
      background: #faad14;
    }
    .dot-ended {
      background: #c0c4cc;
    }
    .status-text {
      font-size: 12px;
      color: #86909c;
      flex: 1;
    }
    .card-more {
      color: #c0c4cc;
      padding: 4px;
      border-radius: 4px;
      &:hover {
        color: #1890ff;
        background: #f0f9ff;
      }
    }
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-meta {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #606266;
    margin-bottom: 6px;

    i {
      margin-right: 6px;
      color: #909399;
      font-size: 12px;
    }
  }

  .card-footer {
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid #f5f5f5;
    .footer-link {
      font-size: 13px;
      color: #1890ff;
      display: flex;
      align-items: center;
    }
    .footer-link-live {
      color: #52c41a;
      font-weight: 500;
    }
  }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(82, 196, 26, 0); }
  100% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0); }
}

// 弹窗样式
::v-deep .start-recording-dialog {
  border-radius: 12px;
  overflow: hidden;

  .el-dialog__header {
    background: linear-gradient(135deg, #1890ff, #40a9ff);
    color: white;
    padding: 16px 24px;

    .el-dialog__title {
      color: white;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    background: #fafafa;
  }
}
</style>