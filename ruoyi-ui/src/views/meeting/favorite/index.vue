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
      <div v-for="item in meetingList" :key="item.id" class="meeting-card">

        <!-- ✅ 左侧图标（已加入收藏角标） -->
        <div class="card-icon">
          <!-- 新增：收藏小五角星，仅收藏时显示 -->
          <i v-if="item.isFavorite" class="favorite-badge el-icon-star-on" />

          <!-- 原有录音图标 -->
          <svg v-if="item.fileType === 'record'" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- <rect width="48" height="48" rx="12" fill="#ffffff"/> -->
            <path d="M24 14C22.3431 14 21 15.3431 21 17V25C21 26.6569 22.3431 28 24 28C25.6569 28 27 26.6569 27 25V17C27 15.3431 25.6569 14 24 14Z" fill="#4A7DFF"/>
            <path d="M19 25C19 27.7614 21.2386 30 24 30C26.7614 30 29 27.7614 29 25" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round"/>
            <line x1="24" y1="30" x2="24" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round"/>
            <line x1="21" y1="34" x2="27" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round"/>
          </svg>

          <!-- 原有上传图标 -->
          <svg v-else viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- <rect width="48" height="48" rx="12" fill="#ffffff"/> -->
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

        <!-- 右侧更多操作下拉菜单 -->
        <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, item)">
          <i class="el-icon-more card-more" />
          <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
            <el-dropdown-item command="download">
              <i class="el-icon-download icon-clr-green" /> <span>下载</span>
            </el-dropdown-item>
            <el-dropdown-item :command="item.isFavorite ? 'removeFavorite' : 'addFavorite'">
              <i :class="['icon-clr-yellow', item.isFavorite ? 'el-icon-star-on' : 'el-icon-star-off']" />
              <span>{{ item.isFavorite ? '从收藏列表移除' : '添加到收藏' }}</span>
            </el-dropdown-item>
            <el-dropdown-item command="move">
              <i class="el-icon-folder-opened icon-clr-blue" /> <span>移动到</span>
            </el-dropdown-item>
            <el-dropdown-item command="rename">
              <i class="el-icon-edit icon-clr-purple" /> <span>重命名</span>
            </el-dropdown-item>
            <el-dropdown-item command="merge">
              <i class="el-icon-document-copy icon-clr-orange" /> <span>合并</span>
            </el-dropdown-item>
            <el-dropdown-item command="delete">
              <i class="el-icon-delete icon-clr-red" /> <span>删除</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>

      </div>

      <div v-if="!meetingList.length" class="empty-state"><p>暂无会议纪要</p></div>
    </div>

    <div class="fab-btn" @click="startRecording">
      <i class="el-icon-microphone" /><span>开始听记</span>
    </div>
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
        { id: 1, title: '产品规划会议', fileType: 'record', duration: '45:20', createTime: '2026-07-30 14:30', isFavorite: false, downloadUrl: '/api/meeting/download/1' },
        { id: 2, title: '团队周例会', fileType: 'upload', duration: '32:15', createTime: '2026-07-29 10:00', isFavorite: true, downloadUrl: '/api/meeting/download/2' },
        { id: 3, title: 'Q3 OKR 对齐会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/3' }
      ]
    }
  },
  computed: {
    currentSortLabel() {
      const map = { timeDesc: '最近生成', timeAsc: '最早生成', titleAsc: '按标题排序' }
      return map[this.sortType] || '最近生成'
    }
  },
  methods: {
    handleSortChange(command) { this.sortType = command },
    handleCommand(command, row) {
      if (command === 'delete') {
        this.$confirm('确定删除该会议纪要吗？删除后不可恢复。', '提示', { type: 'warning' })
          .then(() => this.$message.success('删除成功')).catch(() => {})
      } else if (command === 'download') {
        const link = this.$refs.downloadLink
        link.href = row.downloadUrl
        link.download = `${row.title}.mp3`
        link.click()
      } else if (command === 'addFavorite') {
        row.isFavorite = true; this.$message.success('已添加到收藏')
      } else if (command === 'removeFavorite') {
        row.isFavorite = false; this.$message.success('已从收藏列表移除')
      }
    },
    startRecording() { this.$router.push('/meeting/record') }
  }
}
</script>

<!-- ✅ 原有 scoped 样式完整保留，不做任何删减 -->
<style lang="scss" scoped>
.meeting-index { padding: 24px; min-height: calc(100vh - 130px); background: #ffffff; position: relative; }
.meeting-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.meeting-search {
  max-width: 360px;
  ::v-deep .el-input__inner {
    height: 40px; line-height: 40px; border-radius: 20px; border: none;
    background-color: #f5f6f8; color: #303133; font-size: 14px; padding-left: 40px; transition: background-color 0.2s;
    &::placeholder { color: #b0b3b8; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }
  ::v-deep .el-input__prefix { left: 14px; }
  ::v-deep .el-icon-search { color: #b0b3b8; font-size: 16px; }
}
.sort-trigger {
  display: inline-flex; align-items: center; cursor: pointer; font-size: 14px; color: #606266;
  padding: 8px 12px; border-radius: 8px; transition: background 0.2s; user-select: none;
  &:hover { background: #f5f6f8; }
  i { margin-left: 4px; font-size: 12px; }
}
.meeting-list { display: flex; flex-direction: column; gap: 4px; padding-bottom: 100px; }
.meeting-card {
  display: flex; align-items: center; padding: 16px 20px; background: #ffffff;
  border: none; box-shadow: none; border-radius: 16px; transition: background 0.2s ease; cursor: pointer;
  &:hover { background: #fafbfc; }
}
.card-icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  margin-right: 16px;
  position: relative; /* 为角标提供定位基准 */

  svg {
    width: 100%;
    height: 100%;
  }

  /* 收藏小五角星角标 */
  .favorite-badge {
    position: absolute;
    top: 2px;
    left: 2px;
    font-size: 17px;
    color: #e6a23c;
    z-index: 1;
    pointer-events: none;
    filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.15));
  }
}
.card-info { flex: 1; min-width: 0; }
.card-title { margin: 0 0 6px 0; font-size: 15px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-meta { margin: 0; font-size: 13px; color: #909399; .meta-divider { margin: 0 6px; } }
.card-more {
  flex-shrink: 0; font-size: 20px; color: #909399; padding: 8px; border-radius: 8px; cursor: pointer; transition: all 0.2s;
  &:hover { background: #f5f6f8; color: #606266; }
}
.empty-state { text-align: center; padding: 80px 0; color: #c0c4cc; font-size: 14px; }
.fab-btn {
  position: absolute; left: 50%; bottom: -30px; transform: translateX(-50%);
  display: inline-flex; align-items: center; justify-content: center; gap: 12px;
  padding: 20px 106px; min-width: 220px;
  background: linear-gradient(135deg, #4a7dff 0%, #3b6de6 100%); color: #fff; border-radius: 40px;
  font-size: 20px; font-weight: 600; cursor: pointer; box-shadow: 0 6px 20px rgba(74, 125, 255, 0.45);
  transition: all 0.25s ease; z-index: 100; user-select: none; white-space: nowrap;
  &:hover { transform: translateX(-50%) translateY(-3px); box-shadow: 0 8px 28px rgba(74, 125, 255, 0.55); }
  &:active { transform: translateX(-50%) translateY(0); }
  i { font-size: 24px; }
}
</style>

<!-- ✅ 核心修复：新增非 scoped 全局样式块，专门处理 body 下的 dropdown popper -->
<style lang="scss">
.custom-action-dropdown.el-dropdown-menu {
  /* 4. 圆弧角 20px */
  border-radius: 20px !important;
  padding: 6px 0 !important;
  overflow: hidden;

  .el-dropdown-menu__item {
    /* 2. 适度放大：文字15px + 图标18px，不再用夸张的16px+20px */
    font-size: 15px !important;
    /* 行间距由 line-height 自然控制，不再用超大 padding 撑高度 */
    line-height: 22px !important;
    padding: 10px 20px !important;

    /* 1. 文字强制深色，去除 Element 默认 hover 蓝色 */
    color: #303133 !important;

    i {
      font-size: 18px !important;
      margin-right: 8px;
      vertical-align: middle;
      /* 确保图标与文字基线对齐，避免视觉错位导致间距异常 */
      display: inline-flex;
      align-items: center;
      height: 22px;
    }

    span {
      vertical-align: middle;
    }

    /* hover 仅变背景，文字颜色锁定 */
    &:hover, &:focus {
      background-color: #f5f7fa !important;
      color: #303133 !important;
    }
  }

  /* 3. 图标独立上色（保持不变） */
  .icon-clr-green  { color: #67c23a !important; }
  .icon-clr-yellow { color: #e6a23c !important; }
  .icon-clr-blue   { color: #409eff !important; }
  .icon-clr-purple { color: #9b59b6 !important; }
  .icon-clr-orange { color: #ff8c00 !important; }
  .icon-clr-red    { color: #f56c6c !important; }

  /* 分割线适配修正后的间距 */
  .el-dropdown-menu__item--divided:before {
    margin: 4px 20px !important;
  }
}
</style>