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
  status: string
  createTime: string
  updateTime: string
}

export const getCurrentUserInfo = () => getMe()

export const bindProxy = (data: ProxyBindRequest) =>
  request.post<any, number>('/api/proxy/bind', data)

export const getProxyRelations = () =>
  request.get<any, ProxyRelationVO[]>('/api/proxy/list')

export const revokeProxyRelation = (id: number) =>
  request.put<any, void>(`/api/proxy/${id}/revoke`)

export const getUsersByRole = (role?: string, pageNum = 1, pageSize = 10) =>
  request.get<any, any>('/api/admin/user/list', {
    params: { role: role || undefined, pageNum, pageSize }
  })
