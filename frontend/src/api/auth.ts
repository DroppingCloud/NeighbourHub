import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  roles: string[]
}

export interface RegisterRequest {
  username: string
  password: string
  phone?: string
  realName: string
  idCard: string
}

export interface UserInfoVO {
  userId: number
  username: string
  phone?: string
  email?: string
  roles: string[]
  realName?: string
  idCard?: string
  address?: string
  age?: number
  gender?: string
  birthday?: string
}

export const login = (data: LoginRequest): Promise<LoginResponse> =>
  request.post<any, LoginResponse>('/api/auth/login', data)

export const register = (data: RegisterRequest): Promise<void> =>
  request.post<any, void>('/api/auth/register', data)

export const logout = (): Promise<void> =>
  request.post<any, void>('/api/auth/logout')

export const getMe = (): Promise<UserInfoVO> =>
  request.get<any, UserInfoVO>('/api/auth/me')

export const updateMe = (data: Partial<UserInfoVO>): Promise<void> =>
  request.put<any, void>('/api/auth/me', data)
