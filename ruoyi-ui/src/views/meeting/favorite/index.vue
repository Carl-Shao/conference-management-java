<template>
  <div class="meeting-index">
    <!-- 搜索模式 (全屏/独立视图) -->
    <div v-if="isSearchMode" class="search-mode-overlay">
      <div class="search-bar-wrapper">
        <!-- ✅ 左侧叉号按钮：返回原界面 -->
        <button class="search-close-btn" @click="exitSearch" aria-label="关闭搜索">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>

        <!-- 搜索框：进入后自动获得焦点和光标 -->
        <el-input
          ref="searchInputRef"
          v-model="searchKeyword"
          placeholder="搜索会议纪要..."
          prefix-icon="el-icon-search"
          class="search-mode-input"
          clearable
        />
      </div>

      <!-- 搜索结果列表 -->
      <div class="search-results">
        <template v-if="searchKeyword.trim()">
          <div class="results-hint">
            找到 {{ filteredMeetingList.length }} 条相关纪要
          </div>

          <div class="meeting-list search-result-list">
            <div v-for="item in filteredMeetingList" :key="item.id" class="meeting-card search-result-card">
              <!-- ⚠️ 建议抽取为 <MeetingCard :item="item" /> 子组件避免重复 -->
              <div class="card-icon">
                <i v-if="item.isFavorite" class="favorite-badge el-icon-star-on" />
                <svg v-if="item.fileType === 'record'" viewBox="0 0 48 48" fill="none">
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
                <h3 class="card-title" v-html="highlightText(item.title, searchKeyword)" />
                <p class="card-meta">
                  <span v-html="highlightText(item.duration, searchKeyword)" />
                  <span class="meta-divider">·</span>
                  <span v-html="highlightText(item.createTime, searchKeyword)" />
                </p>
              </div>
            </div>

            <div v-if="!filteredMeetingList.length" class="empty-state">
              <p>未找到与 "{{ searchKeyword }}" 相关的纪要</p>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- ======================= -->
    <!-- 普通模式 (默认视图)   -->
    <div v-else class="normal-mode-view">
      <!-- 1. 搜索栏置顶：作为页面最顶部的操作区 -->
      <div class="top-search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会议纪要..."
          prefix-icon="el-icon-search"
          class="meeting-search"
          clearable
          @click.native.prevent="enterSearchMode"
        />

        <div class="action-buttons-group">

          <!-- 1. 上传文件按钮 (蓝色渐变 + 文字) -->
          <button class="upload-btn" @click="$refs.fileInput.click()">
            <svg viewBox="0 0 24 24" fill="none">
              <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" stroke="#fff" stroke-width="2" />
              <path d="M16 6l-4-4-4 4" stroke="#fff" stroke-width="2" />
              <path d="M12 2v11" stroke="#fff" stroke-width="2" />
            </svg>
            <span>上传文件</span>
            <input ref="fileInput" type="file" accept=".mp3,.wav,.m4a,.mp4,.mov,.pdf,.docx" style="display: none;"
              @change="handleFileUpload" />
          </button>

          <!-- 2. 排序按钮 (圆形 + hover旋转) -->
          <el-dropdown trigger="click" @command="handleSortChange">
            <button class="icon-action-btn sort-button" title="排序">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 6h18v2H3V6zm0 4h12v2H3v-2zm0 4h6v2H3v-2zm0 4h18v2H3v-2z" />
              </svg>
            </button>

            <!-- 修改点：为每个 dropdown-item 添加语义化图标 -->
            <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
              <el-dropdown-item command="timeDesc">
                <i class="el-icon-time icon-clr-blue" />
                <span>最近生成</span>
              </el-dropdown-item>

              <el-dropdown-item command="timeAsc">
                <i class="el-icon-time icon-clr-green" />
                <span>最早生成</span>
              </el-dropdown-item>

              <el-dropdown-item command="titleAsc">
                <i class="el-icon-sort-up icon-clr-purple" />
                <span>按标题 A-Z</span>
              </el-dropdown-item>

              <el-dropdown-item command="titleDesc">
                <i class="el-icon-sort-down icon-clr-orange" />
                <span>按标题 Z-A</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>

          <!-- 3. 筛选按钮 (漏斗图标 + hover旋转) -->
          <el-dropdown trigger="click" @command="handleFilterChange">
            <button class="icon-action-btn filter-button" :class="{ 'is-active': currentFilter !== 'all' }" title="筛选">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16l-8 8v8H8v-8L4 4z" />
              </svg>
            </button>

            <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
              <!-- 全部纪要：使用录制音频的麦克风图标（作为默认/全部的代表） -->
              <el-dropdown-item command="all">
                <svg class="dropdown-svg-icon" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path
                    d="M14 6H30L38 14V38C38 40.2091 36.2091 42 34 42H14C11.7909 42 10 40.2091 10 38V10C10 7.79086 11.7909 6 14 6Z"
                    stroke="#909399" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M30 6V14H38" stroke="#909399" stroke-width="2.5" stroke-linecap="round"
                    stroke-linejoin="round" />
                  <line x1="16" y1="22" x2="32" y2="22" stroke="#909399" stroke-width="2.5" stroke-linecap="round" />
                  <line x1="16" y1="28" x2="28" y2="28" stroke="#909399" stroke-width="2.5" stroke-linecap="round" />
                  <line x1="16" y1="34" x2="24" y2="34" stroke="#909399" stroke-width="2.5" stroke-linecap="round" />
                </svg>
                <span>全部纪要</span>
              </el-dropdown-item>

              <!-- 录制音频文件：复用产品规划会议的麦克风图标 -->
              <el-dropdown-item command="record">
                <svg class="dropdown-svg-icon" viewBox="0 0 46 46" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect x="18" y="6" width="12" height="20" rx="6" fill="#4A7DFF" />
                  <path d="M12 22a12 12 0 0 0 24 0" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                  <line x1="24" y1="34" x2="24" y2="40" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                  <line x1="18" y1="40" x2="30" y2="40" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" />
                </svg>
                <span>录制音频文件</span>
              </el-dropdown-item>

              <!-- 上传音频文件：复用团队周例会的上传箭头图标 -->
              <el-dropdown-item command="upload">
                <svg class="dropdown-svg-icon" viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 4v12" stroke="#67C23A" stroke-width="2.2" stroke-linecap="round" />
                  <path d="M8 9l4-5 4 5" stroke="#67C23A" stroke-width="2.2" stroke-linecap="round"
                    stroke-linejoin="round" />
                  <path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" stroke="#67C23A" stroke-width="2.2"
                    stroke-linecap="round" />
                </svg>
                <span>上传音频文件</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>

        </div>
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
              <svg v-if="item.fileType === 'record'" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
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

          <div v-if="!meetingList.length" class="empty-state">
            <p>暂无会议纪要</p>
          </div>
        </div>
      </div>

      <div class="fab-btn" @click="startRecording">
        <i class="el-icon-microphone" /><span>开始听记</span>
      </div>
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
      isSearchMode: false,
      sortType: 'timeDesc',
      currentFilter: 'all',
      clickingId: null,
      meetingList: [
        { id: 1, title: '产品规划会议', fileType: 'record', duration: '45:20', createTime: '2026-07-30 14:30', isFavorite: false, downloadUrl: '/api/meeting/download/1' },
        { id: 2, title: '团队周例会', fileType: 'upload', duration: '32:15', createTime: '2026-07-29 10:00', isFavorite: true, downloadUrl: '/api/meeting/download/2' },
        { id: 3, title: 'Q3 OKR 对齐会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/3' },
        { id: 4, title: '技术部晨会', fileType: 'record', duration: '18:40', createTime: '2026-07-27 15:30', isFavorite: false, downloadUrl: '/api/meeting/download/4' },
        { id: 5, title: '07_25会议', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/5' },
        { id: 6, title: '项目会议', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/6' },
        { id: 7, title: 'Q2 OKR 对齐会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/7' },
        { id: 8, title: '需求分析会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/8' },
        { id: 9, title: '技术部晨会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/9' },
        { id: 10, title: '07_15会议', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/10' },
        { id: 11, title: '产品优化会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/11' },
        { id: 12, title: '产品风险评估', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/12' },
        { id: 13, title: '技术部晨会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/13' },
        { id: 14, title: 'Q1 OKR 对齐会', fileType: 'record', duration: '58:40', createTime: '2026-07-28 16:20', isFavorite: false, downloadUrl: '/api/meeting/download/14' }
      ]
    }
  },
  computed: {
    filteredMeetingList() {
      if (!this.searchKeyword.trim()) return [];
      const kw = this.searchKeyword.toLowerCase();
      return this.meetingList.filter(item =>
        item.title.toLowerCase().includes(kw) ||
        (item.duration && item.duration.includes(kw)) ||
        (item.createTime && item.createTime.includes(kw))
      );
    },
    currentSortLabel() {
      const map = { timeDesc: '最近生成', timeAsc: '最早生成', titleAsc: '按标题排序' }
      return map[this.sortType] || '最近生成'
    }
  },
  methods: {
    enterSearchMode() {
      this.isSearchMode = true;
      // 等待DOM渲染后自动聚焦，光标立即出现在新搜索框中
      this.$nextTick(() => {
        const input = this.$refs.searchInputRef;
        if (input) {
          input.focus();
          // 如果已有文字，全选方便替换
          if (this.searchKeyword) {
            input.select();
          }
        }
      });
    },
    exitSearch() {
      this.isSearchMode = false;
      this.searchKeyword = ''; // 清空关键词，回到原始列表
    },

    handleFileUpload(event) {
      const file = event.target.files[0];
      if (!file) return;
      const newItem = {
        id: Date.now(),
        title: file.name.replace(/\.[^/.]+$/, ""),
        fileType: 'upload',
        duration: '--:--', // 上传文件可能需要后端解析时长
        createTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
        isFavorite: false,
        downloadUrl: URL.createObjectURL(file) // 本地预览链接
      };
      this.meetingList.unshift(newItem);
      this.$message.success(`文件 "${file.name}" 上传成功`);

      // ⚠️ 重要：重置input以允许重复上传同一文件
      this.$refs.fileInput.value = '';
    },
    highlightText(text, keyword) {
      if (!keyword || !keyword.trim()) return this.escapeHtml(text);
      const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // 转义正则特殊字符
      const regex = new RegExp(`(${escaped})`, 'gi');
      // 对原始文本做 HTML 转义防止 XSS，再替换关键字
      return this.escapeHtml(text).replace(
        new RegExp(`(${this.escapeHtml(escaped)})`, 'gi'),
        '<span class="search-highlight">$1</span>'
      );
    },
    escapeHtml(str) {
      const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
      return String(str).replace(/[&<>"']/g, c => map[c]);
    },
    handleSortChange(command) { this.sortType = command },
    handleFilterChange(command) {
      this.currentFilter = command;
      // TODO: 根据 command 过滤显示列表
    },
    handleCommand(command, row) {
      if (command === 'delete') {
        this.$confirm('确定删除该会议纪要吗？删除后不可恢复。', '提示', { type: 'warning' })
          .then(() => {
            this.meetingList = this.meetingList.filter(item => item.id !== row.id);
            this.$message.success('已删除');
          }).catch(() => { });
      } else if (command === 'download') {
        const link = this.$refs.downloadLink;
        link.href = row.downloadUrl;
        link.download = `${row.title}.mp3`;
        link.click();
      } else if (command === 'addFavorite') {
        row.isFavorite = true; this.$message.success('已添加到收藏');
      } else if (command === 'removeFavorite') {
        row.isFavorite = false; this.$message.success('已从收藏列表移除');
      }
    },
    handleCardClick(event, item) {
      // 🔑 核心逻辑：检查点击目标是否在下拉菜单内部
      const target = event.target;
      const isDropdownClick = target.closest('.el-dropdown') || 
                              target.closest('.el-dropdown-menu') ||
                              target.classList.contains('card-more');
      
      if (isDropdownClick) {
        // 如果点的是下拉菜单或更多按钮，直接返回，不触发卡片特效
        return;
      }
      this.clickingId = item.id;
      
      // 300ms 后重置动画状态，允许重复点击
      setTimeout(() => {
        this.clickingId = null;
      }, 300);

      // 在这里执行你的业务逻辑（如跳转详情页）
      console.log('打开会议纪要:', item.title);
      // this.$router.push(`/meeting/detail/${item.id}`);
    },
    startRecording() {
      let backRoute = '';
      try {
        backRoute = JSON.stringify({
          name: this.$route.name,
          path: this.$route.path,
          query: this.$route.query || {}
        });
      } catch (e) {
        console.warn('backRoute 序列化失败', e);
        backRoute = this.$route.path;
      }

      this.$router.push({
        path: '/meeting/record',
        query: { backRoute }
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.meeting-index {
  padding: 24px;
  background: #ffffff;
  position: relative;
  height: 100vh; 
  display: flex; 
  flex-direction: column;
  overflow: hidden;
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

.search-bar-wrapper {
  display: relative;
  align-items: center;
  gap: 20px;
  padding: 8px 20px 8px 64px;
  flex-shrink: 0;
}

.search-close-btn {
  position: absolute;
  left: 20px;
  width: 30px; height: 30px;
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

  ::v-deep .el-input__inner {
    height: 30px;
    line-height: 30px;
    border-radius: 14px;
    border: none;
    background-color: #f5f6f8;
    font-size: 14px;
    padding-left: 40px;
    color: #303133;

    &::placeholder { color: #b0b3b8; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }

  ::v-deep .el-input__prefix {
    left: 10px;
    font-size: 14px;
    line-height: 0px;
    top: -3px; 
  }

  ::v-deep .el-input__suffix {
    line-height: 40px;
    top: -3px;
  }
}

/* ========== 搜索结果专属样式 ========== */
.search-result-list {
  gap: 0; /*  取消原有卡片间距，改用分割线分隔 */
}

.search-result-card {
  padding: 0px 20px; /*  缩小行间距（原 16px → 12px） */
  border-radius: 0;   /*  去掉圆角，配合分割线更整洁 */
  background: transparent; /*  默认底色透明 */
  transition: opacity 0.2s ease, background-color 0.2s ease;
  margin: 0 12px -15px;

  /*  每条记录中间用直线分隔，颜色与文字一致 */
  & + .search-result-card {
    border-top: 1px solid #e8e9ea;
    border-radius: 0;
  }

  /*  hover 时底色与背景一致（白色），整条记录降低透明度 */
  &:hover {
    background: #ffffff !important; /* 覆盖原有 hover 蓝色背景 */
    opacity: 0.55; /* 图案和内容整体增加透明度 */
  }

  /* 确保内部元素不单独响应 hover（由父级统一控制透明度） */
  .card-icon,
  .card-info {
    pointer-events: none;
  }
}

.search-results {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.results-hint {
  padding: 12px 20px 4px;
  font-size: 13px;
  color: #909399;
}

.normal-mode-view {
  flex: 1;
  min-height: 0vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 置顶搜索栏：页面最顶部 */
.top-search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  flex-shrink: 0;
  /* 与下方内容区拉开距离 */
}

/* 内容区标题 */
.section-title {
  margin: 0 0 16px 0;
  margin-left: 20px;
  font-size: 25px;
  font-weight: 600;
  color: #5a606b;
  line-height: 1.4;
}

/* 搜索框样式保持不变 */
.meeting-search {
  margin-left: 20px; // 
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

    &::placeholder {
      color: #b0b3b8;
    }

    &:focus {
      background-color: #edeef0;
      box-shadow: none;
    }
  }

  ::v-deep .el-input__prefix {
    left: 8px; // 调整图标距左边距离
    font-size: 14px; 
    line-height: 32px; 
    transition: none; 
  }

  ::v-deep .el-input__prefix .el-icon-search {
    font-size: 14px; // 确保图标字号同步缩小
    line-height: inherit; // 继承父级行高
  }
}

::v-deep .search-highlight {
  color: #4A7DFF;
  text-shadow: 0 0 6px rgba(74, 125, 255, 0.45);
  font-weight: 600;
  background: transparent; /* 不加背景色，仅阴影 */
}

/* 右侧按钮组布局 */
.action-buttons-group {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-left: auto;
  padding-right: 20px;
}

/* 上传按钮：蓝色渐变 + 白色图标文字 */
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
  overflow: hidden;

  svg {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 28px rgba(31, 107, 240, 0.4);
  }

  &:active {
    transform: translateY(-1px);
  }
}

/* 排序 & 筛选按钮：圆形透明底 + hover旋转 */
.icon-action-btn {
  width: 42px;
  height: 42px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #5a606b;
  transition: background 0.2s, transform 0.2s;

  svg {
    width: 22px;
    height: 22px;
  }

  &:hover {
    background: rgba(20, 24, 40, 0.06);
    transform: rotate(-15deg);
  }

  /* 筛选激活态 */
  &.is-active {
    color: #2f7bff;
    background: rgba(47, 123, 255, 0.08);
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

.meeting-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;

  padding-bottom: 120px;

  scrollbar-width: thin;
  scrollbar-color: #dcdfe6 transparent;

  -webkit-overflow-scrolling: touch;
}

.meeting-list::-webkit-scrollbar {
  width: 6px;
}

.meeting-list::-webkit-scrollbar-track {
  background: transparent;
}

.meeting-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 10px;
}

.meeting-list::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.content-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

  &:hover {
    background: #e6f0ff;
  }

  &.is-clicking {
    background: #d6e8ff !important; // 比 hover 更深的蓝色反馈
    transform: scale(0.97);        // 轻微缩放模拟按压感
    transition: all 0.1s ease-out;  // 快速响应
  }

  .el-dropdown {
    position: relative;
    z-index: 2; // 保证下拉菜单层级高于卡片点击层
  }
}

.search-result-card {
  &.is-clicking {
    opacity: 0.4 !important;
    background: #f0f7ff !important;
    transition: all 0.1s ease-out;
  }
}

.card-icon {
  flex-shrink: 0;
  width: 64px;
  height: 48px;
  margin-right: 16px;
  position: relative;

  svg {
    width: 100%;
    height: 100%;
  }

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

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  margin: 0 0 6px 0;
  font-size: 16px;
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

.fab-btn {
  position: absolute;
  flex-shrink: 0;
  left: 50%;
  bottom: 50px;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 18px 116px;
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

<!-- 全局下拉菜单样式（保持不变） -->
<style lang="scss">
.custom-action-dropdown.el-dropdown-menu {
  border-radius: 20px !important;
  padding: 6px 0 !important;
  overflow: hidden;

  .el-dropdown-menu__item {
    font-size: 15px !important;
    line-height: 22px !important;
    padding: 10px 20px !important;
    color: #303133 !important;

    i {
      font-size: 18px !important;
      margin-right: 8px;
      vertical-align: middle;
      display: inline-flex;
      align-items: center;
      height: 22px;
    }

    span {
      vertical-align: middle;
    }

    &:hover,
    &:focus {
      background-color: #f5f7fa !important;
      color: #303133 !important;
    }
  }

  .icon-clr-green {
    color: #67c23a !important;
  }

  .icon-clr-yellow {
    color: #e6a23c !important;
  }

  .icon-clr-blue {
    color: #409eff !important;
  }

  .icon-clr-purple {
    color: #9b59b6 !important;
  }

  .icon-clr-orange {
    color: #ff8c00 !important;
  }

  .icon-clr-red {
    color: #f56c6c !important;
  }

  .el-dropdown-menu__item--divided:before {
    margin: 4px 20px !important;
  }

  .dropdown-svg-icon {
    width: 26px;
    height: 26px;
    margin-right: 10px;
    vertical-align: middle;
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
  }
}
</style>