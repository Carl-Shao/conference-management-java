import request from '@/utils/request'

// 开始录制
export function startRecord(meetingId) {
  return request({
    url: `/huiyi/record/${meetingId}/start`,
    method: 'post'
  })
}

// 暂停录制
export function pauseRecord(meetingId) {
  return request({
    url: `/huiyi/record/${meetingId}/pause`,
    method: 'post'
  })
}

// 继续录制
export function resumeRecord(meetingId) {
  return request({
    url: `/huiyi/record/${meetingId}/resume`,
    method: 'post'
  })
}

// 结束录制
export function stopRecord(meetingId) {
  return request({
    url: `/huiyi/record/${meetingId}/stop`,
    method: 'post'
  })
}

// 获取录制状态
export function getRecordStatus(meetingId) {
  return request({
    url: `/huiyi/record/${meetingId}/status`,
    method: 'get'
  })
}