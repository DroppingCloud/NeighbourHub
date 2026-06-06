import request from '@/utils/request'
import { getMe, type UserInfoVO } from '@/api/auth'

export type UserInfo = UserInfoVO

export interface ProxyBindRequest {
  targetUserId?: number
  targetProfileId?: number
  relation?: string
  authorizedActions?: string
}

export interface ProxyRelationVO extends ProxyBindRequest {
  id: number
  proxyUserId: number
  targetUserId?: number
  targetProfileName?: string
  proxyUserName?: string
  status: string
  createTime: string
  updateTime: string
}

export interface ProxyApplyRequest {
  realName: string
  idCard: string
  relation: string
  authorizedActions?: string
}

export const getCurrentUserInfo = () => getMe()

export const bindProxy = (data: ProxyBindRequest) =>
  request.post<any, number>('/api/proxy/bind', data)

export const getProxyRelations = () =>
  request.get<any, ProxyRelationVO[]>('/api/proxy/list', { headers: { 'X-Skip-Proxy': 'true' } })

export const revokeProxyRelation = (id: number) =>
  request.put<any, void>(`/api/proxy/${id}/revoke`)

export const getUsersByRole = (role?: string, pageNum = 1, pageSize = 10) =>
  request.get<any, any>('/api/admin/user/list', {
    params: { role: role || undefined, pageNum, pageSize }
  })

export interface StaffCreateRequest {
  username: string
  realName: string
  phone?: string
  email?: string
  communityId?: number
  staffType: 'application' | 'booking'
  bookingServiceType?: 'dining' | 'accompany' | 'home_visit'
}

export const createStaff = (data: StaffCreateRequest) =>
  request.post<any, number>('/api/admin/staff', data)

export const applyProxy = (data: ProxyApplyRequest) =>
  request.post<any, number>('/api/proxy/apply', data)

export const getPendingRequests = () =>
  request.get<any, ProxyRelationVO[]>('/api/proxy/pending-requests', { headers: { 'X-Skip-Proxy': 'true' } })

export const confirmProxy = (id: number) =>
  request.put<any, void>(`/api/proxy/${id}/confirm`)

export const rejectProxy = (id: number) =>
  request.put<any, void>(`/api/proxy/${id}/reject`)
