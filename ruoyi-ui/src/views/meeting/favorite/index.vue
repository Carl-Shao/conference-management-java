<template>
  <div class="meeting-index">
    <!-- ======================= -->
    <!-- 搜索模式 (全屏/独立视图) -->
    <!-- ======================= -->
    <div v-if="isSearchMode" class="search-mode-overlay">
      <!-- 修改点：将筛选栏整合进 search-bar-wrapper -->
      <div class="search-bar-wrapper">
        <!-- 左侧叉号按钮 -->
        <button class="search-close-btn" @click="exitSearch" aria-label="关闭搜索">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        <!-- 搜索框 -->
        <el-input
          ref="searchInputRef"
          v-model="queryParams.title"
          placeholder="搜索会议标题或总结..."
          prefix-icon="el-icon-search"
          class="search-mode-input"
          clearable
          @input="handleSearchInput"
        />

        <!-- 右侧操作区：排序 + 筛选 + 日期 (原 search-filter-bar 内容) -->
        <div class="search-actions-right">
          <div class="action-divider"></div>
          
          <!-- 排序 -->
          <el-dropdown trigger="click" @command="handleSortChange">
            <button class="icon-action-btn sort-button" title="排序">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 6h18v2H3V6zm0 4h12v2H3v-2zm0 4h6v2H3v-2zm0 4h18v2H3v-2z" />
              </svg>
            </button>
            <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
              <el-dropdown-item command="timeDesc"><i class="el-icon-time icon-clr-blue" /><span>按时间正序</span></el-dropdown-item>
              <el-dropdown-item command="timeAsc"><i class="el-icon-time icon-clr-green" /><span>按时间倒序</span></el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>

          <!-- 筛选 -->
          <el-dropdown trigger="click" @command="handleFilterChange">
            <button class="icon-action-btn filter-button" :class="{ 'is-active': queryParams.sourceType }" title="筛选">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16l-8 8v8H8v-8L4 4z" />
              </svg>
            </button>
            <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
              <el-dropdown-item command="all">
                <svg class="dropdown-svg-icon" viewBox="0 0 48 48" fill="none"><path d="M14 6H30L38 14V38C38 40.2091 36.2091 42 34 42H14C11.7909 42 10 40.2091 10 38V10C10 7.79086 11.7909 6 14 6Z" stroke="#909399" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" /><path d="M30 6V14H38" stroke="#909399" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" /><line x1="16" y1="22" x2="32" y2="22" stroke="#909399" stroke-width="2.5" stroke-linecap="round" /><line x1="16" y1="28" x2="28" y2="28" stroke="#909399" stroke-width="2.5" stroke-linecap="round" /><line x1="16" y1="34" x2="24" y2="34" stroke="#909399" stroke-width="2.5" stroke-linecap="round" /></svg>
                <span>全部纪要</span>
              </el-dropdown-item>
              <el-dropdown-item command="0">
                <svg class="dropdown-svg-icon" viewBox="0 0 46 46" fill="none"><rect x="18" y="6" width="12" height="20" rx="6" fill="#4A7DFF" /><path d="M12 22a12 12 0 0 0 24 0" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" /><line x1="24" y1="34" x2="24" y2="40" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" /><line x1="18" y1="40" x2="30" y2="40" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" /></svg>
                <span>录制音频文件</span>
              </el-dropdown-item>
              <el-dropdown-item command="1">
                <svg class="dropdown-svg-icon" viewBox="0 0 26 26" fill="none"><path d="M12 4v12" stroke="#67C23A" stroke-width="2.2" stroke-linecap="round" /><path d="M8 9l4-5 4 5" stroke="#67C23A" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" /><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" stroke="#67C23A" stroke-width="2.2" stroke-linecap="round" /></svg>
                <span>上传音频文件</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>

          <!-- 日期选择器组 -->
          <div class="date-picker-group">
            <el-date-picker
              v-model="beginTime"
              type="date"
              placeholder="开始日期"
              value-format="yyyy-MM-dd HH:mm:ss"
              :default-time="'00:00:00'"
              size="mini"
              class="single-date-picker"
              @change="handleDateChange"
            />
            <span class="date-separator">-</span>
            <el-date-picker
              v-model="endTime"
              type="date"
              placeholder="结束日期"
              value-format="yyyy-MM-dd HH:mm:ss"
              :default-time="'23:59:59'"
              size="mini"
              class="single-date-picker"
              @change="handleDateChange"
            />
          </div>
        </div>
      </div>

      <!-- 搜索结果列表 -->
      <div class="search-results">
        <template v-if="hasAnyFilter">
          <div class="results-hint">
            找到 {{ meetingList.length }} 条相关纪要
          </div>

          <div class="meeting-list search-result-list">
            <div v-for="item in meetingList" :key="item.id" class="meeting-card search-result-card"
              :class="{ 'is-clicking': clickingId === item.id }" @click="handleCardClick($event, item)">
              <div class="card-icon">
                <i v-if="item.isFavorite" class="favorite-badge el-icon-star-on" />
                <svg v-if="item.sourceType === 'record'" viewBox="0 0 48 48" fill="none">
                  <path
                    d="M24 14C22.3431 14 21 15.3431 21 17V25C21 26.6569 22.3431 28 24 28C25.6569 28 27 26.6569 27 25V17C27 15.3431 25.6569 14 24 14Z"
                    fill="#4A7DFF" />
                  <path d="M19 25C19 27.7614 21.2386 30 24 30C26.7614 30 29 27.7614 29 25" stroke="#4A7DFF"
                    stroke-width="2" stroke-linecap="round" />
                  <line x1="24" y1="30" x2="24" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                  <line x1="21" y1="34" x2="27" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                </svg>
                <svg v-else viewBox="0 0 48 48" fill="none">
                  <path d="M24 16V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round" />
                  <path d="M19 21L24 16L29 21" stroke="#67C23A" stroke-width="2" stroke-linecap="round"
                    stroke-linejoin="round" />
                  <path d="M16 28V32C16 33.1046 16.8954 34 18 34H30C31.1046 34 32 33.1046 32 32V28" stroke="#67C23A"
                    stroke-width="2" stroke-linecap="round" />
                </svg>
              </div>
              <div class="card-info">
                <h3 class="card-title" v-html="highlightText(item.title, queryParams.title)" />
                <p class="card-meta">
                  <span>{{ item.duration }}</span>
                  <span class="meta-divider">·</span>
                  <span>{{ item.createTime }}</span>
                </p>
              </div>
            </div>

            <div v-if="!meetingList.length && !loading" class="empty-state">
              <p>未找到符合条件的纪要</p>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="search-empty-hint">
            <p>输入关键词或选择筛选条件开始搜索</p>
          </div>
        </template>
      </div>
    </div>

    <!-- ======================= -->
    <!-- 普通模式 (默认视图) - 保持不变 -->
    <!-- ======================= -->
    <div v-else class="normal-mode-view">
      <!-- ... 原有普通模式代码保持不变 ... -->
      <div class="top-search-bar">
        <el-input
          v-model="queryParams.title"
          placeholder="搜索会议纪要..."
          prefix-icon="el-icon-search"
          class="meeting-search"
          clearable
          @input="handleSearchInput"
          @click.native.prevent="enterSearchMode"
        />
      </div>

      <!-- 2. 内容区：标题 + 列表 -->
      <div class="content-section">
        <h2 class="section-title">我的收藏</h2>

        <div class="meeting-list">
          <div v-for="item in meetingList" :key="item.id" class="meeting-card" :class="{ 'is-clicking': clickingId === item.id }"
            @click="handleCardClick($event, item)">
            <!-- 左侧图标 -->
            <div class="card-icon">
              <i v-if="item.isFavorite" class="favorite-badge el-icon-star-on" />
              <svg v-if="item.sourceType === 'record'" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M24 14C22.3431 14 21 15.3431 21 17V25C21 26.6569 22.3431 28 24 28C25.6569 28 27 26.6569 27 25V17C27 15.3431 25.6569 14 24 14Z"
                  fill="#4A7DFF" />
                <path d="M19 25C19 27.7614 21.2386 30 24 30C26.7614 30 29 27.7614 29 25" stroke="#4A7DFF"
                  stroke-width="2" stroke-linecap="round" />
                <line x1="24" y1="30" x2="24" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                <line x1="21" y1="34" x2="27" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
              </svg>
              <svg v-else viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M24 16V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round" />
                <path d="M19 21L24 16L29 21" stroke="#67C23A" stroke-width="2" stroke-linecap="round"
                  stroke-linejoin="round" />
                <path d="M16 28V32C16 33.1046 16.8954 34 18 34H30C31.1046 34 32 33.1046 32 32V28" stroke="#67C23A"
                  stroke-width="2" stroke-linecap="round" />
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

            <!-- 右侧更多操作 -->
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, item)">
              <i class="el-icon-more card-more" />
              <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
                <el-dropdown-item command="download"><i class="el-icon-download icon-clr-green" />
                  <span>下载</span></el-dropdown-item>
                <el-dropdown-item :command="item.isFavorite ? 'removeFavorite' : 'addFavorite'">
                  <i :class="['icon-clr-yellow', item.isFavorite ? 'el-icon-star-on' : 'el-icon-star-off']" />
                  <span>{{ item.isFavorite ? '从收藏列表移除' : '添加到收藏' }}</span>
                </el-dropdown-item>
                <el-dropdown-item command="move"><i class="el-icon-folder-opened icon-clr-blue" />
                  <span>移动到</span></el-dropdown-item>
                <el-dropdown-item command="rename"><i class="el-icon-edit icon-clr-purple" />
                  <span>重命名</span></el-dropdown-item>
                <el-dropdown-item command="merge"><i class="el-icon-document-copy icon-clr-orange" />
                  <span>合并</span></el-dropdown-item>
                <el-dropdown-item command="delete"><i class="el-icon-delete icon-clr-red" />
                  <span>删除</span></el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>

          <div v-if="!meetingList.length && !loading" class="empty-state-wrapper">
            <div class="empty-illustration">
              <div class="folder-wrap" aria-hidden="true">
                <svg viewBox="0 0 200 160">
                  <defs>
                    <linearGradient id="empty-back" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0" stop-color="#dfe4ea" />
                      <stop offset="1" stop-color="#c2c8d2" />
                    </linearGradient>
                    <linearGradient id="empty-front" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0" stop-color="#eef1f5" />
                      <stop offset="1" stop-color="#cfd6df" />
                    </linearGradient>
                    <linearGradient id="empty-qg" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0" stop-color="#8194a9" />
                      <stop offset="1" stop-color="#56697f" />
                    </linearGradient>
                    <filter id="empty-ds" x="-30%" y="-30%" width="160%" height="160%">
                      <feDropShadow dx="0" dy="8" stdDeviation="7" flood-color="#3a4658" flood-opacity=".18" />
                    </filter>
                  </defs>
                  <!-- 后片 -->
                  <path d="M30 42 q0-9 9-9 h32 l11 13 h72 q9 0 9 9 v62 q0 9-9 9 H39 q-9 0-9-9 z"
                    fill="url(#empty-back)" />
                  <!-- 问号 -->
                  <g class="qmark" filter="url(#empty-ds)">
                    <text x="148" y="66" font-size="62" font-weight="800" fill="#3f4f63" font-family="Arial"
                      opacity=".35" transform="translate(2,3)">?</text>
                    <text x="148" y="66" font-size="62" font-weight="800" fill="url(#empty-qg)"
                      font-family="Arial">?</text>
                  </g>
                  <!-- 前片 -->
                  <path d="M22 72 q0-7 7-7 h58 l9 11 h76 q7 0 7 7 v40 q0 9-9 9 H31 q-9 0-9-9 z" fill="url(#empty-front)"
                    filter="url(#empty-ds)" />
                  <path d="M24 73 q0-6 6-6 h56 l8 10" fill="none" stroke="#ffffff" stroke-width="2.5"
                    stroke-linecap="round" opacity=".7" />
                </svg>
              </div>
              <p class="empty-text">暂无收藏的会议纪要</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <a ref="downloadLink" style="display:none" />
  </div>
</template>

<script>
// Script 部分完全不需要修改，保持原样即可
import {
  listMeeting,
  getMeetingDetail,
  delMeeting,
  renameMeeting,
  favoriteMeeting,
  moveMeetingToFolder,
  mergeMeetings
} from '@/api/huiyi/minutes'

import { uploadAudio } from '@/api/huiyi/audio'

const SORT_COLUMN_MAP = {
  createTime: 'create_time',
  title: 'title',
  duration: 'duration',
  updateTime: 'update_time'
}

export default {
  name: 'MeetingIndex',
  data() {
    return {
      isSearchMode: false,
      beginTime: undefined, 
      endTime: undefined,   
      clickingId: null,
      meetingList: [],
      loading: false,
      searchDebounceTimer: null,
      queryParams: {
        pageNum: 1,
        pageSize: 999,
        title: undefined,
        sourceType: undefined,
        isFavorite: '1',
        orderByColumn: 'create_time',
        isAsc: 'desc'
      }
    }
  },
  computed: {
    hasAnyFilter() {
      return !!(
        (this.queryParams.title && this.queryParams.title.trim()) ||
        this.queryParams.sourceType ||
        this.beginTime ||
        this.endTime
      )
    }
  },
  created() {
    this.getList()
  },
  beforeDestroy() {
    if (this.searchDebounceTimer) clearTimeout(this.searchDebounceTimer)
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      // 注意：不能写成 params.beginTime / params.endTime 这种顶层key，
      // 后端 MeetingRecord(继承BaseEntity) 没有这两个字段，只在 params 这个 Map 属性里，
      // Spring 绑定Map类型属性要求方括号写法 params[beginTime]=xxx，
      // 顶层key会被Spring直接忽略掉（找不到对应setter，不报错，静默丢弃）
      if (this.beginTime) {
        params['params[beginTime]'] = this.beginTime.length === 10
          ? `${this.beginTime} 00:00:00`
          : this.beginTime
      }
      if (this.endTime) {
        params['params[endTime]'] = this.endTime.length === 10
          ? `${this.endTime} 23:59:59`
          : this.endTime
      }
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === undefined || params[key] === null) delete params[key]
      })
      listMeeting(params).then(response => {
        this.meetingList = (response.rows || []).map(item => ({
          ...item,
          isFavorite: item.isFavorite === '1',
          sourceType: item.sourceType === '0' ? 'record' : 'upload'
        }))
      }).finally(() => { this.loading = false })
    },
    handleSearchInput(val) {
      if (this.searchDebounceTimer) clearTimeout(this.searchDebounceTimer)
      this.searchDebounceTimer = setTimeout(() => {
        this.queryParams.title = val || undefined
        this.getList()
      }, 300)
    },
    handleDateChange() { this.getList() },
    enterSearchMode() {
      this.isSearchMode = true
      this.$nextTick(() => {
        const input = this.$refs.searchInputRef
        if (input) {
          input.focus()
          if (this.queryParams.title) input.select()
        }
      })
    },
    exitSearch() {
      this.isSearchMode = false
      this.queryParams.title = undefined
      this.beginTime = undefined
      this.endTime = undefined
      this.getList()
    },
    handleFileUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      const allowedTypes = ['.mp3', '.wav', '.m4a', '.mp4', '.mov']
      const ext = '.' + file.name.split('.').pop().toLowerCase()
      if (!allowedTypes.includes(ext)) {
        this.$message.error(`不支持的文件格式: ${ext}`)
        this.$refs.fileInput.value = ''
        return
      }
      const loadingInstance = this.$loading({ lock: true, text: `正在上传 "${file.name}"...`, spinner: 'el-icon-loading' })
      uploadAudio([file]).then(response => {
        this.$message.success(`文件 "${file.name}" 上传成功`)
        this.getList()
      }).catch(error => {
        console.error(error)
        this.$message.error(error.msg || '上传失败')
      }).finally(() => {
        loadingInstance.close()
        this.$refs.fileInput.value = ''
      })
    },
    highlightText(text, keyword) {
      if (!keyword || !keyword.trim()) return this.escapeHtml(text)
      const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      return this.escapeHtml(text).replace(new RegExp(`(${this.escapeHtml(escaped)})`, 'gi'), '<span class="search-highlight">$1</span>')
    },
    escapeHtml(str) {
      const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }
      return String(str || '').replace(/[&<>"']/g, c => map[c])
    },
    handleSortChange(command) {
      const map = {
        timeDesc: { col: 'createTime', asc: 'desc' },
        timeAsc:  { col: 'createTime', asc: 'asc' },
        titleAsc: { col: 'title', asc: 'asc' },
        titleDesc:{ col: 'title', asc: 'desc' }
      }
      const config = map[command]
      if (config) {
        this.queryParams.orderByColumn = SORT_COLUMN_MAP[config.col] || config.col
        this.queryParams.isAsc = config.asc
        this.getList()
      }
    },
    handleFilterChange(command) {
      this.queryParams.sourceType = (command === 'all') ? undefined : command
      this.getList()
    },
    handleCommand(command, row) {
      switch (command) {
        case 'delete':
          this.$confirm('确定删除该会议纪要吗？删除后不可恢复。', '提示', { type: 'warning' }).then(() => delMeeting([row.meetingId])).then(() => { this.$message.success('已删除'); this.getList() }).catch(() => {})
          break
        case 'download':
          { const link = this.$refs.downloadLink; link.href = row.downloadUrl || `/huiyi/meeting/download/${row.meetingId}`; link.download = `${row.title}.mp3`; link.click() }
          break
        case 'addFavorite':
          favoriteMeeting(row.meetingId, true).then(() => { row.isFavorite = true; this.$message.success('已添加到收藏') })
          break
        case 'removeFavorite':
          favoriteMeeting(row.meetingId, false).then(() => { row.isFavorite = false; this.$message.success('已从收藏列表移除') })
          break
        case 'rename':
          this.$prompt('请输入新名称', '重命名', { confirmButtonText: '确定', cancelButtonText: '取消', inputValue: row.title }).then(({ value }) => { if (!value || !value.trim()) return; renameMeeting(row.meetingId, value.trim()).then(() => { this.$message.success('重命名成功'); this.getList() }) }).catch(() => {})
          break
        case 'move': this.$message.info('移动功能待对接文件夹选择器'); break
        case 'merge': this.$message.info('合并功能待对接会议选择器'); break
      }
    },
    handleCardClick(event, item) {
      const target = event.target
      if (target.closest('.el-dropdown') || target.classList.contains('card-more')) return
      this.$router.push(`/meeting/detail/${item.meetingId}`)
    },
    startRecording() {
      let backRoute = ''
      try { backRoute = JSON.stringify({ name: this.$route.name, path: this.$route.path, query: this.$route.query || {} }) } catch (e) { backRoute = this.$route.path }
      this.$router.push({ path: '/meeting/record', query: { backRoute } })
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-index {
  padding: 24px;
  background: #ffffff;
  position: relative;
  z-index: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  box-sizing: border-box;
}

.search-mode-overlay {
  position: relative;
  top: 0; left: 0; right: 0; bottom: 0;
  background: #ffffff;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  animation: searchSlideIn 0.2s ease-out;
}

@keyframes searchSlideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ⭐️ 修改：整合后的搜索栏容器 */
.search-bar-wrapper {
  display: flex; /* 改为 Flex 布局 */
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f1f3; /* 添加底部分割线代替原来的 filter-bar */
}

.search-close-btn {
  width: 32px; height: 32px;
  border: none;
  background: #f5f6f8;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
  color: #606266;

  svg { width: 18px; height: 18px; }
  &:hover { background: #e8eaed; color: #303133; }
  &:active { transform: scale(0.92); }
}

/* ⭐️ 修改：搜索框自适应宽度 */
.search-mode-input {
  flex: 1; 
  min-width: 200px; /* 防止过窄 */

  ::v-deep .el-input__inner {
    height: 36px;
    line-height: 36px;
    border-radius: 18px;
    border: none;
    background-color: #f5f6f8;
    font-size: 14px;
    padding-left: 40px;
    color: #303133;

    &::placeholder { color: #b0b3b8; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }

  ::v-deep .el-input__prefix {
    left: 12px;
    font-size: 14px;
    line-height: 36px;
  }
  
  ::v-deep .el-input__suffix {
    line-height: 36px;
  }
}

/* ⭐️ 新增：右侧操作区容器 */
.search-actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.action-divider {
  width: 1px;
  height: 20px;
  background-color: #e4e7ed;
  margin: 0 4px;
}

.date-picker-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

/* 独立日期选择器样式 - 稍微调小以适应单行 */
.single-date-picker {
  width: 130px; 

  ::v-deep .el-input__inner {
    height: 32px;
    line-height: 32px;
    border-radius: 16px;
    border: none;
    background-color: #f5f6f8;
    font-size: 13px;
    padding-left: 32px;
    color: #303133;

    &::placeholder { color: #b0b3b8; }
    &:hover { background-color: #edeef0; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }

  ::v-deep .el-input__prefix {
    left: 8px;
    font-size: 14px;
    line-height: 32px;
  }
}

.search-empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  p { font-size: 15px; color: #b0b3b8; }
}

/* ========== 搜索结果专属样式 ========== */
.search-result-list { gap: 0; }

.search-result-card {
  padding: 0px 24px;
  border-radius: 0;
  background: transparent;
  transition: opacity 0.2s ease, background-color 0.2s ease;
  margin: 0;

  & + .search-result-card {
    border-top: 1px solid #e8e9ea;
    border-radius: 0;
  }

  &:hover {
    background: #ffffff !important;
    opacity: 0.55;
  }

  .card-icon, .card-info { pointer-events: none; }
}

.search-results {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.results-hint {
  padding: 12px 24px 4px;
  font-size: 13px;
  color: #909399;
}

/* ========== 普通模式样式 (保持不变) ========== */
.normal-mode-view {
  flex: 1;
  min-height: 0vh;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.top-search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  flex-shrink: 0;
  z-index: 99;
}

.section-title {
  margin: 0 0 16px 0;
  margin-left: 20px;
  font-size: 25px;
  font-weight: 600;
  color: #5a606b;
  line-height: 1.4;
}

.meeting-search {
  margin-left: 20px;
  max-width: 360px;

  ::v-deep .el-input__inner {
    height: 30px;
    line-height: 40px;
    border-radius: 14px;
    border: none;
    background-color: #ececf4;
    color: #303133;
    font-size: 14px;
    padding-left: 40px;
    transition: background-color 0.2s;
    &::placeholder { color: #b0b3b8; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }
  ::v-deep .el-input__prefix { left: 8px; font-size: 14px; line-height: 32px; transition: none; }
  ::v-deep .el-input__prefix .el-icon-search { font-size: 14px; line-height: inherit; }
}

::v-deep .search-highlight {
  color: #4A7DFF;
  text-shadow: 0 0 6px rgba(74, 125, 255, 0.45);
  font-weight: 600;
  background: transparent;
}

.action-buttons-group {
  display: flex;
  align-items: center;
  gap: 28px;
  padding-right: 20px;
  position: relative;
  z-index: 100;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 20px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(180deg, #3b86ff, #2f7bff);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 30px rgba(31, 107, 240, 0.28);
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  z-index: 101;  
  overflow: visible;
  svg { width: 20px; height: 20px; flex-shrink: 0; }
  &:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(31, 107, 240, 0.4); z-index: 102; }
  &:active { transform: translateY(-1px); }
}

.middle-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;       
  margin-right: 12px;      
}

.icon-action-btn {
  z-index: 100;
  width: 36px; /* 稍微缩小以适应搜索栏 */
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #5a606b;
  transition: background 0.2s, transform 0.2s;
  svg { width: 20px; height: 20px; }
  &:hover { background: rgba(20, 24, 40, 0.06); transform: rotate(-15deg); }
  &.is-active { color: #2f7bff; background: rgba(47, 123, 255, 0.08); }
}

.meeting-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-bottom: 120px;
  scrollbar-width: thin;
  scrollbar-color: #dcdfe6 transparent;
  -webkit-overflow-scrolling: touch;
}
.meeting-list::-webkit-scrollbar { width: 6px; }
.meeting-list::-webkit-scrollbar-track { background: transparent; }
.meeting-list::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 10px; }
.meeting-list::-webkit-scrollbar-thumb:hover { background: #c0c4cc; }

.content-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.meeting-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border: none;
  box-shadow: none;
  border-radius: 16px;
  transition: background 0.2s ease;
  cursor: pointer;
  &:hover { background: #e6f0ff; }
  &.is-clicking { background: #d6e8ff !important; transform: scale(0.97); transition: all 0.1s ease-out; }
  .el-dropdown { position: relative; z-index: 2; }
}

.search-result-card.is-clicking { opacity: 0.4 !important; background: #f0f7ff !important; transition: all 0.1s ease-out; }

.card-icon {
  flex-shrink: 0;
  width: 64px;
  height: 48px;
  margin-right: 16px;
  position: relative;
  svg { width: 100%; height: 100%; }
  .favorite-badge { position: absolute; top: 2px; left: 2px; font-size: 17px; color: #e6a23c; z-index: 1; pointer-events: none; filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.15)); }
}

.card-info { flex: 1; min-width: 0; }
.card-title { margin: 0 0 6px 0; font-size: 16px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-meta { margin: 0; font-size: 13px; color: #909399; .meta-divider { margin: 0 6px; } }
.card-more { flex-shrink: 0; font-size: 20px; color: #909399; padding: 8px; border-radius: 8px; cursor: pointer; transition: all 0.2s; &:hover { background: #f5f6f8; color: #606266; } }

.empty-state-wrapper {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 24px;
}
.empty-illustration { display: flex; flex-direction: column; align-items: center; gap: 18px; animation: emptyFadeIn 0.5s ease-out; }
.folder-wrap { width: 170px; height: 140px; animation: emptyFloat 5s ease-in-out infinite; svg { width: 100%; height: 100%; overflow: visible; } }
.qmark { transform-box: fill-box; transform-origin: center; animation: emptyWiggle 4s ease-in-out infinite; }
.empty-text { font-size: 22px; font-weight: 500; color: #9aa1ad; letter-spacing: 0.3px; }

@keyframes emptyFadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes emptyFloat { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-12px); } }
@keyframes emptyWiggle { 0%, 100% { transform: rotate(0); } 25% { transform: rotate(8deg); } 75% { transform: rotate(-8deg); } }

.fab-btn {
  position: absolute; flex-shrink: 0; left: 50%; bottom: 50px; transform: translateX(-50%);
  display: inline-flex; align-items: center; justify-content: center; gap: 12px;
  padding: 18px 116px; min-width: 220px;
  background: linear-gradient(135deg, #4a7dff 0%, #3b6de6 100%);
  color: #fff; border-radius: 40px; font-size: 20px; font-weight: 600; cursor: pointer;
  box-shadow: 0 6px 20px rgba(74, 125, 255, 0.45); transition: all 0.25s ease; z-index: 100; user-select: none; white-space: nowrap;
  &:hover { transform: translateX(-50%) translateY(-3px); box-shadow: 0 8px 28px rgba(74, 125, 255, 0.55); }
  &:active { transform: translateX(-50%) translateY(0); }
  i { font-size: 24px; }
}
</style>

<!-- 全局下拉菜单样式 -->
<style lang="scss">
.app-main { overflow: visible !important; }
.meeting-page-wrapper .app-main, .app-main:has(.meeting-index) { overflow: visible !important; }

.custom-action-dropdown.el-dropdown-menu {
  border-radius: 20px !important; padding: 6px 0 !important; overflow: hidden;
  .el-dropdown-menu__item {
    font-size: 15px !important; line-height: 22px !important; padding: 10px 20px !important; color: #303133 !important;
    i { font-size: 18px !important; margin-right: 8px; vertical-align: middle; display: inline-flex; align-items: center; height: 22px; }
    span { vertical-align: middle; }
    &:hover, &:focus { background-color: #f5f7fa !important; color: #303133 !important; }
  }
  .icon-clr-green { color: #67c23a !important; }
  .icon-clr-yellow { color: #e6a23c !important; }
  .icon-clr-blue { color: #409eff !important; }
  .icon-clr-purple { color: #9b59b6 !important; }
  .icon-clr-orange { color: #ff8c00 !important; }
  .icon-clr-red { color: #f56c6c !important; }
  .el-dropdown-menu__item--divided:before { margin: 4px 20px !important; }
  .dropdown-svg-icon { width: 26px; height: 26px; margin-right: 10px; vertical-align: middle; display: inline-flex; align-items: center; flex-shrink: 0; }
}
</style>