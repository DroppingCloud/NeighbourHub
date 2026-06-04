import request from '@/utils/request'

export interface NoticeVO {
  noticeId: number
  title: string
  content: string
  type: string
  refType?: string
  refId?: number
  isRead: number
  createTime: string
}

export const getNoticeList = (pageNum = 1, pageSize = 10) =>
  request.get<any, any>('/api/notice/list', { params: { pageNum, pageSize }, headers: { 'X-Skip-Proxy': 'true' } })

export const markNoticeRead = (id: number) =>
  request.put<any, void>(`/api/notice/${id}/read`, undefined, { headers: { 'X-Skip-Proxy': 'true' } })

export const markAllRead = () =>
  request.put<any, void>('/api/notice/read-all', undefined, { headers: { 'X-Skip-Proxy': 'true' } })

export const getUnreadCount = () =>
  request.get<any, number>('/api/notice/unread-count', { headers: { 'X-Skip-Proxy': 'true' } })
