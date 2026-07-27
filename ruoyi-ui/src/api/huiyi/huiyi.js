import request from '@/utils/request'
 
// 查询会议列表（首页全部会议）
export function listMeeting(query) {
  return request({
    url: '/huiyi/meeting/list',
    method: 'get',
    params: query
  })
}
 
// 查询会议详细信息
export function getMeeting(meetingId) {
  return request({
    url: '/huiyi/meeting/' + meetingId,
    method: 'get'
  })
}
 
// 发起会议
// data: { title, roomName, ... }
// 返回新建的会议记录（包含 meetingId），前端拿到后跳转到会议进行中页面
export function startMeeting(data) {
  return request({
    url: '/huiyi/meeting/start',
    method: 'post',
    data: data
  })
}
 
// 暂停会议（暂停音频采集/转写）
export function pauseMeeting(meetingId) {
  return request({
    url: '/huiyi/meeting/pause/' + meetingId,
    method: 'put'
  })
}
 
// 继续会议
export function resumeMeeting(meetingId) {
  return request({
    url: '/huiyi/meeting/resume/' + meetingId,
    method: 'put'
  })
}
 
// 结束会议（会触发后端 LLM 生成 AI 纪要的异步任务）
export function endMeeting(meetingId) {
  return request({
    url: '/huiyi/meeting/end/' + meetingId,
    method: 'put'
  })
}
 
// 在当前会议进度打标记
// data: { pointMs, label }  pointMs 为相对会议开始的毫秒偏移，label 为标记备注（可选）
export function markMeeting(meetingId, data) {
  return request({
    url: '/huiyi/meeting/mark/' + meetingId,
    method: 'post',
    data: data
  })
}
 
// 查询某次会议的所有标记
export function listMarks(meetingId) {
  return request({
    url: '/huiyi/meeting/mark/' + meetingId,
    method: 'get'
  })
}
 
// 增量拉取实时转写内容
// lastId 为客户端已拥有的最后一条转写记录 id，传 0 或不传则拉取全部
export function getTranscript(meetingId, lastId) {
  return request({
    url: '/huiyi/meeting/transcript/' + meetingId,
    method: 'get',
    params: { lastId }
  })
}
 
// 获取完整转写全文（用于会议详情页一次性展示）
export function getFullTranscript(meetingId) {
  return request({
    url: '/huiyi/meeting/transcript/' + meetingId + '/full',
    method: 'get'
  })
}
 
// 保存 / 更新笔记（自动保存场景下前端做防抖后调用）
export function saveNote(meetingId, data) {
  return request({
    url: '/huiyi/meeting/note/' + meetingId,
    method: 'post',
    data: data
  })
}
 
// 获取笔记
export function getNote(meetingId) {
  return request({
    url: '/huiyi/meeting/note/' + meetingId,
    method: 'get'
  })
}
 
// 获取 AI 会议纪要（摘要/待办/决策等结构化内容）
export function getMeetingSummary(meetingId) {
  return request({
    url: '/huiyi/meeting/summary/' + meetingId,
    method: 'get'
  })
}
 
// 手动触发重新生成 AI 会议纪要
export function regenerateSummary(meetingId) {
  return request({
    url: '/huiyi/meeting/summary/regenerate/' + meetingId,
    method: 'post'
  })
}
 
// 删除会议
export function delMeeting(meetingId) {
  return request({
    url: '/huiyi/meeting/' + meetingId,
    method: 'delete'
  })
}
 
// 构造实时转写 WebSocket 地址（用于接收转写文本推送 + 发送二进制 PCM 音频帧）
// 约定：连接建立后，前端持续发送二进制 PCM16 音频帧；
// 服务端通过文本帧（JSON）推送转写结果，格式如：
// { type: 'transcript', id, speaker, text, startMs, endMs, final }
export function buildMeetingWsUrl(meetingId) {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const baseUrl = process.env.VUE_APP_BASE_API || ''
  const host = baseUrl.startsWith('http') ? baseUrl.replace(/^https?:\/\//, '') : window.location.host
  return `${protocol}://${host}/websocket/huiyi/meeting/${meetingId}`
}