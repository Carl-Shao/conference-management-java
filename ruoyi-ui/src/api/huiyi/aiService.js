import request from '@/utils/request'

// 健康检查
export function healthCheck() {
  return request({
    url: '/huiyi/ai/health',
    method: 'get'
  })
}

// 启动ASR录音
export function startAsrRecording(data) {
  return request({
    url: '/huiyi/ai/asr/start',
    method: 'post',
    data: data
  })
}

// 启动完整录音
export function startFullRecording(data) {
  return request({
    url: '/huiyi/ai/recording/start',
    method: 'post',
    data: data
  })
}

// 停止ASR录音
export function stopAsrRecording(roomId) {
  return request({
    url: '/huiyi/ai/asr/stop',
    method: 'post',
    params: { roomId: roomId }
  })
}

// 停止完整录音
export function stopFullRecording() {
  return request({
    url: '/huiyi/ai/recording/stop',
    method: 'post'
  })
}

// 获取录音路径
export function getRecordingPath(roomId) {
  return request({
    url: '/huiyi/ai/recording/path/' + roomId,
    method: 'get'
  })
}

// 获取转录文本
export function getTranscript(roomId) {
  return request({
    url: '/huiyi/ai/asr/transcript/' + roomId,
    method: 'get'
  })
}

// 生成会议纪要
export function generateMinutes(data) {
  return request({
    url: '/huiyi/ai/minutes/generate',
    method: 'post',
    data: data
  })
}

// 获取会议纪要
export function getMinutes(roomId) {
  return request({
    url: '/huiyi/ai/minutes/' + roomId,
    method: 'get'
  })
}

// 获取服务状态
export function getStatus() {
  return request({
    url: '/huiyi/ai/status',
    method: 'get'
  })
}