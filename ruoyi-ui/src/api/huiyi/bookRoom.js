import request from '@/utils/request'

// 查询会议室预约列表
export function listBookRoom(query) {
  return request({
    url: '/huiyi/bookRoom/list',
    method: 'get',
    params: query
  })
}

// 查询我的预约列表
export function listMyBookRoom(query) {
  return request({
    url: '/huiyi/bookRoom/myList',
    method: 'get',
    params: query
  })
}

// 查询会议室预约详细
export function getBookRoom(id) {
  return request({
    url: '/huiyi/bookRoom/' + id,
    method: 'get'
  })
}

// 新增会议室预约
export function addBookRoom(data) {
  return request({
    url: '/huiyi/bookRoom',
    method: 'post',
    data: data
  })
}

// 修改会议室预约
export function updateBookRoom(data) {
  return request({
    url: '/huiyi/bookRoom',
    method: 'put',
    data: data
  })
}

// 取消会议室预约
export function cancelBookRoom(bookingId, cancelReason) {
  return request({
    url: '/huiyi/bookRoom/cancel/' + bookingId,
    method: 'put',
    params: {
      cancelReason: cancelReason
    }
  })
}

// 删除会议室预约
export function delBookRoom(id) {
  return request({
    url: '/huiyi/bookRoom/' + id,
    method: 'delete'
  })
}

// 获取会议室空闲时间
export function getFreeTime(roomId, bookDate) {
  return request({
    url: '/huiyi/bookRoom/freeTime',
    method: 'get',
    params: {
      roomId: roomId,
      bookDate: bookDate
    }
  })
}

// 获取单个会议室当前状态
export function getCurrentStatus(roomId) {
  return request({
    url: '/huiyi/bookRoom/currentStatus/' + roomId,
    method: 'get'
  })
}

// 批量获取多个会议室当前状态
export function getCurrentStatusList(roomIds) {
  return request({
    url: '/huiyi/bookRoom/currentStatus/list',
    method: 'get',
    params: {
      roomIds: roomIds
    }
  })
}