<template>
  <div class="profile-container">
    <!-- 🔍 顶部标题 -->
    <h2 class="section-title">个人资料</h2>

    <!-- 👤 第一组：用户身份卡片 (对应原左侧个人信息) -->
    <div class="mac-group user-identity-group">
      <div class="user-header-row">
        <div class="user-avatar-wrapper">
          <userAvatar />
        </div>
        <div class="user-header-info">
          <div class="user-name">{{ user.userName || '未设置用户名' }}</div>
          <div class="user-dept-post">
            {{ user.dept ? user.dept.deptName : '' }}
            <span v-if="postGroup"> · {{ postGroup }}</span>
          </div>
        </div>
      </div>
      
      <!-- 详细信息列表化展示 -->
      <div class="mac-row" v-if="user.phonenumber">
        <div class="row-icon icon-gray">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
        </div>
        <div class="row-content">
          <span class="row-title">手机号码</span>
          <span class="row-value">{{ user.phonenumber }}</span>
        </div>
      </div>

      <div class="mac-row" v-if="user.email">
        <div class="row-icon icon-blue">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
        </div>
        <div class="row-content">
          <span class="row-title">用户邮箱</span>
          <span class="row-value">{{ user.email }}</span>
        </div>
      </div>

      <div class="mac-row" v-if="roleGroup">
        <div class="row-icon icon-purple">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        </div>
        <div class="row-content">
          <span class="row-title">所属角色</span>
          <span class="row-value">{{ roleGroup }}</span>
        </div>
      </div>

      <div class="mac-row">
        <div class="row-icon icon-teal">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </div>
        <div class="row-content">
          <span class="row-title">创建日期</span>
          <span class="row-value">{{ user.createTime }}</span>
        </div>
      </div>
    </div>

    <!-- ⚙️ 第二组：操作面板 (对应原右侧 Tabs) -->
    <div class="mac-group action-group">
      <!-- 模拟 Tab 切换，但用列表项触发或保持内部 Tab 样式 -->
      <!-- 这里为了保留原有功能，我们使用一个容器包裹原有的子组件，但去掉原生 Tab 样式 -->
      <div class="settings-panel">
        <div class="panel-tabs">
          <button 
            :class="['tab-btn', { active: selectedTab === 'userinfo' }]" 
            @click="selectedTab = 'userinfo'"
          >基本资料</button>
          <button 
            :class="['tab-btn', { active: selectedTab === 'resetPwd' }]" 
            @click="selectedTab = 'resetPwd'"
          >修改密码</button>
        </div>

        <div class="panel-content">
          <transition name="fade" mode="out-in">
            <userInfo v-if="selectedTab === 'userinfo'" key="info" :user="user" />
            <resetPwd v-else key="pwd" />
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import userAvatar from "./userAvatar"
import userInfo from "./userInfo"
import resetPwd from "./resetPwd"
import { getUserProfile } from "@/api/system/user"

export default {
  name: "Profile",
  components: { userAvatar, userInfo, resetPwd },
  data() {
    return {
      user: {},
      roleGroup: "",
      postGroup: "",
      selectedTab: "userinfo"
    }
  },
  created() {
    const activeTab = this.$route.params && this.$route.params.activeTab
    if (activeTab) {
      this.selectedTab = activeTab
    }
    this.getUser()
  },
  methods: {
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data
        this.roleGroup = response.roleGroup
        this.postGroup = response.postGroup
      })
    }
  }
}
</script>

<style lang="scss" scoped>

.profile-container {
  width: 100%;
  height: 100%;
  padding: 40px 6%;
  margin: 0 auto;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
  color: #1d1d1f;
  box-sizing: border-box;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 24px 4px;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

/* ---------- 通用分组卡片 ---------- */
.mac-group {
  background-color: #f5f5f7;
  border-radius: 20px;
  margin-bottom: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.mac-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  min-height: 44px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: background-color 0.2s ease;

  &:last-child { border-bottom: none; }
  
  // 如果是可点击的行，添加 hover 效果
  &.clickable {
    cursor: pointer;
    &:hover { background-color: rgba(0, 0, 0, 0.03); }
    &:active { background-color: rgba(0, 0, 0, 0.06); }
  }
}

/* ---------- 用户头部区域 ---------- */
.user-header-row {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.user-avatar-wrapper {
  margin-right: 16px;
  ::v-deep .user-avatar {
    width: 64px !important;
    height: 64px !important;
    border-radius: 50%;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  }
}

.user-header-info {
  flex: 1;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.user-dept-post {
  font-size: 14px;
  color: #86868b;
  font-weight: 400;
}

/* ---------- 行内容样式 ---------- */
.row-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
  
  svg {
    width: 16px;
    height: 16px;
    stroke: #fff;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  // 图标渐变色定义
  &.icon-gray { background: linear-gradient(145deg, #2be95a,#29cc4c); }
  &.icon-blue { background: linear-gradient(145deg, #4a90ff, #007aff); }
  &.icon-purple { background: linear-gradient(145deg, #af52de, #8944ab); }
  &.icon-teal { background: linear-gradient(145deg, #fcc14b,#e99a12); }
}

.row-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.row-title {
  font-size: 15px;
  font-weight: 500;
  color: #1d1d1f;
}

.row-value {
  font-size: 15px;
  color: #86868b;
  text-align: right;
  word-break: break-all;
  margin-left: 12px;
}

/* ---------- 自定义 Tab 面板 (替代 el-tabs) ---------- */
.settings-panel {
  padding: 4px;
}

.panel-tabs {
  display: flex;
  background: rgba(0,0,0,0.04);
  border-radius: 10px;
  padding: 3px;
  margin: 12px 12px 16px;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #86868b;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.2, 0, 0.2, 1);
  outline: none;

  &.active {
    background: #ffffff;
    color: #1d1d1f;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08), 0 0 1px rgba(0,0,0,0.1);
  }

  &:hover:not(.active) {
    color: #1d1d1f;
  }
}

.panel-content {
  padding: 0 16px 16px;
  min-height: 300px; // 防止切换时高度跳动
  
  /* 针对子组件的样式覆盖，使其融入新风格 */
  ::v-deep {
    .el-form-item__label {
      color: #86868b;
      font-weight: 500;
    }
    .el-input__inner, .el-textarea__inner {
      border-radius: 10px;
      border-color: #e5e5ea;
      background: #fff;
      &:focus {
        border-color: #007aff;
      }
    }
    .el-button--primary {
      background: #007aff;
      border-color: #007aff;
      border-radius: 8px;
      font-weight: 500;
      padding: 10px 24px;
    }
    .mt-4.text-center,
    .el-form-item:last-child {
      margin-top: 32px !important;
      display: flex;
      justify-content: center;
      gap: 16px;
    }
    .el-button {
      border-radius: 10px !important;
      font-weight: 500 !important;
      padding: 10px 28px !important;
      font-size: 14px !important;
      transition: all 0.2s cubic-bezier(0.2, 0, 0.2, 1) !important;
      border: none !important;
      
      &:focus {
        box-shadow: none !important;
      }
    }
    .el-button--default,
    .el-button--info {
      background-color: #f5f5f7 !important;
      color: #1d1d1f !important;
      border: 1px solid rgba(0, 0, 0, 0.08) !important;

      &:hover, &:focus {
        background-color: #e8e8ed !important;
        color: #1d1d1f !important;
        border-color: rgba(0, 0, 0, 0.12) !important;
      }

      &:active {
        background-color: #d1d1d6 !important;
        transform: scale(0.97);
      }
    }
    .el-button--primary {
      background: #007aff !important;
      border-color: #007aff !important;

      &:hover {
        background: #0066d6 !important;
        border-color: #0066d6 !important;
      }

      &:active {
        background: #0055b3 !important;
        transform: scale(0.97);
      }
    }
  }
}

/* Transition */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter, .fade-leave-to {
  opacity: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .profile-container { padding: 16px; }
  .section-title { font-size: 24px; }
  .user-avatar-wrapper ::v-deep .user-avatar { width: 56px !important; height: 56px !important; }
  .user-name { font-size: 18px; }
}
</style>