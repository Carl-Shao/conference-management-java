<template>
  <div class="meeting-index">
    <!-- ======================= -->
    <!-- 搜索模式 (全屏/独立视图) -->
    <!-- ======================= -->
    <div v-if="isSearchMode" class="search-mode-overlay">
      <div class="search-bar-wrapper">
        <button class="search-close-btn" @click="exitSearch" aria-label="关闭搜索">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        <el-input
          ref="searchInputRef"
          v-model="queryParams.title"
          placeholder="搜索会议标题或总结..."
          prefix-icon="el-icon-search"
          class="search-mode-input"
          clearable
          @input="handleSearchInput"
        />

        <div class="search-actions-right">
          <div class="action-divider"></div>
          
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
                  <path d="M24 14C22.3431 14 21 15.3431 21 17V25C21 26.6569 22.3431 28 24 28C25.6569 28 27 26.6569 27 25V17C27 15.3431 25.6569 14 24 14Z" fill="#4A7DFF" />
                  <path d="M19 25C19 27.7614 21.2386 30 24 30C26.7614 30 29 27.7614 29 25" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                  <line x1="24" y1="30" x2="24" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                  <line x1="21" y1="34" x2="27" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                </svg>
                <svg v-else viewBox="0 0 48 48" fill="none">
                  <path d="M24 16V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round" />
                  <path d="M19 21L24 16L29 21" stroke="#67C23A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M16 28V32C16 33.1046 16.8954 34 18 34H30C31.1046 34 32 33.1046 32 32V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round" />
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
    <!-- 普通模式 (默认视图) -->
    <!-- ======================= -->
    <div v-else class="normal-mode-view">
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

      <div class="content-section">
        <h2 class="section-title">我的文件夹</h2>
        <div v-loading="folderLoading" class="folder-grid">
          <!-- 新建文件夹 -->
          <div class="folder-card new-folder-card" @click="createNewFolder">
            <div class="folder-top-left"></div>
            <div class="folder-center">
              <svg class="blue-folder-svg" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="rgba(47, 123, 255, 0.8)" stroke-width="2" fill="none" />
                <line x1="12" y1="7" x2="12" y2="17" stroke="rgba(47, 123, 255, 0.8)" stroke-width="2" stroke-linecap="round" />
                <line x1="7" y1="12" x2="17" y2="12" stroke="rgba(47, 123, 255, 0.8)" stroke-width="2" stroke-linecap="round" />
              </svg>
            </div>
            <div class="folder-bottom-left">新建文件夹</div>
          </div>

          <!-- 动态文件夹 -->
          <div 
            v-for="item in folderList" 
            :key="item.id" 
            class="folder-card"
            :class="{ 'clicked': clickedFolderId === item.id }"
            @click="handleFolderClick(item)"
            @animationend="clickedFolderId = null"
          >
            <div class="folder-header">
              <span class="folder-file-count">{{ item.fileCount || 0 }}个文件</span>
              
              <el-dropdown trigger="click" @command="(cmd) => handleFolderCommand(cmd, item)" @click.native.stop>
                <i class="el-icon-more folder-more" @click.stop />
                <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
                  <el-dropdown-item command="rename">
                    <i class="el-icon-edit icon-clr-purple" />
                    <span>重命名</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="delete">
                    <i class="el-icon-delete icon-clr-red" />
                    <span>删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
            <div class="folder-center">
              <svg class="blue-folder-svg" viewBox="0 0 24 24" fill="none"><path d="M5 7C5 6.44772 5.44772 6 6 6H10L12 8H18C18.5523 8 19 8.44772 19 9V19C19 19.5523 18.5523 20 18 20H6C5.44772 20 5 19.5523 5 19V7Z" fill="#2f7bff"/><path d="M10 6L8 4H4C3.44772 4 3 4.44772 3 5V18C3 18.5523 3.44772 19 4 19H6" stroke="#1f6bf0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </div>
            <div class="folder-bottom-left">{{ item.title || item.folderName }}</div>
          </div>

          <div v-if="!folderLoading && !folderList.length" class="empty-state">
            <p></p>
          </div>
        </div>
      </div>
    </div>

    <a ref="downloadLink" style="display:none" />
  </div>
</template>

<script>
import {
  listMeeting,
  getMeetingDetail
} from '@/api/huiyi/minutes'
import { uploadAudio } from '@/api/huiyi/audio'
import { listFolder, getFolder, addFolder, updateFolder, delFolder } from '@/api/huiyi/folder'

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
      // 搜索 & 会议列表状态
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
        orderByColumn: 'create_time',
        isAsc: 'desc'
      },

      // 文件夹状态
      folderLoading: false,
      clickedFolderId: null,
      folderList: []
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
    this.fetchFolderList()
  },
  beforeDestroy() {
    if (this.searchDebounceTimer) clearTimeout(this.searchDebounceTimer)
  },
  methods: {
    /* ==================== 会议列表 - 后端搜索 (完全复用样例) ==================== */
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
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

    handleSortChange(command) {
      const map = {
        timeDesc: { col: 'createTime', asc: 'desc' },
        timeAsc:  { col: 'createTime', asc: 'asc' }
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

    handleCardClick(event, item) {
      const target = event.target
      if (target.closest('.el-dropdown') || target.classList.contains('card-more')) return
      this.clickingId = item.id
      setTimeout(() => {
        this.$router.push(`/meeting/detail/${item.meetingId}`)
        this.$nextTick(() => {
          this.clickingId = null
        })
      }, 200)
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
      uploadAudio([file]).then(() => {
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

    /* ==================== 文件夹操作 ==================== */
    handleFolderCommand(command, item) {
      switch (command) {
        case 'rename': this.renameFolder(item); break
        case 'delete': this.deleteFolder(item); break
      }
    },

    async fetchFolderList() {
      this.folderLoading = true
      try {
        const res = await listFolder({
          sortType: this.queryParams.isAsc === 'desc' ? 'timeDesc' : 'timeAsc',
          filter: this.queryParams.sourceType === undefined ? 'all' : this.queryParams.sourceType
        })
        this.folderList = res.data || res.rows || res.list || []
      } catch (err) {
        console.error('获取文件夹列表失败:', err)
        this.$message.error('加载文件夹失败，请稍后重试')
      } finally {
        this.folderLoading = false
      }
    },

    createNewFolder() {
      this.$prompt('请输入文件夹名称', '新建文件夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '名称不能为空',
        customClass: 'new-folder-dialog'
      }).then(async ({ value }) => {
        try {
          await addFolder({ title: value.trim(), folderName: value.trim() })
          this.$message.success('创建成功')
          await this.fetchFolderList()
        } catch (err) {
          this.$message.error(err.message || '创建失败，请重试')
        }
      }).catch(() => {})
    },

    renameFolder(item) {
      this.$prompt('请输入新的文件夹名称', '重命名文件夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: item.title || item.folderName,
        inputPattern: /\S+/,
        inputErrorMessage: '名称不能为空',
        customClass: 'new-folder-dialog'
      }).then(async ({ value }) => {
        if (value.trim() === (item.title || item.folderName)) return
        try {
          await updateFolder({ id: item.id, title: value.trim(), folderName: value.trim() })
          this.$message.success('重命名成功')
          await this.fetchFolderList()
        } catch (err) {
          this.$message.error(err.message || '重命名失败，请重试')
        }
      }).catch(() => {})
    },

    deleteFolder(item) {
      this.$confirm(
        `确定删除文件夹「${item.title || item.folderName}」吗？文件夹内的会议记录不会被删除，只会移出文件夹。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', customClass: 'new-folder-dialog', 
        distinguishCancelAndClose: true, showClose: false }
      ).then(async () => {
        try {
          await delFolder(item.id)
          this.$message.success('删除成功')
          await this.fetchFolderList()
        } catch (err) {
          this.$message.error(err.message || '删除失败，请重试')
        }
      }).catch(() => {})
    },

    async handleFolderClick(item) {
      this.clickedFolderId = item.folderId

      const detail = await getFolder(item.folderId)
        .then(res => res.data || res)
        .catch(() => null)

      if (!detail) return

      this.$router.push({
        path: '/meeting/folder-detail',
        query: {
          folderId: item.folderId,
          name: item.title || item.folderName
        }
      })
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

/* ================= 搜索模式 ================= */
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

.search-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f1f3;
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

.search-mode-input {
  flex: 1;
  min-width: 200px;
  ::v-deep .el-input__inner {
    height: 36px; line-height: 36px;
    border-radius: 18px; border: none;
    background-color: #f5f6f8; font-size: 14px;
    padding-left: 40px; color: #303133;
    &::placeholder { color: #b0b3b8; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }
  ::v-deep .el-input__prefix { left: 12px; font-size: 14px; line-height: 36px; }
  ::v-deep .el-input__suffix { line-height: 36px; }
}

.search-actions-right {
  display: flex; align-items: center; gap: 12px; flex-shrink: 0;
}
.action-divider { width: 1px; height: 20px; background-color: #e4e7ed; margin: 0 4px; }
.date-picker-group { display: flex; align-items: center; gap: 8px; }
.date-separator { font-size: 13px; color: #909399; flex-shrink: 0; }
.single-date-picker {
  width: 130px;
  ::v-deep .el-input__inner {
    height: 32px; line-height: 32px; border-radius: 16px; border: none;
    background-color: #f5f6f8; font-size: 13px; padding-left: 32px; color: #303133;
    &::placeholder { color: #b0b3b8; }
    &:hover, &:focus { background-color: #edeef0; box-shadow: none; }
  }
  ::v-deep .el-input__prefix { left: 8px; font-size: 14px; line-height: 32px; }
}

.search-empty-hint {
  display: flex; align-items: center; justify-content: center; padding: 80px 20px;
  p { font-size: 15px; color: #b0b3b8; }
}

.search-result-list { gap: 0; }
.search-result-card {
  padding: 0px 24px; border-radius: 0; background: transparent;
  transition: opacity 0.2s ease, background-color 0.2s ease; margin: 0;
  & + .search-result-card { border-top: 1px solid #e8e9ea; border-radius: 0; }
  &:hover { background: #ffffff !important; opacity: 0.55; }
  .card-icon, .card-info { pointer-events: none; }
}
.search-result-card.is-clicking { opacity: 0.4 !important; background: #f0f7ff !important; transition: all 0.1s ease-out; }
.search-results { flex: 1; overflow-y: auto; -webkit-overflow-scrolling: touch; }
.results-hint { padding: 12px 24px 4px; font-size: 13px; color: #909399; }

/* ================= 普通模式 ================= */
.normal-mode-view {
  flex: 1; min-height: 0vh; display: flex; flex-direction: column; overflow: visible;
}

.top-search-bar {
  display: flex; align-items: center; gap: 16px; margin-bottom: 28px; flex-shrink: 0; z-index: 99;
}

.section-title {
  margin: 0 0 16px 0; margin-left: 20px; font-size: 25px; font-weight: 600; color: #5a606b; line-height: 1.4;
}

.meeting-search {
  margin-left: 20px; max-width: 360px;
  ::v-deep .el-input__inner {
    height: 30px; line-height: 40px; border-radius: 14px; border: none;
    background-color: #ececf4; color: #303133; font-size: 14px; padding-left: 40px;
    transition: background-color 0.2s;
    &::placeholder { color: #b0b3b8; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }
  ::v-deep .el-input__prefix { left: 8px; font-size: 14px; line-height: 32px; transition: none; }
  ::v-deep .el-input__prefix .el-icon-search { font-size: 14px; line-height: inherit; }
}

.middle-actions {
  display: flex; align-items: center; gap: 8px; margin-left: auto; margin-right: 12px;
}

.action-buttons-group {
  display: flex; align-items: center; gap: 28px; padding-right: 20px; position: relative; z-index: 100;
}

.upload-btn {
  display: inline-flex; align-items: center; gap: 8px; height: 42px; padding: 0 20px;
  border: none; border-radius: 14px; background: linear-gradient(180deg, #3b86ff, #2f7bff);
  color: #fff; font-size: 16px; font-weight: 600; cursor: pointer;
  box-shadow: 0 10px 30px rgba(31, 107, 240, 0.28); transition: transform 0.2s, box-shadow 0.2s;
  position: relative; z-index: 101; overflow: visible;
  svg { width: 20px; height: 20px; flex-shrink: 0; }
  &:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(31, 107, 240, 0.4); z-index: 102; }
  &:active { transform: translateY(-1px); }
}

.icon-action-btn {
  z-index: 100; width: 36px; height: 36px; border: none; background: transparent; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center; cursor: pointer; color: #5a606b;
  transition: background 0.2s, transform 0.2s;
  svg { width: 20px; height: 20px; }
  &:hover { background: rgba(20, 24, 40, 0.06); transform: rotate(-15deg); }
  &.is-active { color: #2f7bff; background: rgba(47, 123, 255, 0.08); }
}

/* ================= 文件夹网格 ================= */
.folder-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 17px; padding: 0 34px; margin-top: 20px;
}

.folder-card {
  zoom: 0.78;
  display: flex; flex-direction: column; align-items: stretch; justify-content: flex-start;
  padding: 20px; border-radius: 20px; background: white;
  box-shadow: 0 10px 30px rgba(0,0,0,.28); transition: all 0.3s ease;
  cursor: pointer; position: relative; height: 180px; text-align: center;
  &:hover {
    transform: translateY(-3px); box-shadow: 0 6px 20px rgba(31,107,240,.4);
    .folder-more { opacity: 1; }
  }
  &.clicked { animation: clickEffect 0.3s ease; }
}

.folder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 8px;
}
.folder-file-count {
  font-size: 12px;
  color: #9aa1ad;
  line-height: 1;
}

.folder-more {
  position: static; font-size: 18px; color: #909399;
  padding: 4px 8px; border-radius: 8px; cursor: pointer; transition: all 0.2s;
  z-index: 10; opacity: 0;
  &:hover { background: #f5f6f8; color: #606266; }
}

@keyframes clickEffect {
  0% { transform: scale(1); } 50% { transform: scale(0.98); } 100% { transform: scale(1); }
}

.new-folder-card {
  background: #e0e0e0 !important; border: 2px dashed #2f7bff !important;
  &:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(31,107,240,.4); background: #d0d0d0 !important; }
}

.folder-top-left { height: 25px; min-height: 25px; align-self: flex-start; font-size: 12px; color: #9aa1ad; width: 100%; margin-bottom: 4px; }
.folder-center { flex: 1; display: flex; align-items: center; justify-content: center;font-size: 60px; margin-top: -15px; margin-bottom: auto; width: 100%; }
.blue-folder-svg { width: 60px; height: 60px; flex-shrink: 0; }
.folder-bottom-left {
  align-self: flex-start; font-size: 16px; font-weight: 500; color: #2b2f36;
  width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* ================= 通用列表样式 ================= */
.content-section { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: visible; }

.meeting-list {
  flex: 1; min-height: 0; overflow-y: auto; padding-bottom: 120px;
  scrollbar-width: thin; scrollbar-color: #dcdfe6 transparent; -webkit-overflow-scrolling: touch;
}
.meeting-list::-webkit-scrollbar { width: 6px; }
.meeting-list::-webkit-scrollbar-track { background: transparent; }
.meeting-list::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 10px; }
.meeting-list::-webkit-scrollbar-thumb:hover { background: #c0c4cc; }

.meeting-card {
  display: flex; align-items: center; padding: 16px 20px; background: #ffffff;
  border: none; box-shadow: none; border-radius: 16px; transition: background 0.2s ease; cursor: pointer;
  &:hover { background: #e6f0ff; }
  &.is-clicking { background: #d6e8ff !important; transform: scale(0.97); transition: all 0.1s ease-out; }
  .el-dropdown { position: relative; z-index: 2; }
}

.card-icon {
  flex-shrink: 0; width: 64px; height: 48px; margin-right: 16px; position: relative;
  svg { width: 100%; height: 100%; }
  .favorite-badge { position: absolute; top: 2px; left: 2px; font-size: 17px; color: #e6a23c; z-index: 1; pointer-events: none; filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.15)); }
}

.card-info { flex: 1; min-width: 0; }
.card-title { margin: 0 0 6px 0; font-size: 16px; font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-meta { margin: 0; font-size: 13px; color: #909399; .meta-divider { margin: 0 6px; } }
.card-more { flex-shrink: 0; font-size: 20px; color: #909399; padding: 8px; border-radius: 8px; cursor: pointer; transition: all 0.2s; &:hover { background: #f5f6f8; color: #606266; } }

.empty-state { text-align: center; padding: 80px 0; color: #c0c4cc; font-size: 14px; }

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

::v-deep .search-highlight {
  color: #4A7DFF; text-shadow: 0 0 6px rgba(74, 125, 255, 0.45); font-weight: 600; background: transparent;
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
body .new-folder-dialog.el-message-box { border-radius: 14px !important; }

.new-folder-dialog.el-message-box {
  .el-message-box__btns .el-button {
    border-radius: 14px !important; // ← 按需调整数值
  }
  .el-message-box__btns .el-button--primary {
    background-color: #4a7dff !important;
    border-color: #4a7dff !important;
    color: #fff !important;
  }
  .el-message-box__btns .el-button:first-child {
    background-color: #f56c6c !important;
    border-color: #f56c6c !important;
    color: #fff !important;
  }
}

.new-folder-dialog.el-message-box {
  border-radius: 14px !important;
  .el-message-box__btns .el-button {
    border-radius: 14px !important;
    font-weight: 600;
    padding: 10px 28px;
  }
  .el-message-box__btns .el-button--primary {
    background-color: #4a7dff !important;
    border-color: #4a7dff !important;
    color: #fff !important;
  }
  .el-message-box__btns .el-button:first-child {
    background-color: #f56c6c !important;
    border-color: #f56c6c !important;
    color: #fff !important;
  }
  .el-message-box__btns .el-button--warning,
  .el-message-box__btns .el-button--danger {
    background-color: #f56c6c !important;
    border-color: #f56c6c !important;
    color: #fff !important;
  }
}
</style>