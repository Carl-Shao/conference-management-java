<template>
  <div class="meeting-home">
    <!-- 顶部导航栏 -->
    <div class="home-header">
      <div class="header-left">
        <i class="el-icon-video-camera-solid header-logo"></i>
        <span class="header-title">会议纪要</span>
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
          icon="el-icon-video-camera"
          class="start-btn"
          @click="openStartDialog"
        >发起会议</el-button>
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
        <i class="el-icon-chat-line-square empty-icon"></i>
        <p>暂无会议，点击右上角「发起会议」开始第一场会议吧</p>
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
            <i class="el-icon-time"></i>
            <span>{{ item.startTime }}</span>
          </div>
          <div class="card-meta" v-if="item.duration">
            <i class="el-icon-stopwatch"></i>
            <span>时长 {{ item.duration }}</span>
          </div>
          <div class="card-meta" v-if="item.roomName">
            <i class="el-icon-office-building"></i>
            <span>{{ item.roomName }}</span>
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

    <!-- 发起会议弹窗 -->
    <el-dialog
      title="发起会议"
      :visible.sync="startDialogVisible"
      width="480px"
      custom-class="start-meeting-dialog"
    >
      <el-form :model="startForm" :rules="startRules" ref="startForm" label-width="80px">
        <el-form-item label="会议主题" prop="title">
          <el-input v-model="startForm.title" placeholder="请输入会议主题，例如：周例会" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="所在会议室" prop="roomName">
          <el-input v-model="startForm.roomName" placeholder="请输入会议室名称（选填）" maxlength="30" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="startDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="starting" @click="confirmStart">立即发起</el-button>
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
        title: '',
        roomName: ''
      },
      startRules: {
        title: [{ required: true, message: '请输入会议主题', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
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
      this.startForm = { title: '', roomName: '' }
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
          this.$router.push({ path: '/huiyi/meeting/room/' + meetingId })
        }).catch(() => {
          this.starting = false
        })
      })
    },
    handleEnterMeeting(item) {
      if (item.status === 'ended') {
        this.$router.push({ path: '/huiyi/meeting/detail/' + item.meetingId })
      } else {
        this.$router.push({ path: '/huiyi/meeting/room/' + item.meetingId })
      }
    },
    handleCommand(command, item) {
      if (command === 'detail') {
        this.$router.push({ path: '/huiyi/meeting/detail/' + item.meetingId })
      } else if (command === 'delete') {
        this.$modal.confirm('确认删除会议「' + item.title + '」吗？删除后无法恢复。').then(() => {
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
  background: #f5f6f8;
  padding-bottom: 24px;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

  .header-left {
    display: flex;
    align-items: center;
    .header-logo {
      font-size: 24px;
      color: #1890ff;
      margin-right: 8px;
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
      width: 220px;
      margin-right: 16px;
    }
    .start-btn {
      background: #1890ff;
      border-color: #1890ff;
      border-radius: 6px;
      font-weight: 500;
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
}

.home-body {
  padding: 20px 24px;
}

.empty-block {
  text-align: center;
  padding: 80px 0;
  color: #909399;
  .empty-icon {
    font-size: 48px;
    color: #c0c4cc;
    margin-bottom: 12px;
    display: block;
  }
}

.meeting-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.meeting-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
  border: 1px solid #f0f0f0;

  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .card-top {
    display: flex;
    align-items: center;
    margin-bottom: 10px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #c0c4cc;
      margin-right: 6px;
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
      padding: 2px 6px;
      &:hover { color: #1890ff; }
    }
  }

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 10px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-meta {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #86909c;
    margin-bottom: 4px;
    i {
      margin-right: 4px;
    }
  }

  .card-footer {
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px dashed #f0f0f0;
    .footer-link {
      font-size: 13px;
      color: #1890ff;
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
</style>