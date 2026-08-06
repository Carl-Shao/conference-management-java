<template>
    <div id="app">
        <div class="recording-page">
            <!-- 顶部信息栏 -->
            <div class="recording-header">
                <div class="header-left">
                    <button class="el-button el-button--default is-circle" @click="handleBack" style="padding: 10px;">
                        <i class="el-icon-arrow-left" style="font-size: 20px;"></i>
                    </button>
                    <div class="meeting-info">
                        <div class="meeting-meta">
                            <div class="meeting-title">{{ meeting.title || 'AI听记' }}</div>
                            <span class="rec-status"
                                :class="{ 'rec-paused': recordStatus === 'paused', 'rec-ended': recordStatus === 'ended' }">
                                <span class="rec-dot"
                                    :class="{ 'rec-paused': recordStatus === 'paused', 'rec-ended': recordStatus === 'ended' }"></span>
                                {{ recordStatus === 'idle' ? '待开始' : recordStatus === 'paused' ? '已暂停' : recordStatus === 'ended' ? '已结束' : '录制中' }}
                            </span>
                        </div>
                    </div>
                </div>
                <div class="header-right">
                    <el-tag v-if="connected" type="success" size="medium" effect="plain"
                        style="font-size: 14px;">麦克风已连接</el-tag>
                    <el-tag v-else type="info" size="medium" effect="plain" style="font-size: 14px;">连接中...</el-tag>
                </div>
            </div>

            <!-- 主体内容：左右分栏 -->
            <div class="recording-body">
                <!-- 左侧：实时转写 -->
                <div class="transcript-panel" style="position: relative;">
                    <div class="panel-header">
                        <i class="el-icon-microphone"></i>
                        <span class="panel-title">实时转写</span>
                    </div>
                    <div class="transcript-content" ref="transcriptScroll">
                        <div v-if="transcripts.length === 0" class="empty-transcript">
                            <i class="el-icon-microphone-off"></i>
                            <p>等待发言中，转写内容将实时显示</p>
                        </div>
                        <div v-for="(line, index) in transcripts" :key="index" class="transcript-item"
                            :class="{ marked: markedIds.has(line.id) }">
                            <div class="speaker-info">
                                <span class="speaker-name">{{ line.speaker || '发言人' }}</span>
                                <span class="timestamp">{{ formatOffset(line.startMs) }}</span>
                            </div>
                            <div class="transcript-text">{{ line.text }}</div>
                        </div>
                    </div>
                    <!-- 控制组件覆盖在实时转写面板底部 -->
                    <div class="controls-overlay">
                        <!-- 底部控制按钮 -->
                        <div class="control-buttons">
                            <!-- 还没开始录制：只显示"开始听记"按钮 -->
                            <template v-if="recordStatus === 'idle'">
                                <el-button type="primary" round :loading="isApiLoading" @click="startListening">
                                    <i class="el-icon-video-camera" style="margin-right: 6px;"></i>开始听记
                                </el-button>
                            </template>
                            <!-- 已经开始录制：显示 标记/暂停继续/结束 三个按钮 -->
                            <template v-else>
                                <!-- 标记按钮 -->
                                <div class="control-btn" @click="handleMark">
                                    <i class="el-icon-collection-tag" style="font-size: 32px;"></i>
                                </div>

                                <!-- 暂停/继续按钮 -->
                                <div class="control-btn" @click="handleToggleRecord"
                                    :style="{ opacity: recordStatus === 'ended' ? 0.5 : 1, cursor: recordStatus === 'ended' ? 'not-allowed' : 'pointer' }">
                                    <i :class="recordStatus === 'paused' ? 'el-icon-video-play' : 'el-icon-video-pause'"
                                        style="font-size: 32px;" :disabled="recordStatus === 'ended'"></i>
                                </div>

                                <!-- 结束会议按钮 -->
                                <div class="control-btn" @click="handleEnd">
                                    <i class="el-icon-switch-button" style="font-size: 32px;"></i>
                                </div>
                            </template>
                        </div>

                        <!-- 音频可视化区域 和 计时器 -->
                        <div class="audio-visualizer">
                            <div class="wave-container">
                                <div class="wave-bar" v-for="n in 30" :key="n"
                                    :style="{ height: recordStatus === 'paused' ? '3px' : waveHeights[n - 1] + 'px' }"
                                    :class="{ paused: recordStatus === 'paused' }"></div>
                            </div>
                            <div class="visualizer-label">
                                <span class="timer">{{ formattedTimer }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 右侧：笔记 -->
                <div class="notes-panel">
                    <div class="panel-header">
                        <i class="el-icon-edit"></i>
                        <span class="panel-title">我的笔记</span>
                        <span class="save-status">{{ saveStatus }}</span>
                    </div>
                    <div class="notes-content">
                        <el-input type="textarea" v-model="notesContent" class="notes-textarea" :rows="20"
                            placeholder="记录会议要点、待办事项..." @input="debouncedSaveNotes" />
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { addMeeting } from '@/api/huiyi/minutes'
import { 
    startRecord, 
    pauseRecord, 
    resumeRecord, 
    stopRecord
} from '@/api/huiyi/record';

export default {
    name: 'MeetingRecorder',
    props: {
        // 保留 prop 方式（适用于 router-view 直接嵌入的场景）
        from: {
            type: [String, Object],
            default: ''
        }
    },
    data() {
        return {
            meeting: {
                title: 'AI听记',
                id: null
            },
            recordStatus: 'idle', // idle | ongoing | paused | ended
            elapsedSeconds: 0,
            timerInterval: null,

            // 音频可视化
            waveHeights: Array(30).fill(2), // 默认高度为2px（平直线）
            connected: false,

            // 转写数据
            transcripts: [],
            markedIds: new Set(),

            // 笔记数据
            notesContent: '',
            saveStatus: '',

            // 音频相关
            audioContext: null,
            analyser: null,
            microphone: null,
            animationFrame: null,

            recording: false,
            ws: null,
            recorderNode: null,

            // 笔记保存定时器
            saveTimeout: null,
            _simulateInterval: null,
            isApiLoading: false
        };
    },

    computed: {
        formattedTimer() {
            const h = String(Math.floor(this.elapsedSeconds / 3600)).padStart(2, '0');
            const m = String(Math.floor((this.elapsedSeconds % 3600) / 60)).padStart(2, '0');
            const s = String(this.elapsedSeconds % 60).padStart(2, '0');
            return `${h}:${m}:${s}`;
        }
    },

    activated() {
        this._enableRecordingLayout();
    },

    deactivated() {
        this._disableRecordingLayout(); 
    },

    beforeDestroy() {
        this._disableRecordingLayout(); 
        this.cleanupResources();
    },

    beforeRouteLeave(to, from, next) {
        if (this.recordStatus === 'ongoing' || this.recordStatus === 'paused') {
            const answer = window.confirm('会议正在录制中，确定要离开吗？');
            if (answer) {
                this.cleanupResources();
                this._disableRecordingLayout(); 
                next();
            } else {
                next(false);
            }
        } else {
            // ✅ 只移除class，不主动openSideBar
            this._disableRecordingLayout(); 
            next();
        }
    },

    created() {
        // 如果是带着已有 meetingId 进来的（比如从"全部会议纪要"列表点进一条还没结束的录制），
        // 只用来展示标题，不自动开始录制——录制只能由用户点"开始听记"触发
        this.meeting.id = this.$route.params.id || this.$route.query.meetingId || this.from?.id;
        this._enableRecordingLayout();
        this.startTimer();
        this.$nextTick(() => this.startMicrophone());
        // 注意：这里不再自动调用 startRecord。录制状态默认是 'idle'，
        // 用户点击"开始听记"按钮时才会创建会议记录并真正开始录制。
    },

    methods: {
         _enableRecordingLayout() {
            document.body.classList.add('recording-fullscreen');
        },
        _disableRecordingLayout() {
            // 1. 先移除全屏class（解除 !important 锁定）
            document.body.classList.remove('recording-fullscreen');
            this.$store.dispatch('app/openSideBar', {
                withoutAnimation: true
            });
        },
        cleanupResources() {
            // 先关掉WebSocket和AudioWorklet节点（比如用户没点"结束会议"、直接刷新/离开页面的情况）
            this.cleanupRecording();

            if (this.timerInterval) clearInterval(this.timerInterval);
            if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
            if (this._simulateInterval) clearInterval(this._simulateInterval);
            if (this.saveTimeout) clearTimeout(this.saveTimeout);

            // 停止麦克风流
            if (this.microphone) {
                this.microphone.disconnect();
                // 获取并停止所有媒体轨道
                const stream = this.microphone.mediaStream;
                if (stream) {
                    stream.getTracks().forEach(track => track.stop());
                }
            }

            if (this.audioContext && this.audioContext.state !== 'closed') {
                this.audioContext.close();
            }

            this.connected = false;
        },
        // 启动计时器
        startTimer() {
            this.timerInterval = setInterval(() => {
                if (this.recordStatus === 'ongoing') {
                    this.elapsedSeconds++;
                }
            }, 1000);
        },

        // 开始使用麦克风
        async startMicrophone() {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

                this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
                this.analyser = this.audioContext.createAnalyser();
                this.microphone = this.audioContext.createMediaStreamSource(stream);

                this.analyser.fftSize = 256;
                this.microphone.connect(this.analyser);

                this.connected = true;

                // 开始音频可视化
                this.visualizeAudio();

                this.$message.success('麦克风已连接');
            } catch (err) {
                console.error('无法访问麦克风:', err);
                this.$message.error('无法访问麦克风，请检查权限设置');
                this.simulateAudioVisualization();
            }
        },

        // 音频可视化
        visualizeAudio() {
            const bufferLength = this.analyser.frequencyBinCount;
            const dataArray = new Uint8Array(bufferLength);

            const update = () => {
                if (this.analyser && this.recordStatus === 'ongoing') {
                    this.analyser.getByteFrequencyData(dataArray);

                    // 将音频数据映射到波形高度
                    for (let i = 0; i < 30; i++) {
                        const value = dataArray[Math.floor(i * bufferLength / 30)] || 0;
                        // 根据实际音频值调整高度，范围在2-25px之间
                        const height = Math.max(2, Math.min(25, value / 6 + 2));
                        this.$set(this.waveHeights, i, height);
                    }
                } else if (this.recordStatus === 'paused') {
                    // 暂停时重置为平直线
                    for (let i = 0; i < 30; i++) {
                        this.$set(this.waveHeights, i, 2);
                    }
                }

                if (this.recordStatus === 'ongoing') {
                    this.animationFrame = requestAnimationFrame(update);
                }
            };

            update();
        },

        // 模拟音频可视化（当无法访问麦克风时）
        simulateAudioVisualization() {
            const interval = setInterval(() => {
                if (this.recordStatus === 'paused' || this.recordStatus === 'ended') {
                    // 暂停或结束后显示平直线
                    for (let i = 0; i < 30; i++) {
                        this.$set(this.waveHeights, i, 2);
                    }
                } else {
                    // 模拟动态音量波浪效果
                    for (let i = 0; i < 30; i++) {
                        this.$set(this.waveHeights, i, Math.random() * 15 + 5);
                    }
                }

                // 如果组件已销毁或录音已结束且不需要模拟，可考虑清除interval
                // 这里为了保持与原逻辑一致，未做额外清除，实际项目中建议在beforeDestroy中处理
            }, 100);

            // 将interval保存到data或实例属性以便销毁时清除（原代码未做，此处为严谨补充）
            this._simulateInterval = interval;
        },

        // 添加转写内容（用于模拟或未来接入语音识别）
        addTranscript(text, speaker = '发言人') {
            const newLine = {
                id: Date.now(),
                speaker: speaker,
                text: text,
                startMs: this.elapsedSeconds * 1000
            };

            this.transcripts.push(newLine);

            // 滚动到底部
            this.$nextTick(() => {
                const el = this.$refs.transcriptScroll;
                if (el) el.scrollTop = el.scrollHeight;
            });
        },

        // 格式化时间偏移
        formatOffset(ms) {
            if (!ms && ms !== 0) return '';
            const totalSec = Math.floor(ms / 1000);
            const m = String(Math.floor(totalSec / 60)).padStart(2, '0');
            const s = String(totalSec % 60).padStart(2, '0');
            return `${m}:${s}`;
        },

        // 暂停/继续会议
        async handleToggleRecord() {
            console.log('[排查] handleToggleRecord被调用, recordStatus=', this.recordStatus, ' isApiLoading=', this.isApiLoading, ' meeting.id=', this.meeting.id);
            if (this.recordStatus === 'idle' || this.recordStatus === 'ended' || this.isApiLoading) {
                console.log('[排查] 被守卫拦下了，没有继续往下执行');
                return;
            }

            this.isApiLoading = true;
            const prevStatus = this.recordStatus;

            try {
                if (this.recordStatus === 'ongoing') {
                    await pauseRecord(this.meeting.id);
                    this.recordStatus = 'paused';
                    this.$message.success('会议已暂停');
                    for (let i = 0; i < 30; i++) this.$set(this.waveHeights, i, 2);
                } else {
                    await resumeRecord(this.meeting.id);
                    this.recordStatus = 'ongoing';
                    this.$message.success('会议已继续');
                    if (this.analyser) this.visualizeAudio();
                }
            } catch (error) {
                console.error('切换录制状态失败:', error);
                this.$message.error('操作失败，请重试');
                // 操作失败时回滚 UI 状态
                this.recordStatus = prevStatus;
            } finally {
                this.isApiLoading = false;
            }
        },

        // 添加标记
        handleMark() {
            this.$message.success(`已在 ${this.formattedTimer} 处添加标记`);
            if (this.transcripts.length > 0) {
                const lastId = this.transcripts[this.transcripts.length - 1].id;
                this.markedIds.add(lastId);
            }
        },

        // 结束会议
        async handleEnd() {
            console.log('[排查] handleEnd被调用, isApiLoading=', this.isApiLoading, ' recordStatus=', this.recordStatus, ' meeting.id=', this.meeting.id);
            if (this.isApiLoading) {
                console.log('[排查] 被isApiLoading拦下了');
                return;
            }
            if (this.recordStatus === 'idle') {
                // 还没开始录制，没什么可结束的，直接当返回处理
                this.handleBack();
                return;
            }

            if (confirm('确认结束当前会议吗？')) {
                this.isApiLoading = true;
                try {
                    const durationMs = this.elapsedSeconds * 1000;
                    // stopListening 内部会关掉 WebSocket / AudioWorklet，并调用后端 /stop 接口
                    await this.stopListening();

                    if (this.timerInterval) clearInterval(this.timerInterval);
                    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
                    if (this.microphone) this.microphone.disconnect();
                    if (this.audioContext && this.audioContext.state !== 'closed') this.audioContext.close();

                    this.recordStatus = 'ended';
                    for (let i = 0; i < 30; i++) this.$set(this.waveHeights, i, 2);
                } catch (error) {
                    console.error('结束会议失败:', error);
                    this.$message.error('结束会议失败，请重试');
                } finally {
                    this.isApiLoading = false;
                }
            }
        },


        handleBack() {
            const backRouteStr = this.$route.query.backRoute;

            if (!backRouteStr) {
                this.$router.push('/index');
                return;
            }

            try {
                const routeInfo = JSON.parse(backRouteStr);

                if (routeInfo.name) {
                    this.$router.push({
                        name: routeInfo.name,
                        query: routeInfo.query || {}
                    });
                } else {
                    // 兜底：如果没有 name（极端情况），再用 path
                    this.$router.push({
                        path: routeInfo.path,
                        query: routeInfo.query || {}
                    });
                }
            } catch (e) {
                console.error('解析 backRoute 失败', e);
                this.$router.push('/index');
            }
        },

        // 防抖保存笔记
        debouncedSaveNotes() {
            this.saveStatus = '编辑中...';

            if (this.saveTimeout) {
                clearTimeout(this.saveTimeout);
            }

            this.saveTimeout = setTimeout(() => {
                this.saveStatus = '已保存';
                setTimeout(() => {
                    this.saveStatus = '';
                }, 2000);
            }, 1000);
        },

        /**
         * 根据项目里已经在用的后端API地址（VUE_APP_BASE_API）推导WebSocket该连的地址。
         *
         * - 如果 VUE_APP_BASE_API 是绝对地址（比如 http://localhost:8080）：
         *   直接从这里换算出后端host，WebSocket直接连后端，完全绕开前端devServer代理，
         *   彻底避开"代理没开 ws: true 导致WS升级请求到不了后端"这个问题。
         * - 如果 VUE_APP_BASE_API 是相对路径（比如 /dev-api，说明走的是devServer代理）：
         *   没法从这里推出后端真实host，只能沿用走代理这条路，
         *   这种情况必须去 vue.config.js 的 devServer.proxy 对应规则里加上 ws: true，
         *   代码这边是绕不开的，需要改配置文件。
         */
        buildWsUrl(wsPath) {
            const base = process.env.VUE_APP_BASE_API || '';
            if (/^https?:\/\//i.test(base)) {
                const backendOrigin = base.replace(/^http/i, 'ws').replace(/\/$/, '');
                return backendOrigin + wsPath;
            }
            const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
            return protocol + location.host + wsPath;
        },

        async startListening() {
            if (this.recording || this.isApiLoading) {
                return; // 防止重复点击
            }
            // 先乐观地切UI状态，让点击有立刻的反馈，不等下面这一串网络请求跑完
            this.recording = true;
            this.recordStatus = 'ongoing';
            if (this.analyser) this.visualizeAudio();

            this.isApiLoading = true;
            try {
                // 1. 建会议记录
                const addRes = await addMeeting({
                    title: this.meeting.title || ('会议记录 ' + new Date().toLocaleString()),
                    sourceType: '0'
                });
                this.meeting.id = addRes.data;
 
                // 2. 开始录制，拿 wsPath（具体字段名以你 MeetingRecordVO 实际结构为准，
                //    如果不是 data.wsPath 而是别的字段名，这里改一下就行）
                const startRes = await startRecord(this.meeting.id);
                const wsPath = startRes.data.wsPath;
 
                // 3. 建 WebSocket 连接
                // 不用 location.host 拼（那是前端开发服务器的地址，WebSocket升级请求
                // 默认不会走devServer的HTTP代理，除非代理配置显式开了 ws: true），
                // 改成从项目里已经在用的后端API地址（VUE_APP_BASE_API）推导出真实后端host
                const wsUrl = this.buildWsUrl(wsPath);
                this.ws = new WebSocket(wsUrl);
                this.ws.binaryType = 'arraybuffer';
 
                await new Promise((resolve, reject) => {
                    let settled = false; // 防止 resolve/reject 被调用多次

                    // 10秒内没连上就当失败处理，不让isApiLoading永远卡在pending
                    const timeoutId = setTimeout(() => {
                        if (!settled) {
                            settled = true;
                            reject(new Error('WebSocket连接超时（10秒），请检查后端服务是否正常、握手是否被拒绝'));
                        }
                    }, 10000);

                    this.ws.onopen = () => {
                        if (settled) return;
                        settled = true;
                        clearTimeout(timeoutId);
                        resolve();
                    };
                    this.ws.onerror = (e) => {
                        if (settled) return;
                        settled = true;
                        clearTimeout(timeoutId);
                        reject(e);
                    };
                    // 后端转写结果会通过这个连接推回来（MeetingSessionManager.pushToClient）
                    this.ws.onmessage = (event) => {
                        try {
                            const dto = JSON.parse(event.data);
                            this.onTranscriptPush(dto); // 你自己实现：把分段转写文本追加到页面上
                        } catch (e) {
                            // 心跳等非JSON文本消息，忽略即可
                        }
                    };
                    // 之前这里只打了个警告日志，没有reject——如果握手在onopen之前就被拒绝，
                    // Promise永远不会settle，await会一直挂着，isApiLoading就永远是true
                    this.ws.onclose = (event) => {
                        if (!settled) {
                            settled = true;
                            clearTimeout(timeoutId);
                            reject(new Error('WebSocket连接被关闭（code=' + event.code + '），未能建立成功，可能是握手被后端拒绝'));
                            return;
                        }
                        if (this.recording) {
                            console.warn('[MeetingRecorder] WebSocket意外断开');
                        }
                    };
                });
 
                // 4. 加载AudioWorklet，把麦克风流接上去，开始推PCM数据
                //    如果 startMicrophone() 里的 audioContext 采样率跟后端配置的 sampleRate 不一致，
                //    要在这里重建一个指定采样率的 AudioContext，不能直接用可视化那个，
                //    否则后端按固定采样率算出来的时长/WAV头会跟实际数据不匹配
                if (!this.audioContext || !this.microphone) {
                    throw new Error('麦克风未就绪，请检查 startMicrophone 是否已成功执行');
                }
                await this.audioContext.audioWorklet.addModule('/worklets/pcm-recorder-processor.js');
                this.recorderNode = new AudioWorkletNode(this.audioContext, 'pcm-recorder-processor');
                this.microphone.connect(this.recorderNode);
 
                this.recorderNode.port.onmessage = (event) => {
                    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                        this.ws.send(event.data);
                    }
                };
 
                this.$message.success('已开始录制');
            } catch (err) {
                console.error('[MeetingRecorder] 开始录制失败', err);
                this.$message.error('开始录制失败: ' + (err.message || err));
                // 网络请求失败了，把刚才乐观切过去的UI状态回滚回idle
                this.recording = false;
                this.recordStatus = 'idle';
                this.cleanupRecording();
            } finally {
                this.isApiLoading = false;
            }
        },
        async stopListening() {
            if (!this.recording) {
                return;
            }
            this.cleanupRecording();
            try {
                await stopRecord(this.meeting.id);
                this.$message.success('录制已结束，正在生成纪要');
            } catch (err) {
                console.error('[MeetingRecorder] 结束录制失败', err);
                this.$message.error('结束录制失败: ' + (err.message || err));
            }
        },
        cleanupRecording() {
            this.recording = false;
            if (this.recorderNode) {
                this.recorderNode.disconnect();
                this.recorderNode.port.onmessage = null;
                this.recorderNode = null;
            }
            if (this.ws) {
                this.ws.onmessage = null;
                this.ws.onclose = null;
                this.ws.close();
                this.ws = null;
            }
        }
    }
};
</script>

<style scoped>
/* 
  注意：原HTML中的样式是全局的。
  在Vue SFC中，为了保证1:1复刻且不影响其他组件，
  这里去掉了scoped或者你可以保留scoped但需确保Element UI的样式穿透正常。
  考虑到原代码直接修改了body和*，建议在App.vue或全局样式中处理reset，
  此处仅保留#app内部的样式以确保组件独立性。
*/

* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

/* body 样式建议移至全局，此处仅作为参考 */
/* 
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background-color: #f8fafd;
  height: 100vh;
  overflow: hidden;
} 
*/

#app {
    height: 100vh;
    display: flex;
    flex-direction: column;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    background-color: #f8fafd;
    overflow: hidden;
}

/* 录音页面样式 */
.recording-page {
    height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f8fafd;
}

.recording-header {
    height: 60px;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    z-index: 10;
    position: relative;
}

.header-left {
    display: flex;
    align-items: center;
}

.meeting-info {
    margin-left: 20px;
}

.meeting-title {
    font-size: 20px;
    font-weight: 600;
    color: #1d2129;
    margin-right: 16px;
    /* 添加右边距 */
}

.meeting-meta {
    display: flex;
    align-items: center;
    gap: 16px;
}

.timer {
    font-family: 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
    font-size: 18px;
    font-weight: 600;
    color: #1d2129;
}

.rec-status {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 16px;
    color: #52c41a;
}

.rec-status.rec-paused {
    color: #faad14;
}

.rec-status.rec-ended {
    color: #f56c6c;
}

.rec-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #52c41a;
    animation: blink 1.2s infinite;
}

.rec-dot.rec-paused {
    background: #faad14;
    animation: none;
}

.rec-dot.rec-ended {
    background: #f56c6c;
    animation: none;
}

@keyframes blink {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.3;
    }
}

.recording-body {
    flex: 1;
    display: flex;
    padding: 16px 16px 0;
    gap: 16px;
    overflow: hidden;
}

.transcript-panel,
.notes-panel {
    flex: 1;
    background: #fff;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.panel-header {
    display: flex;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f0;
}

.panel-header i {
    font-size: 20px;
    color: #1890ff;
    margin-right: 8px;
}

.panel-title {
    font-size: 18px;
    font-weight: 600;
    color: #1d2129;
}

.save-status {
    margin-left: auto;
    font-size: 16px;
    color: #86909c;
}

.transcript-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px 100px 24px;
    /* 底部留出空间放置控件 */
}

.empty-transcript {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #c0c4cc;
}

.empty-transcript i {
    font-size: 64px;
    margin-bottom: 20px;
}

.empty-transcript p {
    font-size: 18px;
}

.transcript-item {
    padding: 16px 0;
    border-bottom: 1px solid #f5f5f5;
}

.transcript-item.marked {
    background: #f0f9ff;
    border-left: 4px solid #1890ff;
    padding-left: 20px;
}

.speaker-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}

.speaker-name {
    font-size: 16px;
    font-weight: 600;
    color: #1890ff;
}

.timestamp {
    font-size: 14px;
    color: #c0c4cc;
}

.transcript-text {
    font-size: 16px;
    line-height: 1.8;
    color: #1d2129;
}

.notes-content {
    flex: 1;
    padding: 0 24px 24px;
}

.notes-textarea textarea {
    resize: none;
    border: none;
    height: 100% !important;
    padding: 0;
    font-size: 16px;
    line-height: 1.8;
}

/* 在实时转写面板底部添加控件区域 */
.controls-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    padding: 15px 20px;
    border-radius: 0 0 12px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    z-index: 10;
}

.audio-visualizer {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-left: 30px;
    /* 修改为左边距 */
}

.wave-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    height: 40px;
    width: 250px;
}

.wave-bar {
    width: 5px;
    background: #52c41a;
    border-radius: 2px;
    transition: height 0.1s ease;
    min-height: 3px;
}

.wave-bar.paused {
    height: 3px !important;
    background: #c0c4cc;
}

.visualizer-label {
    text-align: center;
    font-size: 14px;
    color: #86909c;
    margin-top: 5px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    /* 添加间隔 */
}

.visualizer-label .timer {
    font-size: 14px;
    font-weight: 600;
    color: #1d2129;
}

/* 控制按钮 */
.control-buttons {
    display: flex;
    gap: 15px;
}

.control-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: auto;
    /* 改为自动宽度 */
    height: auto;
    /* 改为自动高度 */
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s;
    color: #606266;
    font-size: 28px;
    /* 增大图标大小 */
    padding: 12px;
    /* 添加内边距 */
    background: transparent;
    /* 透明背景 */
    border: none;
    outline: none;
}

.control-btn:hover {
    color: #1890ff;
    background: rgba(24, 144, 255, 0.1);
}

.control-btn span {
    margin-top: 6px;
    font-size: 14px;
    color: #606266;
    display: none;
    /* 隐藏文字 */
}

/* 响应式设计 */
@media (max-width: 1200px) {
    .recording-body {
        flex-direction: column;
    }

    .controls-overlay {
        flex-direction: column;
        gap: 10px;
        align-items: flex-start;
    }

    .audio-visualizer {
        margin-right: 0;
        margin-bottom: 10px;
    }

    .control-buttons {
        width: 100%;
        justify-content: space-around;
    }
}
</style>

<style>
body.recording-fullscreen .sidebar-container,
body.recording-fullscreen .el-aside {
    display: none !important;
    width: 0 !important;
    min-width: 0 !important;
    overflow: hidden !important;
    transition: none !important;
}

body.recording-fullscreen .main-container,
body.recording-fullscreen .app-main {
    margin-left: 0 !important;
    padding-left: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    transition: none !important;
}
</style>

<style>
/* 仅当全屏模式解除时生效 */
body:not(.recording-fullscreen) .app-wrapper {
    display: flex !important;
    flex-direction: row !important;
    align-items: stretch !important;
}

body:not(.recording-fullscreen) .sidebar-container {

    display:flex!important;
    flex-direction:column!important;

    width:300px!important;
    min-width:300px!important;

    position:relative!important;

    top:0;
    bottom:0;
    left:0;

    height:100vh!important;

    z-index:1001!important;

    overflow:hidden!important;
}

body:not(.recording-fullscreen) .main-container {
    flex: 1 !important;            /* 关键：让主区域自动填满剩余空间 */
    min-width: 0 !important;       /* 防止flex子项溢出 */
    margin-left: 0 !important;     /* 清除可能残留的负margin */
}
/* 接在之前的 body:not(.recording-fullscreen) 规则之后 */



/* 3. 确保菜单项容器不被裁切 */
body:not(.recording-fullscreen) .sidebar-container .el-menu-item,
body:not(.recording-fullscreen) .sidebar-container .el-submenu__title {
    overflow: visible !important;
    display: flex !important;
    align-items: center !important;
}
</style>