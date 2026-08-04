import request from '@/utils/request'

// 上传音频文件
export function uploadAudio(files) {
  const formData = new FormData()
  for (let i = 0; i < files.length; i++) {
    formData.append('files', files[i])
  }
  return request({
    url: '/huiyi/audio/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData
  })
}
