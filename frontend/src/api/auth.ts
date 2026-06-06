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
  staffType?: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone?: string
  realName: string
  idCard: string
  role?: 'resident' | 'family'
  account?: string
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
  avatar?: string
  staffType?: string
}

export const login = (data: LoginRequest): Promise<LoginResponse> =>
  request.post<any, LoginResponse>('/api/auth/login', data)

export interface LoginSmsRequest {
  phone: string
  code?: string
}

export const loginBySms = (data: LoginSmsRequest): Promise<LoginResponse> =>
  request.post<any, LoginResponse>('/api/auth/login/sms', data)

export const register = (data: RegisterRequest): Promise<void> =>
  request.post<any, void>('/api/auth/register', data)

export const logout = (): Promise<void> =>
  request.post<any, void>('/api/auth/logout')

export const getMe = (): Promise<UserInfoVO> =>
  request.get<any, UserInfoVO>('/api/auth/me')

export const updateMe = (data: Partial<UserInfoVO>): Promise<void> =>
  request.put<any, void>('/api/auth/me', data)

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export const changePassword = (data: ChangePasswordRequest): Promise<void> =>
  request.post<any, void>('/api/auth/change-password', data)

// ===================== 新增 重置密码 =====================
export interface ResetPasswordRequest {
  phone: string
  code: string
  newPassword: string
  confirmPassword: string
}

export const resetPassword = (data: ResetPasswordRequest): Promise<void> =>
  request.post<any, void>('/api/auth/reset-password', data)
// ======================================================

export const deleteAccount = (): Promise<void> =>
  request.delete<any, void>('/api/auth/account')
