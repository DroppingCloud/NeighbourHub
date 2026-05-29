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

export interface ApplicationMaterialRequest {
  templateId?: number | null
  materialName: string
  fileName: string
  filePath: string
  fileSize?: number
  fileType?: string
}

export interface ApplicationMaterialVO extends ApplicationMaterialRequest {
  materialId: number
  applicationId: number
  ocrText?: string
  precheckStatus: 'pending' | 'passed' | 'failed'
  precheckRemark?: string
  uploadTime: string
}

export interface MaterialPrecheckRequest {
  precheckStatus: 'pending' | 'passed' | 'failed'
  precheckRemark?: string
  ocrText?: string
}

export const submitApplication = (data: ApplicationSubmitRequest) =>
  request.post<any, number>('/api/application/submit', data)

export const getApplicationList = (params: ApplicationQueryParams) =>
  request.get<any, any>('/api/application/list', { params })

export const getApplicationDetail = (id: number) =>
  request.get<any, ApplicationVO>(`/api/application/${id}`)

export const resubmitApplication = (id: number, data: ApplicationSubmitRequest) =>
  request.put<any, void>(`/api/application/${id}/resubmit`, data)

export const uploadApplicationMaterial = (applicationId: number, data: ApplicationMaterialRequest) =>
  request.post<any, number>(`/api/application/${applicationId}/materials`, data)

export const getApplicationMaterials = (applicationId: number) =>
  request.get<any, ApplicationMaterialVO[]>(`/api/application/${applicationId}/materials`)

export const precheckApplicationMaterial = (materialId: number, data: MaterialPrecheckRequest) =>
  request.put<any, void>(`/api/application/material/${materialId}/precheck`, data)
