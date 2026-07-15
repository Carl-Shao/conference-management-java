// 查询任务状态（模拟API）
export function getTaskStatus(taskId) {
  // 实际应用中应该调用后端API
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        code: 200,
        data: {
          taskId: taskId,
          status: 'completed', // pending, processing, completed, failed
          progress: 100,
          transcript: '',
          minutes: ''
        }
      })
    }, 500)
  })
}

// 批量查询任务状态
export function getBatchTaskStatus(taskIds) {
  return Promise.all(taskIds.map(id => getTaskStatus(id)))
}