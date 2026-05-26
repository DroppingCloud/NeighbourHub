import request from '@/utils/request'

export interface UserInfo {
  userId: number
  username: string
  phone?: string
  email?: string
  roles: string[]
  realName?: string
  idCard?: string
  address?: string
  age?: number
}

export interface UpdateUserRequest {
  phone?: string
  email?: string
  address?: string
}

export const getUserInfo = (userId: number) =>
  request.get<any, UserInfo>(`/api/user/${userId}`)

export const updateUserInfo = (userId: number, data: UpdateUserRequest) =>
  request.put<any, void>(`/api/user/${userId}`, data)

export const bindProxy = (targetUserId: number) =>
  request.post<any, void>('/api/user/proxy/bind', { targetUserId })

export const getUsersByRole = (role: string, pageNum = 1, pageSize = 10) =>
  request.get<any, any>('/api/admin/user/list', { params: { role, pageNum, pageSize } })
