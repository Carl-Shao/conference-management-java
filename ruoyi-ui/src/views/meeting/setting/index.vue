<template>
  <div class="meeting-index">
    <!-- 🔍 搜索模式 (全屏/独立视图) - 保持不变 -->
    <div v-if="isSearchMode" class="search-mode-overlay">
      <div class="search-bar-wrapper">
        <button class="search-close-btn" @click="exitSearch" aria-label="关闭搜索">
          <svg viewBox="0 0 24 24" fill="none"  stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
        <el-input
          ref="searchInputRef"
          v-model="searchKeyword"
          placeholder="搜索会议纪要..."
          prefix-icon="el-icon-search"
          class="search-mode-input"
          clearable
        />
      </div>
      <div class="search-results">
        <template v-if="searchKeyword.trim()">
          <div class="results-hint">找到 {{ filteredMeetingList.length }} 条相关纪要</div>
          <div class="mac-group search-result-list">
            <div v-for="item in filteredMeetingList" :key="item.id" class="mac-row search-result-card">
              <div class="row-icon">
                <svg v-if="item.fileType === 'record'" viewBox="0 0 48 48" fill="none"><path d="M24 14C22.3431 14 21 15.3431 21 17V25C21 26.6569 22.3431 28 24 28C25.6569 28 27 26.6569 27 25V17C27 15.3431 25.6569 14 24 14Z" fill=none /><path d="M19 25C19 27.7614 21.2386 30 24 30C26.7614 30 29 27.7614 29 25" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" /><line x1="24" y1="30" x2="24" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" /><line x1="21" y1="34" x2="27" y2="34" stroke="#4A7DFF" stroke-width="2" stroke-linecap="round" /></svg>
                <svg v-else viewBox="0 0 48 48" fill="none"><path d="M24 16V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round" /><path d="M19 21L24 16L29 21" stroke="#67C23A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /><path d="M16 28V32C16 33.1046 16.8954 34 18 34H30C31.1046 34 32 33.1046 32 32V28" stroke="#67C23A" stroke-width="2" stroke-linecap="round" /></svg>
              </div>
              <div class="row-content">
                <span class="row-title" v-html="highlightText(item.title, searchKeyword)" />
                <span class="row-subtitle">
                  <span v-html="highlightText(item.duration, searchKeyword)" /> · <span v-html="highlightText(item.createTime, searchKeyword)" />
                </span>
              </div>
            </div>
            <div v-if="!filteredMeetingList.length" class="empty-state"><p>未找到与 "{{ searchKeyword }}" 相关的纪要</p></div>
          </div>
        </template>
      </div>
    </div>

    <!-- ======================= -->
    <!-- 📋 普通模式 (设置面板)   -->
    <div v-else class="normal-mode-view">
      <div class="top-search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会议纪要..."
          prefix-icon="el-icon-search"
          class="meeting-search"
          clearable
          @click.native.prevent="enterSearchMode"
        />
      </div>

      <div class="settings-container">
        <h2 class="section-title">设置</h2>

        <!-- ✅ 第一组：用户信息（点击头像栏直接进入用户中心） -->
        <div class="mac-group">
          <div class="mac-row user-row" @click="$router.push('/user/profile')">
            <div class="user-avatar">
              <img v-if="avatar" :src="avatar" class="user-avatar-img" />
              <svg v-else viewBox="0 0 24 24" fill="none" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div class="row-content">
              <span class="row-title">{{ nickName || '未登录' }}</span>
              <span class="row-subtitle">个人资料、账户与安全</span>
            </div>
            <div class="row-chevron"><svg viewBox="0 0 24 24" width="16" height="16" stroke-width="2.5" fill="none"><polyline points="9 18 15 12 9 6"/></svg></div>
          </div>
        </div>

        <!-- ✅ 第二组：账户设置 -->
        <div class="mac-group">
          <div class="mac-row" @click="setting && $emit('setLayout')">
            <div class="row-icon icon-layout">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8"><rect x="3" y="4" width="18" height="16" rx="4"/><path d="M3 10h18"/><path d="M9 10v10"/></svg>
            </div>
            <div class="row-content"><span class="row-title">布局设置</span></div><span class="row-subtitle">调整系统界面布局</span>
            <div class="row-chevron">›</div>
          </div>
          <div class="mac-row" @click="lockScreen">
            <div class="row-icon icon-lock">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8"><rect x="5" y="10" width="14" height="11" rx="4"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
            </div>
            <div class="row-content"><span class="row-title">锁定屏幕</span></div><span class="row-subtitle">保护当前账号安全</span>
            <div class="row-chevron">›</div>
          </div>
        </div>

        <!-- ✅ 退出登录固定放最下面 -->
        <div class="mac-group logout-group">
          <div class="mac-row" @click="logout">
            <div class="row-icon icon-exit">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8"><path d="M10 17l5-5-5-5"/><path d="M15 12H3"/><path d="M21 19V5a2 2 0 0 0-2-2h-5"/></svg>
            </div>
            <div class="row-content"><span class="row-title">退出登录</span></div><span class="row-subtitle">退出当前账号</span>
            <div class="row-chevron">›</div>
          </div>
        </div>

        <!-- ✅ 第二组：通用设置 -->
        <div class="mac-group">
          <div class="mac-row">
            <div class="row-icon icon-blue">
              <svg viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.88)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8"/><path d="M10 21h4"/></svg>
            </div>
            <div class="row-content">
              <span class="row-title">通知</span>
              <span class="row-subtitle">会前提醒、纪要生成通知</span>
            </div>
            <div class="row-chevron"><svg viewBox="0 0 24 24" width="16" height="16"  stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></div>
          </div>
        </div>

        <!-- ✅ 第四组：其他 -->
        <div class="mac-group">
          <div class="mac-row">
            <div class="row-icon icon-teal">
              <svg viewBox="0 0 24 24" fill="none"  stroke-width="1.8"><circle cx="12" cy="12" r="9.2"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            </div>
            <div class="row-content">
              <span class="row-title">关于</span>
              <span class="row-subtitle">版本 v2.4.1</span>
            </div>
            <div class="row-chevron"><svg viewBox="0 0 24 24" width="16" height="16"  stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></div>
          </div>
        </div>
      </div>
    </div>

    <a ref="downloadLink" style="display:none" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'MeetingIndex',
  data() {
    return {
      searchKeyword: '',
      isSearchMode: false,
      sortType: 'timeDesc',
      currentFilter: 'all',
      meetingList: []
    }
  },
  computed: {
    ...mapGetters(['avatar', 'nickName']),
    setting() {
      return this.$store.state.settings.showSettings
    },
    filteredMeetingList() {
      if (!this.searchKeyword.trim()) return [];
      const kw = this.searchKeyword.toLowerCase();
      return this.meetingList.filter(item =>
        item.title.toLowerCase().includes(kw) ||
        (item.duration && item.duration.includes(kw)) ||
        (item.createTime && item.createTime.includes(kw))
      );
    }
  },
  methods: {
    enterSearchMode() {
      this.isSearchMode = true;
      this.$nextTick(() => {
        const input = this.$refs.searchInputRef;
        if (input) {
          input.focus();
          if (this.searchKeyword) input.select();
        }
      });
    },
    exitSearch() {
      this.isSearchMode = false;
      this.searchKeyword = '';
    },
    highlightText(text, keyword) {
      if (!keyword || !keyword.trim()) return this.escapeHtml(text);
      const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
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
    handleFilterChange(command) { this.currentFilter = command },

    lockScreen() {
      const currentPath = this.$route.fullPath;
      this.$store.dispatch('lock/lockScreen', currentPath).then(() => {
        this.$router.push('/lock');
      });
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index';
        });
      }).catch(() => {});
    }
  }
}
</script>

<style lang="scss" scoped>
/* =============================================
   🍎 macOS System Settings — Complete Style
   ============================================= */

.meeting-index {
  padding: 24px 32px;
  background: #ffffff;
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
}

/* ---------- 搜索模式 ---------- */
.search-mode-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  animation: searchSlideIn 0.25s cubic-bezier(0.2, 0, 0.2, 1);
}
@keyframes searchSlideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.search-bar-wrapper {
  position: relative;
  padding: 16px 24px 16px 72px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.search-close-btn {
  position: absolute;
  left: 24px; top: 50%;
  transform: translateY(-50%);
  width: 32px; height: 32px;
  border: none;
  background: #f5f5f7;
  border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  color: #1d1d1f;
  transition: all 0.2s ease;
  svg { width: 16px; height: 16px; }
  &:hover { background: #e8e8ed; }
  &:active { transform: translateY(-50%) scale(0.92); }
}

.search-mode-input {
  width: 100%;
  ::v-deep .el-input__inner {
    height: 36px; line-height: 36px;
    border-radius: 20px; border: none;
    background-color: #f5f5f7;
    font-size: 15px; padding-left: 40px; color: #1d1d1f;
    &::placeholder { color: #86868b; }
    &:focus { background-color: #edeef0; box-shadow: none; }
  }
  ::v-deep .el-input__prefix { left: 12px; font-size: 15px; line-height: 36px; color: #86868b; }
  ::v-deep .el-input__suffix { line-height: 36px; }
}

.search-results {
  flex: 1; overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 16px 24px;
}

.results-hint {
  padding: 4px 4px 12px;
  font-size: 12px; color: #86868b;
  font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;
}

.search-result-list.mac-group {
  margin: 0;
  .search-result-card {
    &:hover { background: rgba(0, 0, 0, 0.03) !important; }
  }
}

/* ---------- 普通模式 ---------- */
.normal-mode-view {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column;
  overflow: hidden;
}

.top-search-bar {
  display: flex; align-items: center;
  margin-bottom: 24px; flex-shrink: 0;
}

.meeting-search {
  max-width: 320px; width: 100%;
  ::v-deep .el-input__inner {
    height: 36px; line-height: 36px;
    border-radius: 20px; border: 1px solid transparent;
    background-color: #f5f5f7;
    color: #1d1d1f; font-size: 14px; padding-left: 38px;
    transition: all 0.2s ease;
    &::placeholder { color: #86868b; }
    &:focus { background-color: #fff; border-color: #d1d1d6; box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.04); }
  }
  ::v-deep .el-input__prefix { left: 12px; font-size: 14px; line-height: 36px; color: #86868b; }
}

.settings-container {
  flex: 1; min-height: 0;
  overflow-y: auto;
  padding-bottom: 40px;
  -webkit-overflow-scrolling: touch;
}

.section-title {
  margin: 0 0 20px 4px;
  font-size: 28px; font-weight: 700;
  color: #1d1d1f; line-height: 1.2;
  letter-spacing: -0.5px;
}

/* =============================================
   🎯 macOS 分组核心样式
   ============================================= */

.mac-group {
  background-color: #f5f5f7;
  border-radius: 20px;
  margin-bottom: 24px;
  overflow: hidden;
  display: flex; flex-direction: column;
}

.mac-row {
  display: flex; align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);

  &:last-child { border-bottom: none; }
  &:hover { background-color: rgba(0, 0, 0, 0.03); }
  &:active { background-color: rgba(0, 0, 0, 0.06); }
}

/* --- 用户头像行 --- */
.user-row {
  padding: 16px;
  // el-dropdown 包裹后需要撑满宽度
  width: 100%;
  box-sizing: border-box;

  .row-title {
    font-size: 19px;      // 从默认的 14px 增大到 17px (macOS 标题标准尺寸)
    font-weight: 600;     // 加粗以增强层级感
    line-height: 1.2;     // 配合大字号调整行高
  }
}

.user-avatar {
  width: 48px; height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #5268d8 100%);
  display: flex; align-items: center; justify-content: center;
  margin-right: 10px; flex-shrink: 0;
  overflow: hidden;
  svg { width: 26px; height: 26px; stroke: #fff; }
}

.user-avatar-img {
  width: 100%; height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.logout-item {

  color:#1d1d1f !important;

  &:hover,
  &:focus {
    background-color:#1d1d1f !important;
    color:white !important;
  }

}

/* --- 图标样式 --- */
.row-icon {
  width: 25px;
  height: 25px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 10px; flex-shrink: 0;
  svg { width: 14px; height: 14px; stroke: #fff; stroke-linecap: round; stroke-linejoin: round; }

  background: linear-gradient(145deg, #eef3f8, #dfe7ef);
  box-shadow: inset 0 1px 2px rgba(255,255,255,.8), 0 5px 12px rgba(120,130,150,.12);
  color:#6f8499;
  &.icon-layout { background: linear-gradient(145deg,#6fa8ff,#3f78e8); color:#fff; box-shadow: 0 5px 12px rgba(63,120,232,.28); }
  &.icon-lock { background: linear-gradient(145deg,#fcc14b,#e99a12); color:#fff; box-shadow: 0 5px 12px rgba(233,154,18,.26); }
  &.icon-exit { background: linear-gradient(145deg,#fa513b,#e95252); color:#fff; box-shadow: 0 5px 12px rgba(233,82,82,.24); }
  &.icon-blue { background: linear-gradient(145deg,#62a8ff,#2877e8); color:#fff; box-shadow: 0 5px 12px rgba(40,119,232,.25); }
  &.icon-gray { background: linear-gradient(145deg,#eeeeef,#dedee3); color:#777983; }
  &.icon-green { background: linear-gradient(145deg,#e2efe7,#d1e3d7); color:#668673; }
  &.icon-purple { background: linear-gradient(145deg,#eee7f2,#ded1e8); color:#806b91; }
  &.icon-orange { background: linear-gradient(145deg,#f1eadf,#e4d7c4); color:#8c775b; }
  &.icon-red { background: linear-gradient(145deg,#efe5e3,#dfd1ce); color:#8b6f69; }
  &.icon-teal { background: linear-gradient(145deg,#2be95a,#29cc4c); color:#fff; box-shadow: 0 5px 12px rgba(50,174,188,.25); }
}

/* --- 文字内容 --- */
.row-content {
  flex: 1; 
  min-width: 0;
  display: flex; 
  flex-direction: row; 
  justify-content: space-between;
}

.row-title {
  font-size: 14px; font-weight: 500;
  color: #1d1d1f; line-height: 1.3;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.row-subtitle {
  font-size: 12px; color: #86868b;
  line-height: 1; margin-left: auto; margin-top: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* --- 右侧箭头 --- */
.row-chevron {
  margin-left: auto; flex-shrink: 0;
  color: #c7c7cc;
  display: flex; align-items: center;
}

/* --- 搜索高亮 --- */
::v-deep .search-highlight {
  color: #007AFF; font-weight: 600;
  background: transparent; text-shadow: none;
}

.empty-state {
  text-align: center; padding: 60px 0;
  color: #86868b; font-size: 14px;
}

/* iOS SF Symbols style icons */
.row-icon svg {
  width: 18px;
  height: 18px;
  fill: none !important;
  opacity: 1;
}

</style>

<!-- 全局下拉菜单样式 -->
<style lang="scss">
/* 设置页用户下拉菜单 */
.custom-user-dropdown.el-dropdown-menu {
  border-radius: 12px !important;
  padding: 4px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;
  min-width: 180px;

  .el-dropdown-menu__item {
    font-size: 14px !important;
    line-height: 20px !important;
    padding: 8px 12px !important;
    border-radius: 6px !important;
    color: #1d1d1f !important;
    margin: 2px 0;

    i {
      font-size: 16px !important;
      margin-right: 8px;
      vertical-align: middle;
      display: inline-flex;
      align-items: center;
      height: 20px;
    }

    span { vertical-align: middle; }

    &:hover, &:focus {
      background-color: #007AFF !important;
      color: #fff !important;
    }
  }

  .el-dropdown-menu__item--divided {

      margin-top: 10px !important;
      padding-top: 10px !important;

      &:before {
          top: 0 !important;
          left: 12px !important;
          right: 12px !important;
          height: 0px !important;
          background-color: #e5e5ea !important;
      }
  }
}

/* 原有全局下拉菜单样式保持不变 */
.custom-action-dropdown.el-dropdown-menu {
  border-radius: 12px !important;
  padding: 4px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;

  .el-dropdown-menu__item {
    font-size: 14px !important; line-height: 20px !important;
    padding: 8px 12px !important; border-radius: 6px !important;
    color: #1d1d1f !important; margin: 2px 0;
    i { font-size: 16px !important; margin-right: 8px; vertical-align: middle; display: inline-flex; align-items: center; height: 20px; }
    span { vertical-align: middle; }
    &:hover, &:focus { background-color: #007AFF !important; color: #fff !important; }
  }
  .el-dropdown-menu__item--divided:before { margin: 4px 12px !important; }
  .dropdown-svg-icon { width: 20px; height: 20px; margin-right: 10px; vertical-align: middle; display: inline-flex; align-items: center; flex-shrink: 0; }
}

.row-icon {
  width: 15px;
  height: 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-blue svg,
.icon-gray svg,
.icon-green svg,
.icon-purple svg,
.icon-orange svg,
.icon-red svg,
.icon-teal svg {
  color: #ffffff;
}

</style>