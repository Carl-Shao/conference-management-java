import request from '@/utils/request'
import { get } from 'sortablejs'

/**
 * ========================
 * 会议记录 API
 * ========================
 */

// ---------------- 基础 CRUD ----------------

/**
 * 查询会议记录列表（全部 / 收藏 / 文件夹内，三合一）
 * @param {Object} query 查询参数（MeetingRecord 字段 + pageNum + pageSize）
 */
export function listMeeting(query) {
  return request({
    url: '/huiyi/record/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取会议详情（转写 + 纪要 + 笔记一次性返回）
 * @param {Number|String} meetingId 会议ID
 */
export function getMeeting(meetingId) {
  return request({
    url: '/huiyi/record/' + meetingId,
    method: 'get'
  })
}

/**
 * 新增会议记录
 * @param {Object} data MeetingRecord 对象
 */
export function addMeeting(data) {
  return request({
    url: '/huiyi/record',
    method: 'post',
    data: data
  })
}

/**
 * 修改会议记录
 * @param {Object} data MeetingRecord 对象
 */
export function updateMeeting(data) {
  return request({
    url: '/huiyi/record',
    method: 'put',
    data: data
  })
}

/**
 * 删除会议记录（支持批量）
 * @param {Array|String} meetingIds 会议ID数组，如 [1,2,3] 或 "1,2,3"
 */
export function delMeeting(meetingIds) {
  return request({
    url: '/huiyi/record/' + meetingIds,
    method: 'delete'
  })
}

// ---------------- 扩展操作 ----------------

/**
 * 重命名会议
 * @param {Number|String} meetingId 会议ID
 * @param {String} title 新标题
 */
export function renameMeeting(meetingId, title) {
  return request({
    url: '/huiyi/record/' + meetingId + '/rename',
    method: 'put',
    params: { title }
  })
}

/**
 * 收藏 / 取消收藏
 * @param {Number|String} meetingId 会议ID
 * @param {Boolean} favorite true=收藏, false=取消收藏
 */
export function favoriteMeeting(meetingId, favorite) {
  return request({
    url: '/huiyi/record/' + meetingId + '/favorite',
    method: 'put',
    params: { favorite }
  })
}

/**
 * 批量移动会议到文件夹
 * @param {Object} data MeetingMoveFolderDTO { meetingIds: [], folderId: '' }
 *   - folderId 传空表示移出文件夹
 */
export function addMeetingToFolder(data) {
  return request({
    url: '/huiyi/record/folder/add',
    method: 'put',
    data: data
  })
}

/**
 * 批量给会议去掉某个标签，不影响其他标签归属
 * @param {Object} data MeetingMoveFolderDTO { meetingIds: [], folderId: '' }
 *   - folderId 传空表示移出文件夹
 */
export function removeMeetingFromFolder(data) {
  return request({
    url: '/huiyi/record/folder/remove',
    method: 'put',
    data: data
  })
}

/**
 * 合并多条会议记录
 * @param {Object} data MeetingMoveFolderDTO { meetingIds: [], folderId: '' }
 *   - folderId 传空表示移出文件夹
 */
export function setMeetingFolders(data) {
  return request({
    url: '/huiyi/record/folders',
    method: 'put',
    data: data
  })
}

/**
 * 合并多条会议记录
 * @param {Object} data MeetingMergeDTO { meetingIds: [], title: '' }
 * @returns {Promise} 返回新合并后的 meetingId
 */
export function mergeMeeting(data) {
  return request({
    url: '/huiyi/record/merge',
    method: 'post',
    data: data
  })
}

/**
 * 保存 / 更新用户笔记
 * @param {Number|String} meetingId 会议ID
 * @param {String} content 笔记内容（纯文本/富文本HTML）
 */
export function saveMeetingNote(meetingId, content) {
  return request({
    url: '/huiyi/record/' + meetingId + '/note',
    method: 'put',
    // 后端 @RequestBody String 接收的是纯文本，需指定 Content-Type
    headers: { 'Content-Type': 'text/plain' },
    data: content
  })
}

/**
 * 纪要保存
 * @param {*} meetingId 
 * @param {*} content 
 * @returns 
 */
export function saveMeetingMinutes(meetingId, content) {
  return request({
    url: '/huiyi/record/' + meetingId + '/minutes',
    method: 'put',
    headers: { 'Content-Type': 'application/json' },
    data: JSON.stringify(content)
  })
}

/**
 * 获取会议音频流 (返回 Blob)
 */
export function getMeetingAudioBlob(meetingId) {
  return request({
    url: 'huiyi/record/' + meetingId + '/audio',
    method: 'get',
    responseType: 'blob',
    skipInterceptor: true
  })
}