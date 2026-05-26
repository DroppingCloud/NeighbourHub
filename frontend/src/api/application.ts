import request from '@/utils/request'

export interface ApplicationSubmitRequest {
  itemId: number
  proxyUserId?: number
  formData?: Record<string, any>
  remark?: string
}

export interface ApplicationVO {
  applicationId: number
  itemId: number
  itemName: string
  category: string
  status: string
  statusLabel: string
  submitTime: string
  updateTime: string
  remark?: string
  isProxy: boolean
}

export interface ApplicationQueryParams {
  status?: string
  itemId?: number
  pageNum?: number
  pageSize?: number
}

export const submitApplication = (data: ApplicationSubmitRequest) =>
  request.post<any, number>('/api/application/submit', data)

export const getApplicationList = (params: ApplicationQueryParams) =>
  request.get<any, any>('/api/application/list', { params })

export const getApplicationDetail = (id: number) =>
  request.get<any, ApplicationVO>(`/api/application/${id}`)

export const resubmitApplication = (id: number, data: ApplicationSubmitRequest) =>
  request.put<any, void>(`/api/application/${id}/resubmit`, data)
