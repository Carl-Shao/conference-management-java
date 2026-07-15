import request from '@/utils/request'

// 查询音频处理列表
export function listAudio(query) {
  return request({
    url: '/huiyi/audio/list',
    method: 'get',
    params: query
  })
}

// 上传音频文件
export function uploadAudio(data) {
  return request({
    url: '/huiyi/audio/upload',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: data
  })
}

// 获取音频处理任务状态
export function getAudioTaskStatus(taskId) {
  return request({
    url: '/huiyi/audio/task/' + taskId,
    method: 'get'
  })
}

// 获取批量音频处理任务状态
export function getBatchAudioTaskStatus(taskIds) {
  return request({
    url: '/huiyi/audio/task/batch',
    method: 'post',
    data: taskIds
  })
}

// 获取音频详情
export function getAudio(id) {
  return request({
    url: '/huiyi/audio/' + id,
    method: 'get'
  })
}

// 删除音频
export function delAudio(id) {
  return request({
    url: '/huiyi/audio/' + id,
    method: 'delete'
  })
}