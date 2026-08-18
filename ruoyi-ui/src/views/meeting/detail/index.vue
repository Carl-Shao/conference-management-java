<template>
  <div class="meeting-detail-page">
    <!-- ==================== 顶部导航栏 (保持原样) ==================== -->
    <header class="topbar">
      <button class="back-btn" @click="$router.back()" aria-label="返回">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>

      <div class="record-info">
        <div class="record-icon-svg">
          <span v-if="detail.sourceType === '2'" class="emoji-icon">📋</span>
          <svg v-else-if="detail.sourceType === '0'" viewBox="0 0 48 48" fill="none">
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
        <input v-if="isEditingTitle" ref="titleInputRef" v-model="editingTitleValue" class="inline-title-input"
          @blur="finishEditTitle" @keydown.enter="finishEditTitle" @keydown.escape="cancelEditTitle" />
        <!-- 原标题：点击触发编辑 -->
        <div v-else class="record-title" @click="startEditTitle" :title="'点击编辑: ' + (detail.title || '')">
          {{ detail.title || '加载中...' }}
        </div>

      </div>

      <div class="top-actions">
        <button 
          class="icon-btn favorite-btn" 
          :class="{ 'is-favorite': isFavorite }"
          @click="toggleFavorite" 
          :title="isFavorite ? '取消收藏' : '添加到收藏'"
        >
          <i :class="isFavorite ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
        </button>

        <button class="icon-btn download-btn" title="下载音频/纪要" @click="handleDownload" :disabled="loading">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
        </button>
        
        <el-dropdown trigger="click" @command="handleCommand" placement="bottom-end">
          <button class="icon-btn" title="更多选项">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/>
            </svg>
          </button>
          <el-dropdown-menu slot="dropdown" class="custom-action-dropdown">
            <el-dropdown-item command="copy">
              <i class="el-icon-document-copy icon-clr-blue"></i>
              <span>复制全文</span>
            </el-dropdown-item>
            <el-dropdown-item command="rename">
              <i class="el-icon-edit icon-clr-purple"></i>
              <span>重命名</span>
            </el-dropdown-item>
            <el-dropdown-item command="delete">
              <i class="el-icon-delete icon-clr-red"></i>
              <span>删除纪要</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </header>

    <!-- ==================== 主内容区 ==================== -->
    <div class="content-area">
      <div class="tab-group">
        <div class="tab-buttons" ref="tabContainer">
          <div class="tab-indicator" :style="indicatorStyle"></div>
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            class="tab-btn" 
            :class="{ active: currentTab === tab.key }"
            @click="switchTab(tab.key, $event)"
          >
            <!-- 新增：每个 Tab 对应的 SVG 图标 -->
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path :d="tab.icon" />
              <template v-if="tab.extraPaths">
                <path v-for="(p, idx) in tab.extraPaths" :key="idx" :d="p" />
              </template>
            </svg>
            {{ tab.label }}
          </button>
        </div>
      </div>

      <!-- 内容卡片 (保持原有逻辑不变) -->
      <div class="tab-content">
        <!-- ==================== 纪要 Tab ==================== -->
        <div v-show="currentTab === 'summary'" class="content-card fade-in"
          @click="!summaryEditing && startEdit('summary')">
          <!-- <textarea v-if="summaryEditing" ref="summaryEditor" v-model="editContent" class="in-card-editor"
            placeholder="点击即可编辑纪要..." @blur="handleBlur('summary')"
            @keydown="handleKeydown($event, 'summary')"></textarea> -->
          <div v-if="detail.minutesContent" class="markdown-body" v-html="renderMarkdown(detail.minutesContent)"></div>
          <div v-else class="empty-placeholder">
            <i class="el-icon-document"></i>
            <p>{{ loading ? 'AI 正在生成纪要...' : '点击此处编写纪要' }}</p>
          </div>
        </div>

        <div v-show="currentTab === 'transcript'" class="content-card fade-in transcript-card">
          <div class="transcript-list">
            <div v-for="seg in segments" :key="seg.seqNo" class="transcript-item">
              <span class="time-badge">{{ formatTime(seg.startOffsetMs) }}</span>
              <p class="transcript-text">{{ seg.text }}</p>
            </div>
            <div v-if="!segments.length && !loading" class="empty-placeholder small">
              <p>暂无转写内容</p>
            </div>
          </div>
        </div>

        <div v-show="currentTab === 'notes'" class="content-card fade-in" @click="!notesEditing && startEdit('notes')">
          <textarea v-if="notesEditing" ref="notesEditor" v-model="editContent" class="in-card-editor"
            placeholder="点击即可编辑笔记..." @blur="handleBlur('notes')" @keydown="handleKeydown($event, 'notes')"></textarea>
          <div v-else-if="detail.noteContent" class="notes-display"
            style="white-space: pre-wrap; line-height: 1.8; font-size: 15px; color: var(--ink);">
            {{ detail.noteContent }}
          </div>
          <div v-else class="empty-placeholder">
            <i class="el-icon-edit-outline"></i>
            <p>{{ loading ? '加载中...' : '点击此处写笔记' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 底部播放器 (保持原样) ==================== -->
    <div class="audio-player">
      <audio ref="audioRef" :src="audioSrc" preload="auto" @timeupdate="onTimeUpdate" @loadedmetadata="onAudioLoaded"
        @ended="onAudioEnded" @error="onAudioError"></audio>
      <div class="progress-container">
        <span class="time">{{ formatTime(currentTime * 1000) }}</span>
        <div class="progress-bar" @click="seekAudio">
          <div class="progress" :style="{ width: progressPercent + '%' }">
            <div class="progress-handle"></div>
          </div>
        </div>
        <span class="time">{{ formatDuration(detail.duration) }}</span>
      </div>
      <div class="controls">
        <!-- 左侧：倒退 15 秒 -->
        <button class="control-btn skip-btn" title="倒退15秒" @click="skipTime(-15)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
            stroke-linejoin="round">
            <path d="M7 8.5A8 8 0 1 1 5.2 14" />
            <polyline points="7 5 5 9 9 10.5" />
            <text x="13" y="16" text-anchor="middle" font-size="7" font-weight="600" fill="currentColor" stroke="none"
              font-family="Arial, sans-serif">
              15
            </text>
          </svg>
        </button>
        <button class="control-btn play-pause" @click="togglePlay">
          <svg v-if="!isPlaying" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="3"
            stroke-linecap="round" stroke-linejoin="round">
            <polygon points="5 3 19 12 5 21 5 3" />
          </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"
            stroke-linejoin="round">
            <line x1="8" y1="5" x2="8" y2="19" />
            <line x1="16" y1="5" x2="16" y2="19" />
          </svg>
        </button>
        <!-- 右侧 1：快进 15 秒 -->
        <button class="control-btn skip-btn" title="快进15秒" @click="skipTime(15)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
            stroke-linejoin="round">
            <g transform="translate(24,0) scale(-1,1)">
              <path d="M7 8.5A8 8 0 1 1 5.2 14" />
              <polyline points="7 5 5 9 9 10.5" />
            </g>
            <text x="11" y="16" text-anchor="middle" font-size="7" font-weight="700" fill="currentColor" stroke="none"
              font-family="Arial, sans-serif">
              15
            </text>
          </svg>
        </button>
        <!-- 右侧 2：倍速播放 (点击循环切换 1x -> 1.25x -> 1.5x -> 2x) -->
        <button class="control-btn rate-btn" :title="`当前 ${playbackRate}x`" @click="changePlaybackRate">
          {{ playbackRate }}x
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { getMeeting, delMeeting, favoriteMeeting, renameMeeting, saveMeetingNote, saveMeetingMinutes, getMeetingAudioBlob } from '@/api/huiyi/minutes'

const marked = require('marked')

export default {
  name: 'MeetingDetail',
  data() {
    return {
      detail: {},
      segments: [],
      loading: false,
      isFavorite: false,
      currentTab: 'summary',
      isEditingTitle: false,      // 是否处于标题编辑模式
      editingTitleValue: '',

      summaryEditing: false,   // 纪要是否处于编辑模式
      notesEditing: false,     // 笔记是否处于编辑模式
      editContent: '',         // 编辑器临时内容
      saveTimer: false,           // 保存中状态

      // 更新：为每个 Tab 增加图标路径配置，其余保持不变
      tabs: [
        { 
          key: 'summary', 
          label: '纪要',
          icon: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z',
          extraPaths: ['M14 2v6h6', 'M16 13H8', 'M16 17H8', 'M10 9H8']
        },
        { 
          key: 'transcript', 
          label: '转写',
          icon: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z',
          extraPaths: ['M14 2v6h6', 'M16 13H8', 'M16 17H8', 'M10 9H8']
        },
        { 
          key: 'notes', 
          label: '笔记',
          icon: 'M12 20h9',
          extraPaths: ['M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z']
        }
      ],
      indicatorStyle: {},
      isPlaying: false,
      currentTime: 0,
      audioSrc: '',
      audioDuration: 0,
      playbackRate: 1, // 当前倍速，默认 1x
      playbackRates: [1, 1.25, 1.5, 2], // 可选的倍速数组
    }
  },
  computed: {
    progressPercent() {
      if (!this.audioDuration) return 0
      return (this.currentTime / this.audioDuration) * 100
    }
  },
  created() {
    const meetingId = Number(this.$route.params.meetingId)
    if (!meetingId || isNaN(meetingId)) {
      this.$message.error('无效的会议ID')
      this.$router.back()
      return
    }
    this.fetchDetail(meetingId)
  },
  mounted() {
    this.$nextTick(() => this.updateIndicator())
    window.addEventListener('resize', this.updateIndicator)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.updateIndicator)
    if(this.audioSrc) {
      URL.revokeObjectURL(this.audioSrc)
    }
  },
  methods: {
    fetchDetail(meetingId) {
      this.loading = true
      getMeeting(meetingId).then(res => {
        const data = res.data || res
        // 1. 会议基本信息
        this.detail = data.meeting || {}
        // 2. 纪要内容
        this.$set(this.detail, 'minutesContent', data.minutes?.content || '')
        // 3. 笔记内容
        this.$set(this.detail, 'noteContent', data.note?.content || '')
        // 4. 转写段落：content 是 JSON 字符串，需解析
        let segments = []
        try {
          const raw = data.transcript?.content
          if (raw) {
            const trimmed = typeof raw === 'string' ? raw.trim() : ''
            if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
              segments = JSON.parse(trimmed)
            } else if (typeof raw === 'string') {
              // 如果是纯文本，将其包装为单条转写记录，防止页面空白
              segments = [{
                seqNo: 0,
                startOffsetMs: 0,
                text: raw
              }]
            } else if (Array.isArray(raw)) {
              // 如果后端已经返回了数组对象（非字符串）
              segments = raw
            }
          }
        } catch (e) {
          console.error('转写内容 JSON 解析失败:', e)
          const fallbackText = data.transcript?.content || ''
          if (fallbackText) {
            segments = [{ seqNo: 0, startOffsetMs: 0, text: String(fallbackText) }]
          }
        }
        this.segments = segments.sort((a, b) => (a.startTime || 0) - (b.startTime || 0))
        // 5. 收藏状态
        this.isFavorite = String(this.detail.isFavorite) === '1'
        // 6. 请求音频流
        this.loadAudio(meetingId)
      }).catch(err => {
        console.error('详情接口报错:', err)
        this.$message.error(err.msg || '加载会议详情失败')
      }).finally(() => {
        this.loading = false
      })
    },
    toggleFavorite() {
      const newStatus = !this.isFavorite
      favoriteMeeting(this.detail.meetingId, newStatus).then(() => {
        this.isFavorite = newStatus
        this.$message.success(newStatus ? '已添加到收藏' : '已取消收藏')
      }).catch(() => this.$message.error('操作失败'))
    },
    async handleDownload() {
      if (!this.detail || this.loading) return;
      try {
        const fileUrl = this.detail.fileUrl || this.detail.audioUrl || this.detail.videoUrl;
        if (fileUrl) {
          const link = document.createElement('a');
          link.href = fileUrl;
          const ext = fileUrl.split('.').pop()?.split('?')[0] || 'mp3';
          link.download = `${this.detail.title || '会议记录'}.${ext}`;
          link.target = '_blank';
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          this.$message.success('开始下载');
          return;
        }
        if (this.detail.minutesContent) {
          const blob = new Blob([this.detail.minutesContent], { type: 'text/markdown;charset=utf-8' });
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `${this.detail.title || '会议纪要'}.md`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(url);
          this.$message.success('纪要已导出');
          return;
        }
        this.$message.warning('暂无可下载的内容');
      } catch (err) {
        console.error('下载失败:', err);
        this.$message.error('下载失败，请稍后重试');
      }
    },
    renderMarkdown(content) {
      if (!content) return ''

      try {
        const html = marked.parse(content)

        return html
      } catch (e) {
        console.error('Markdown 解析失败:', e)
        return content
      }
    },
    startEditTitle() {
      if (this.loading) return
      this.editingTitleValue = this.detail.title || ''
      this.isEditingTitle = true
      this.$nextTick(() => {
        const input = this.$refs.titleInputRef
        if (input) {
          input.focus()
          input.select() // 自动全选方便修改
        }
      })
    },
    async finishEditTitle() {
      const newTitle = (this.editingTitleValue || '').trim()
      const oldTitle = (this.detail.title || '').trim()
      // 内容没变或为空，直接取消编辑
      if (!newTitle || newTitle === oldTitle) {
        this.isEditingTitle = false
        return
      }
      try {
        await renameMeeting(this.detail.meetingId, newTitle)
        this.detail.title = newTitle
        this.$message.success('重命名成功')
      } catch (err) {
        this.$message.error(err?.msg || '重命名失败')
        // 失败时保持编辑状态，让用户可以重试或按 Esc 取消
        return
      } finally {
        this.isEditingTitle = false
      }
    },
    cancelEditTitle() {
      this.isEditingTitle = false
    },
    async loadAudio(meetingId) {
      try {
        const res = await getMeetingAudioBlob(meetingId)
        const blob = res instanceof Blob ? res : new Blob([res])
        console.log('Blob type:', blob.type, 'size:', blob.size)
        if (blob.size === 0 || blob.type.includes('json')) {
          const text = await blob.text()
          console.error('后端返回非音频数据:', text)
          this.$message.error('音频加载失败：服务端返回异常')
          return
        }

        if (this.audioSrc) URL.revokeObjectURL(this.audioSrc)
        this.audioSrc = URL.createObjectURL(blob)

        await this.$nextTick()
        const audioEl = this.$refs.audioRef
        if (audioEl && audioEl instanceof HTMLAudioElement) {
          audioEl.load()
        }
      } catch (err) {
        console.error('音频加载失败:', err)
        this.$message.error('音频加载失败')
      }
    },
    switchTab(key, event) {
      this.currentTab = key
      this.$nextTick(() => {
        // 直接从 DOM 中查找，不依赖 event.currentTarget
        this.updateIndicator()
      })
    },
    updateIndicator(el) {
      let target = el
      if (!target || !(target instanceof HTMLElement)) {
        const container = this.$refs.tabContainer
        target = container?.querySelector('.tab-btn.active')
      }

      const container = this.$refs.tabContainer
      // 双重安全检查：target 和 container 都必须是有效 DOM 元素
      if (!target || !container ||
        typeof target.getBoundingClientRect !== 'function' ||
        typeof container.getBoundingClientRect !== 'function') {
        return
      }

      const rect = target.getBoundingClientRect()
      const containerRect = container.getBoundingClientRect()
      const offsetLeft = rect.left - containerRect.left - 4

      this.indicatorStyle = {
        width: `${rect.width}px`,
        transform: `translateX(${offsetLeft}px)`,
      }
    },
    startEdit(type) {
      if (this.loading) return
      this.editContent = type === 'summary' 
        ? (this.detail.minutesContent || '') 
        : (this.detail.noteContent || '')
      type === 'summary' ? this.summaryEditing = true : this.notesEditing = true
      // 等待DOM渲染后自动聚焦
      this.$nextTick(() => {
        const editor = this.$refs[type + 'Editor']
        if (editor) editor.focus()
      })
    },
    handleBlur(type) {
      clearTimeout(this.saveTimer)
      this.saveTimer = setTimeout(() => this.autoSave(type), 300)
    },
    async autoSave(type) {
      const original = type === 'summary' 
        ? (this.detail.minutesContent || '') 
        : (this.detail.noteContent || '')
      // 内容没变就不请求
      if (this.editContent === original) {
        type === 'summary' ? this.summaryEditing = false : this.notesEditing = false
        return
      }
      try {
        if (type === 'summary') {
          await saveMeetingMinutes(this.detail.meetingId, this.editContent)
          this.$set(this.detail, 'minutesContent', this.editContent)
        } else {
          await saveMeetingNote(this.detail.meetingId, this.editContent)
          this.$set(this.detail, 'noteContent', this.editContent)
        }
        this.$message.success('已自动保存')
      } catch (err) {
        this.$message.error('保存失败，内容未丢失')
      } finally {
        type === 'summary' ? this.summaryEditing = false : this.notesEditing = false
      }
    },
    handleKeydown(e, type) {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault()
        clearTimeout(this.saveTimer)
        this.autoSave(type)
      }
      if (e.key === 'Escape') {
        clearTimeout(this.saveTimer)
        type === 'summary' ? this.summaryEditing = false : this.notesEditing = false
      }
    },
    handleCommand(cmd) {
      if (cmd === 'rename') {
        this.$prompt('请输入新名称', '重命名', { confirmButtonText: '确定', cancelButtonText: '取消', inputValue: this.detail.title,
        inputPattern: /\S+/, inputErrorMessage: '名称不能为空', customClass: 'detail-rename-dialog', distinguishCancelAndClose: true
        }).then(({ value }) => {
          if (!value || !value.trim()) return
          renameMeeting(this.detail.meetingId, value.trim()).then(() => {
            this.$message.success('重命名成功')
            // 详情页无需刷新列表，直接更新本地标题即可
            this.detail.title = value.trim()
          })
        }).catch(() => { })
        return
      }
      if (cmd === 'delete') {
        this.$confirm('确定删除该会议纪要吗？', '提示', { type: 'warning',confirmButtonText: '删除', cancelButtonText: '取消',
        customClass: 'detail-delete-dialog', distinguishCancelAndClose: true, showClose: false }).then(() => {
          const id = Number(this.detail.meetingId)
          if (isNaN(id)) return this.$message.error('ID无效')
          delMeeting([id]).then(() => {
            this.$message.success('已删除')
            this.$router.push('/huiyi/meeting/index')
          })
        }).catch(() => {})
      } else if (cmd === 'copy') {
        let text = ''
        if (this.currentTab === 'transcript') {
           text = this.segments.map(s => `[${this.formatTime(s.startOffsetMs)}] ${s.text}`).join('\n')
        } else {
           const div = document.createElement('div')
           div.innerHTML = this.detail.minutesContent || ''
           text = div.innerText || div.textContent || ''
        }
        navigator.clipboard.writeText(text).then(() => this.$message.success('内容已复制'))
      }
    },
    togglePlay() {
      const audio = this.$refs.audioRef
      if (!audio || !this.audioSrc) {
        this.$message.warning('音频尚未加载完成')
        return
      }
      if (this.isPlaying) {
        audio.pause()
      } else {
        audio.play().catch(err => {
          console.error('播放失败:', err)
          this.$message.error('音频播放失败')
        })
      }
      this.isPlaying = !this.isPlaying
    },
    seekAudio(e) {
      const bar = e.currentTarget
      const percent = e.offsetX / bar.offsetWidth
      const audio = this.$refs.audioRef
      if (audio && this.audioDuration) {
        audio.currentTime = percent * this.audioDuration
      }
    },
     onTimeUpdate() {
      const audio = this.$refs.audioRef
      if (audio) this.currentTime = audio.currentTime
    },
    onAudioLoaded() {
      const audio = this.$refs.audioRef
      if (audio && audio.duration) {
        this.audioDuration = audio.duration
        // 如果后端没返回 duration，用真实的覆盖
        if (!this.detail.duration) {
          this.$set(this.detail, 'duration', Math.floor(audio.duration))
        }
      }
    },
    onAudioEnded() {
      this.isPlaying = false
      this.currentTime = 0
    },
    onAudioError(e) {
      const audio = e.target
      if (!audio.src || !audio.src.startsWith('blob:')) {
        return
      }
      const errCode = audio.error?.code
      const messages = {
        1: '音频加载被中止',
        2: '音频网络加载失败',
        3: '音频解码失败，文件可能已损坏',
        4: '浏览器不支持该音频格式'
      }
      console.error('音频播放错误详情:', {
        code: errCode,
        message: messages[errCode] || '未知错误',
        src: this.audioSrc?.substring(0, 80),
        networkState: audio.networkState,
        readyState: audio.readyState,
        blobUrlValid: this.audioSrc?.startsWith('blob:')
      })
      // 仅当 error.code 存在时才提示用户，避免虚假 error 干扰
      if (errCode) {
        this.$message.error(messages[errCode] || '音频加载异常')
        this.isPlaying = false
      } else {
        console.warn('收到无 error.code 的 error 事件，可能是 Blob URL 预加载的正常行为')
      }
    },
    onAudioLoaded() {
      const audio = this.$refs.audioRef
      if (audio) {
        this.audioDuration = audio.duration
        // 如果接口返回的 duration 不准，以实际为准
        if (!this.detail.duration) {
          this.$set(this.detail, 'duration', Math.floor(audio.duration))
        }
      }
    },
    formatTime(ms) {
      if (ms == null) return '00:00'
      const total = Math.max(0, Math.floor(ms / 1000))
      const m = String(Math.floor(total / 60)).padStart(2, '0')
      const s = String(total % 60).padStart(2, '0')
      return `${m}:${s}`
    },
    formatDuration(sec) {
      if (!sec) return '--:--'
      const m = Math.floor(sec / 60)
      const s = sec % 60
      return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    },
    skipTime(seconds) {
      const audio = this.$refs.audioRef
      if (!audio || !this.audioDuration) {
        this.$message.warning('音频尚未加载完成')
        return
      }
      let newTime = audio.currentTime + seconds
      newTime = Math.max(0, Math.min(newTime, this.audioDuration))

      audio.currentTime = newTime
      if (this.isPlaying && audio.paused) {
        audio.play()
      }
    },
    changePlaybackRate() {
      const audio = this.$refs.audioRef
      if (!audio) return
      const currentIndex = this.playbackRates.indexOf(this.playbackRate)
      const nextIndex = (currentIndex + 1) % this.playbackRates.length
      this.playbackRate = this.playbackRates[nextIndex]
      audio.playbackRate = this.playbackRate
      this.$message.success(`播放速度已调整为 ${this.playbackRate}x`)
    }
  }
}
</script>

<style lang="scss" scoped>
/* ==========================================
   CSS 变量 (完全保留)
   ========================================== */
.meeting-detail-page {
  --main-bg: #fafbfd;
  --search-bg: #ebedf2;
  --ink: #2b2f36;
  --ink-soft: #5a606b;
  --muted: #9aa1ad;
  --blue: #2f7bff;
  --blue-soft: #d1e0fa;
  --line: rgba(20, 24, 40, .06);
  --shadow-sm: 0 1px 2px rgba(20, 24, 40, .06);
  --font: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  
  height: 100vh;
  background:
    radial-gradient(900px 500px at 70% -10%, rgba(47, 123, 255, .05), transparent 60%),
    radial-gradient(700px 600px at 100% 110%, rgba(244, 163, 192, .05), transparent 60%),
    var(--main-bg); 
  font-family: var(--font);
  color: var(--ink);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

/* ==========================================
   顶部导航栏 (完全保留)
   ========================================== */
.topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 24px; 
  flex-shrink: 0;
}

.back-btn {
  width: 36px; height: 36px; border-radius: 10px;          
  border: 1px solid var(--line); background: #fff;
  display: grid; place-items: center; cursor: pointer;
  color: var(--ink-soft); transition: background 0.2s;
  flex-shrink: 0; padding: 0; outline: none; box-sizing: border-box;         
  &:hover { background: var(--search-bg); }
}

.record-info {
  display: flex; flex-direction: row; align-items: center;
  margin-left: 0px; flex: 1; min-width: 0; gap: 0px;
}
.record-icon-svg { width: 48px; height: 48px; flex-shrink: 0; svg { width: 100%; height: 100%; } }
.record-title { 
  font-size: 18px; font-weight: 600; color: var(--ink); 
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.2;
  cursor: pointer; border-radius: 6px; padding: 2px 6px; margin-left: -6px; 
  &:hover { background: rgba(20, 24, 40, 0.04); }
}
.inline-title-input {
  font-size: 18px; font-weight: 600; color: var(--ink); line-height: 1.2; border: none;
  border-radius: 6px; padding: 2px 6px; margin-left: -6px; outline: none; background: transparent;
  width: 100%; max-width: 400px; box-sizing: border-box; font-family: var(--font);
  &::selection { background: var(--blue-soft); }
}

.top-actions { display: flex; align-items: center; gap: 10px; }
.icon-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: grid; place-items: center;
  background: transparent; border: none; cursor: pointer;
  color: #3a3f47; transition: background 0.2s, transform 0.2s;
  svg { width: 18px; height: 18px; }
  i { font-size: 20px; }
  &:hover { background: rgba(20, 24, 40, .06); transform: rotate(-15deg); }
}
.favorite-btn {
  i { color: #c0c4cc; transition: color 0.2s, transform 0.2s; }
  &.is-favorite i { color: #e6a23c; transform: scale(1.1); }
  &:hover i { color: #e6a23c; }
}
.download-btn {
  &:hover { transform: none !important; background: rgba(20, 24, 40, .06); }
  svg { color: var(--ink-soft); transition: color 0.2s, transform 0.2s; }
  &:hover svg { color: var(--blue); transform: translateY(2px); }
  &[disabled] { opacity: 0.5; cursor: not-allowed; &:hover { transform: none !important; background: transparent; svg { transform: none; color: var(--ink-soft); } } }
}

.emoji-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 32px;       /* 与普通模式 SVG 视觉大小匹配 */
  line-height: 1;
  user-select: none;
  pointer-events: none;
}

/* 搜索结果列表中的 emoji 稍微小一点以匹配其 SVG 尺寸 */
.search-result-card .emoji-icon {
  font-size: 28px;
}

/* ==========================================
   ★ 中间内容区 & 严格复刻 Tab 样式
   ========================================== */
.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 0 0 100px 0; 
  display: flex;
  flex-direction: column;
  align-items: center;
  
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 3px; }
}

/* ★ 严格复刻 HTML .tab-group */
.tab-group {
  width: 100%; 
  margin-bottom: 16px;
  position: sticky; top: 0; z-index: 10;
  padding-top: 8px;
  padding-left: 30px;
  padding-right: 30px;
}

/* 严格复刻 HTML .tab-button */
.tab-buttons {
  display: flex;
  gap: 2px; 
  background: var(--search-bg); /* #ebedf2 */
  border-radius: 999px; 
  padding: 4px; /* 对应 HTML padding */
  position: relative;
  height: 45px; /* 固定高度 */
  width: 100%;
  box-sizing: border-box;
}

/* ★ 严格复刻 HTML .tab-indicator (白色滑块) */
.tab-indicator {
  position: absolute;
  background: #fff;
  border-radius: 999px;
  transition: transform 0.3s ease, width 0.3s ease; 
  z-index: 1;
  top: 4px; 
  left: 4px;
  bottom: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  pointer-events: none;
}

/* ★ 严格复刻 HTML .tab-btn (含图标支持) */
.tab-btn {
  flex: 1;
  display: flex; 
  align-items: center; 
  justify-content: center; 
  gap: 6px; /* 图标与文字间距 */
  
  border: none; 
  background: transparent;
  font-size: 15px; 
  font-weight: 600; /* 对应 HTML 570 */
  color: var(--ink-soft); 
  
  cursor: pointer; 
  z-index: 2;
  transition: color 0.3s;
  border-radius: 999px;
  
  svg { 
    width: 16px; 
    height: 16px; 
    stroke-width: 2;
    transition: stroke 0.3s;
  }

  &.active { 
    color: var(--blue);
    svg { stroke: var(--blue); }
  }
  
  &:hover:not(.active) { 
    color: var(--blue); 
    opacity: 0.8; 
  }
}

/* 内容区域 (保持原有样式) */
.tab-content { 
  width: 100%; 
  padding: 0 33px;
  box-sizing: border-box;
}

.content-card {
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--line);
  min-height: 300px;
  animation: fadeIn 0.3s ease;
  width: 100%;
  box-sizing: border-box;
}

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.fade-in { animation: fadeIn 0.3s ease forwards; }

.meeting-detail-page .markdown-body {
  font-size: 15px;
  line-height: 1.8;
  color: #2b2f36;
  word-break: break-word;

  h1, h2, h3 {
    color: #2b2f36;
    font-weight: 700;
    margin: 24px 0 12px;
  }
  h2 {
    color: #2f7bff;
    font-size: 20px;
  }
  h3 {
    font-size: 17px;
  }
  p {
    margin: 10px 0;
  }
  strong {
    font-weight: 700;
  }
  ul, ol {
    margin: 10px 0;
    padding-left: 28px;
  }
  li {
    margin: 5px 0;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    font-size: 14px;
  }
  th, td {
    border: 1px solid #dcdfe6;
    padding: 9px 12px;
    text-align: left;
  }
  th {
    background: #f5f7fa;
    font-weight: 600;
  }
  blockquote {
    margin: 12px 0;
    padding: 8px 16px;
    border-left: 4px solid #2f7bff;
    background: #f5f7fa;
  }
}

.transcript-card { padding: 0; overflow: hidden; }
.transcript-list { max-height: 60vh; overflow-y: auto; padding: 20px; }
.transcript-item { display: flex; gap: 16px; margin-bottom: 16px; &:last-child { margin-bottom: 0; } }
.time-badge {
  flex-shrink: 0; font-size: 13px; font-weight: 600; color: var(--blue);
  background: var(--blue-soft); padding: 4px 10px; border-radius: 8px;
  height: fit-content; margin-top: 2px; font-family: monospace;
}
.transcript-text { margin: 0; line-height: 1.6; color: var(--ink-soft); font-size: 15px; }

.empty-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 300px; color: var(--muted); gap: 12px;
  i { font-size: 40px; opacity: 0.5; }
  p { font-size: 16px; }
  &.small { height: 150px; }
}

/* ==========================================
   底部播放器 (完全保留)
   ========================================== */
.audio-player {
  position: absolute; bottom: 30px; left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 60px); max-width: 600px;
  background: #fff; border-radius: 20px;
  padding: 16px 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, .15);
  display: flex; flex-direction: column; gap: 12px;
  z-index: 100; border: 1px solid rgba(0,0,0,0.02);
}

.progress-container { display: flex; align-items: center; gap: 12px; }
.time { font-size: 12px; color: var(--ink-soft); min-width: 40px; font-family: monospace; }
.progress-bar { flex: 1; height: 6px; background: var(--search-bg); border-radius: 3px; cursor: pointer; position: relative; }
.progress { height: 100%; background: var(--blue); border-radius: 3px; position: relative; transition: width 0.1s linear; }
.progress-handle {
  width: 14px; height: 14px; background: #fff; border: 2px solid var(--blue); border-radius: 50%;
  position: absolute; right: -7px; top: 50%; transform: translateY(-50%); box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.controls { display: flex; align-items: center; justify-content: center; gap: 20px; }
.control-btn {
  width: 48px; height: 48px; border-radius: 50%; background: var(--blue); color: #fff; border: none;
  display: grid; place-items: center; cursor: pointer; box-shadow: 0 4px 12px rgba(47, 123, 255, 0.3);
  transition: transform 0.2s; svg { width: 22px; height: 22px; } &:hover { transform: scale(1.05); }
  &.play-pause { background: transparent; box-shadow: none; color: var(--blue); 
    &:hover { background: rgba(47, 123, 255, 0.08); transform: scale(1.08); }}
  &.skip-btn { background: transparent; box-shadow: none; color: var(--ink-soft); svg { width: 26px; height: 26px; }
    &:hover { color: var(--blue); background: rgba(47, 123, 255, 0.06); transform: scale(1.1); }
    &:active { transform: scale(0.95); }}
  &.rate-btn { display: flex; align-items: center; justify-content: center; gap: 5px; min-width: 65px;
  height: 34px; padding: 0 12px; border-radius: 18px; background: linear-gradient( 135deg, #f5f7fa, #e8ecf3 );
  border: 1px solid rgba(0,0,0,0.08); color: #374151; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all .25s ease; box-shadow: 0 2px 6px rgba(0,0,0,.08); }
  .rate-btn:hover { transform: translateY(-2px); background: linear-gradient( 135deg, #409eff, #66b1ff );
  color:white; box-shadow: 0 5px 15px rgba(64,158,255,.35); }
  .rate-btn:active { transform: scale(.95); }
}
.more-control-btn {
  width: 36px; height: 36px; border-radius: 50%; background: var(--search-bg); border: none;
  display: grid; place-items: center; cursor: pointer; color: var(--ink-soft); font-size: 18px;
  &:hover { background: #dcdfe6; }
}

::v-deep .custom-action-dropdown {
  border-radius: 12px; padding: 6px 0;
  .el-dropdown-menu__item { padding: 10px 16px; display: flex; align-items: center; gap: 8px; }
  .icon-clr-blue { color: var(--blue); }
  .icon-clr-red { color: #f56c6c; }
}
::v-deep .back-btn {
  color: #5a606b !important;
  border-color: rgba(20, 24, 40, .06) !important;
  background: #fff !important;
}
::v-deep .back-btn svg {
  stroke-width: 2.5 !important;
  width: 18px !important;
  height: 18px !important;
}
::v-deep .record-title {
  color: #2b2f36 !important;
}

/* 点击即编辑 - 交互样式 */
.content-card {
  cursor: text; /* 整个卡片显示文本光标，暗示可编辑 */
  transition: border-color 0.2s, box-shadow 0.2s;
  
  &:hover:not(:has(.in-card-editor)) {
    border-color: rgba(47, 123, 255, 0.2);
  }
}

.in-card-editor {
  width: 100%;
  min-height: 260px;
  padding: 0;
  margin: 0;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  line-height: 1.8;
  color: var(--ink);
  font-family: var(--font);
  background: transparent;
  box-sizing: border-box;
  
  &::placeholder { color: var(--muted); opacity: 0.6; }
}
</style>

<style lang="scss">
/* ========== 详情页下拉菜单 ========== */
.custom-action-dropdown.el-dropdown-menu {
  border-radius: 20px !important;
  padding: 6px 0 !important;
  overflow: hidden;
  min-width: 160px;
  .el-dropdown-menu__item {
    font-size: 15px !important;
    line-height: 22px !important;
    padding: 10px 20px !important;
    color: #303133 !important;
    display: flex !important;
    align-items: center !important;
    i {
      font-size: 18px !important;
      margin-right: 10px !important;
      width: 22px !important;
      text-align: center !important;
      flex-shrink: 0;
    }
    span { flex: 1; }

    &:hover, &:focus {
      background-color: #f5f7fa !important;
    }
    &.custom-divider {
      position: relative;
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 20px;
        right: 20px;
        height: 1px;
        background: #ebeef5;
      }
    }
  }
  .icon-clr-blue { color: #409eff !important; }
  .icon-clr-purple { color: #9b59b6 !important; }
  .icon-clr-red { color: #f56c6c !important; }
}
/* ========== 详情页-重命名弹窗 (独立类名) ========== */
.detail-rename-dialog.el-message-box {
  border-radius: 14px !important;
  padding-bottom: 20px !important;
  .el-message-box__header { padding: 20px 24px 10px; }
  .el-message-box__title { font-size: 18px; font-weight: 600; color: #303133; }
  .el-message-box__content { padding: 10px 24px; }
  .el-message-box__input input {
    border-radius: 10px !important;
    height: 40px;
    line-height: 40px;
  }
  .el-message-box__btns {
    padding: 10px 24px 0;
    .el-button {
      border-radius: 14px !important;
      font-weight: 600;
      padding: 10px 28px;
    }
    .el-button--primary {
      background-color: #4a7dff !important;
      border-color: #4a7dff !important;
    }
  }
}
/* ========== 详情页-删除确认弹窗 (独立类名) ========== */
.detail-delete-dialog.el-message-box {
  border-radius: 14px !important;
  padding-bottom: 20px !important;
  .el-message-box__header { padding: 20px 24px 10px; }
  .el-message-box__title { font-size: 18px; font-weight: 600; color: #303133; }
  .el-message-box__message p { font-size: 14px; color: #606266; line-height: 1.6; }
  .el-message-box__content { padding: 10px 24px; }
  .el-message-box__btns {
    padding: 10px 24px 0;
    .el-button {
      border-radius: 14px !important;
      font-weight: 600;
      padding: 10px 28px;
    }
    /* 取消按钮 */
    .el-button:first-child {
      background-color: #f5f6f8 !important;
      border-color: #dcdfe6 !important;
      color: #606266 !important;
      &:hover { background-color: #e8eaed !important; }
    }
    /* 删除按钮 - 红色 */
    .el-button:last-child {
      background-color: #f56c6c !important;
      border-color: #f56c6c !important;
      color: #fff !important;
      &:hover { background-color: #f78989 !important; border-color: #f78989 !important; }
    }
  }
}
</style>