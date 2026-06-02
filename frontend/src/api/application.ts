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
  orderId?: number
  workOrderStatus?: string
  workOrderStatusLabel?: string
  auditOpinion?: string
  staffName?: string
  formData?: string
  materials?: ApplicationMaterialVO[]
  requiredMaterials?: MaterialTemplateVO[]
  materialCompleteness?: MaterialCompletenessVO
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
  fileUrl?: string
  ocrText?: string
  precheckStatus: 'pending' | 'passed' | 'failed'
  precheckRemark?: string
  uploadTime: string
}

export interface MaterialTemplateVO {
  templateId: number
  itemId: number
  materialName: string
  materialType?: string
  description?: string
  sampleUrl?: string
  isRequired: number
  sortOrder: number
}

export interface MaterialCompletenessVO {
  applicationId: number
  itemId: number
  requiredCount: number
  uploadedRequiredCount: number
  complete: boolean
  missingMaterialNames: string[]
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

export const withdrawApplication = (id: number) =>
  request.put<any, void>(`/api/application/${id}/withdraw`)

export const cleanupFailedDraftApplication = (id: number) =>
  request.delete<any, void>(`/api/application/${id}/failed-draft`)

export const uploadApplicationMaterial = (applicationId: number, data: ApplicationMaterialRequest) =>
  request.post<any, number>(`/api/application/${applicationId}/materials`, data)

export const uploadApplicationMaterialFile = (
  applicationId: number,
  data: {
    templateId?: number | null
    materialName?: string
    file: File
  }
) => {
  const formData = new FormData()
  if (data.templateId !== undefined && data.templateId !== null) {
    formData.append('templateId', String(data.templateId))
  }
  if (data.materialName) {
    formData.append('materialName', data.materialName)
  }
  formData.append('file', data.file)
  return request.post<any, number>(`/api/application/${applicationId}/materials/file`, formData)
}

export const getApplicationMaterials = (applicationId: number) =>
  request.get<any, ApplicationMaterialVO[]>(`/api/application/${applicationId}/materials`)

export const checkApplicationMaterialCompleteness = (applicationId: number) =>
  request.get<any, MaterialCompletenessVO>(`/api/application/${applicationId}/materials/completeness`)

export const precheckApplicationMaterial = (materialId: number, data: MaterialPrecheckRequest) =>
  request.put<any, void>(`/api/application/material/${materialId}/precheck`, data)

export const runAiPrecheckApplicationMaterial = (materialId: number) =>
  request.post<any, ApplicationMaterialVO>(`/api/application/material/${materialId}/ai-precheck`)

export const getApplicationMaterialFileUrl = (materialId: number) =>
  `/api/application/material/${materialId}/file`
