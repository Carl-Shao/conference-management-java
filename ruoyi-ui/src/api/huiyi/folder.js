import request from '@/utils/request'
 
// 文件夹列表
export function listFolder(query) {
  return request({
    url: '/huiyi/folder/list',
    method: 'get',
    params: query
  })
}
 
// 文件夹详情
export function getFolder(folderId) {
  return request({
    url: '/huiyi/folder/' + folderId,
    method: 'get'
  })
}
 
// 新建文件夹
export function addFolder(data) {
  return request({
    url: '/huiyi/folder',
    method: 'post',
    data: data
  })
}
 
// 修改文件夹（改名 / 改备注）
export function updateFolder(data) {
  return request({
    url: '/huiyi/folder',
    method: 'put',
    data: data
  })
}
 
// 删除文件夹（支持批量；文件夹内的会议记录不会被删除，只会移出文件夹）
export function delFolder(folderIds) {
  return request({
    url: '/huiyi/folder/' + folderIds,
    method: 'delete'
  })
}