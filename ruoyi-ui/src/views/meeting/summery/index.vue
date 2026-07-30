<template>
  <div class="meeting-index">
    <!-- 顶部操作栏 -->
    <div class="meeting-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索会议纪要..."
        prefix-icon="el-icon-search"
        class="meeting-search"
        clearable
      />
      <el-dropdown trigger="click" @command="handleSortChange">
        <span class="sort-trigger">
          {{ currentSortLabel }}
          <i class="el-icon-arrow-down el-icon--right" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="timeDesc">最近生成</el-dropdown-item>
          <el-dropdown-item command="timeAsc">最早生成</el-dropdown-item>
          <el-dropdown-item command="titleAsc">按标题排序</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 列表内容区 -->
    <div class="meeting-list">
      <div
        v-for="item in meetingList"
        :key="item.id"
        class="meeting-card"
      >
        <!-- 左侧图标：底色全白、无边框 -->
        <div class="card-icon">
          <!-- 录制音频生成纪要图标 -->
          <svg v-if="item.fileType === 'record'" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="#ffffff"/>
            <path d="M24 14C22.3431 14 21 15.3431 21 17V25C21 26.6569 22.3431 28 24 28C25.6569 28 27 26.6569 27 25V17C27 15.3431 25.6569 14 24 14Z" fill="#4A7DFF"/>
            <path d="M19 25C19 27.7614 21.2386 30 24 30C26.7614 30 29 27.7614 29 25" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round"/>
            <line x1="24" y1="30" x2="24" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round"/>
            <line x1="21" y1="34" x2="27" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <!-- 上传音频生成纪要图标 -->
          <svg v-else viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="#ffffff"/>
            <path d="M24 16V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round"/>
            <path d="M19 21L24 16L29 21" stroke="#67C23A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16 28V32C16 33.1046 16.8954 34 18 34H30C31.1046 34 32 33.1046 32 32V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>

        <!-- 中间信息 -->
        <div class="card-info">
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-meta">
            <span>{{ item.duration }}</span>
            <span class="meta-divider">·</span>
            <span>{{ item.createTime }}</span>
          </p>
        </div>

        <!-- 右侧操作下拉菜单 -->
        <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, item)">
          <i class="el-icon-more card-more" />
          <el-dropdown-menu slot="dropdown">
            <!-- 下载项：绿色 -->
            <el-dropdown-item command="download" class="download-item">
              <i class="el-icon-download download-icon" /> <span class="download-text">下载</span>
            </el-dropdown-item>
            <el-dropdown-item :command="item.isFavorite ? 'removeFavorite' : 'addFavorite'">
              <i :class="item.isFavorite ? 'el-icon-star-on' : 'el-icon-star-off'" />
              {{ item.isFavorite ? '从收藏列表移除' : '添加到收藏' }}
            </el-dropdown-item>
            <el-dropdown-item command="move">
              <i class="el-icon-folder-opened" /> 移动到
            </el-dropdown-item>
            <el-dropdown-item command="rename">
              <i class="el-icon-edit" /> 重命名
            </el-dropdown-item>
            <el-dropdown-item command="merge">
              <i class="el-icon-document-copy" /> 合并
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided>
              <i class="el-icon-delete" style="color:#F56C6C" />
              <span style="color:#F56C6C">删除</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>

      <!-- 空状态占位 -->
      <div v-if="!meetingList.length" class="empty-state">
        <p>暂无会议纪要</p>
      </div>
    </div>

    <!-- 悬浮按钮：相对于 .meeting-index 内容区居中，加长尺寸 -->
    <div class="fab-btn" @click="startRecording">
      <i class="el-icon-microphone" />
      <span>开始听记</span>
    </div>

    <!-- 隐藏下载锚点 -->
    <a ref="downloadLink" style="display:none" />
  </div>
</template>

<script>
export default {
  name: 'MeetingIndex',
  data() {
    return {
      searchKeyword: '',
      sortType: 'timeDesc',
      meetingList: [
        {
          id: 1,
          title: '产品规划会议',
          fileType: 'record',
          duration: '45:20',
          createTime: '2026-07-30 14:30',
          isFavorite: false,
          downloadUrl: '/api/meeting/download/1'
        },
        {
          id: 2,
          title: '团队周例会',
          fileType: 'upload',
          duration: '32:15',
          createTime: '2026-07-29 10:00',
          isFavorite: true,
          downloadUrl: '/api/meeting/download/2'
        },
        {
          id: 3,
          title: 'Q3 OKR 对齐会',
          fileType: 'record',
          duration: '58:40',
          createTime: '2026-07-28 16:20',
          isFavorite: false,
          downloadUrl: '/api/meeting/download/3'
        }
      ]
    }
  },
  computed: {
    currentSortLabel() {
      const map = {
        timeDesc: '最近生成',
        timeAsc: '最早生成',
        titleAsc: '按标题排序'
      }
      return map[this.sortType] || '最近生成'
    }
  },
  methods: {
    handleSortChange(command) {
      this.sortType = command
      console.log('排序变更:', command)
    },
    handleCommand(command, row) {
      switch (command) {
        case 'download':
          this.triggerDownload(row)
          break
        case 'addFavorite':
          row.isFavorite = true
          this.$message.success('已添加到收藏')
          break
        case 'removeFavorite':
          row.isFavorite = false
          this.$message.success('已从收藏列表移除')
          break
        case 'move':
          console.log('移动到:', row.id)
          break
        case 'rename':
          console.log('重命名:', row.id)
          break
        case 'merge':
          console.log('合并:', row.id)
          break
        case 'delete':
          this.$confirm('确定删除该会议纪要吗？删除后不可恢复。', '提示', {
            type: 'warning'
          }).then(() => {
            console.log('删除:', row.id)
            this.$message.success('删除成功')
          }).catch(() => {})
          break
        default:
          break
      }
    },
    triggerDownload(row) {
      const link = this.$refs.downloadLink
      link.href = row.downloadUrl
      link.download = `${row.title}.mp3`
      link.click()
    },
    startRecording() {
      this.$router.push('/meeting/record')
    }
  }
}
</script>

<style lang="scss" scoped>
/* ====== 页面容器：纯白背景，作为按钮定位参照 ====== */
.meeting-index {
  padding: 24px;
  min-height: calc(100vh - 130px);
  background: #ffffff;
  position: relative; /* 关键：为 fab-btn 提供定位上下文 */
}

/* ====== 顶部操作栏 ====== */
.meeting-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.meeting-search {
  max-width: 360px;

  ::v-deep .el-input__inner {
    height: 40px;
    line-height: 40px;
    border-radius: 20px;
    border: none;
    background-color: #f5f6f8;
    color: #303133;
    font-size: 14px;
    padding-left: 40px;
    transition: background-color 0.2s;

    &::placeholder {
      color: #b0b3b8;
    }

    &:focus {
      background-color: #edeef0;
      box-shadow: none;
    }
  }

  ::v-deep .el-input__prefix {
    left: 14px;
  }

  ::v-deep .el-icon-search {
    color: #b0b3b8;
    font-size: 16px;
  }
}

.sort-trigger {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
  user-select: none;

  &:hover {
    background: #f5f6f8;
  }

  i {
    margin-left: 4px;
    font-size: 12px;
  }
}

/* ====== 列表卡片：无边框、无阴影 ====== */
.meeting-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-bottom: 100px;
}

.meeting-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border: none;       /* 移除边框 */
  box-shadow: none;   /* 移除阴影 */
  border-radius: 16px;
  transition: background 0.2s ease;
  cursor: pointer;

  &:hover {
    background: #fafbfc; /* hover 仅用极淡灰底区分，不加边框阴影 */
  }
}

.card-icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  margin-right: 16px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  margin: 0;
  font-size: 13px;
  color: #909399;

  .meta-divider {
    margin: 0 6px;
  }
}

.card-more {
  flex-shrink: 0;
  font-size: 20px;
  color: #909399;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f5f6f8;
    color: #606266;
  }
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #c0c4cc;
  font-size: 14px;
}

/* ====== 下载项绿色样式（scoped 穿透） ====== */
::v-deep .download-item {
  .download-icon,
  .download-text {
    color: #67c23a !important;
  }
}

/* ====== 悬浮按钮：相对内容区居中 + 加长 ====== */
.fab-btn {
  position: absolute;      /* 改为 absolute，相对于 .meeting-index 定位 */
  left: 50%;
  bottom: -30px;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  /* 在上次基础上进一步加长：padding 横向加大，min-width 保证最小宽度 */
  padding: 20px 106px;
  min-width: 220px;
  background: linear-gradient(135deg, #4a7dff 0%, #3b6de6 100%);
  color: #fff;
  border-radius: 40px;
  font-size: 20px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(74, 125, 255, 0.45);
  transition: all 0.25s ease;
  z-index: 100;
  user-select: none;
  white-space: nowrap;

  &:hover {
    transform: translateX(-50%) translateY(-3px);
    box-shadow: 0 8px 28px rgba(74, 125, 255, 0.55);
  }

  &:active {
    transform: translateX(-50%) translateY(0);
  }

  i {
    font-size: 24px;
  }
}
</style>