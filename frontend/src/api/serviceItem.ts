import request from '@/utils/request'

export interface ServiceItemVO {
  itemId: number
  itemName: string
  itemCode: string
  category: string
  description: string
  conditions: string
  status: string
  createTime: string
}

export interface ServiceItemRequest {
  itemName: string
  itemCode: string
  category: string
  description?: string
  conditions?: string
  status?: string
}

export interface MaterialTemplateVO {
  templateId: number
  itemId: number
  materialName: string
  materialType?: string
  description?: string
  sampleUrl?: string
  isRequired?: number
  sortOrder?: number
}

export type MaterialTemplateRequest = Omit<MaterialTemplateVO, 'templateId'>

export const getServiceItemList = (params?: {
  category?: string; status?: string; pageNum?: number; pageSize?: number
}) => request.get<any, any>('/api/admin/service-item/list', { params })

export const getPublicServiceItemList = (params?: {
  category?: string; status?: string; pageNum?: number; pageSize?: number
}) => request.get<any, any>('/api/service-item/list', { params })

export const getServiceItemDetail = (id: number) =>
  request.get<any, ServiceItemVO>(`/api/admin/service-item/${id}`)

export const createServiceItem = (data: ServiceItemRequest) =>
  request.post<any, number>('/api/admin/service-item', data)

export const updateServiceItem = (id: number, data: ServiceItemRequest) =>
  request.put<any, void>(`/api/admin/service-item/${id}`, data)

export const deleteServiceItem = (id: number) =>
  request.delete<any, void>(`/api/admin/service-item/${id}`)

export const getMaterialTemplates = (itemId: number) =>
  request.get<any, MaterialTemplateVO[]>(`/api/admin/service-item/${itemId}/materials`)

export const getPublicMaterialTemplates = (itemId: number) =>
  request.get<any, MaterialTemplateVO[]>(`/api/service-item/${itemId}/materials`)

export const createMaterialTemplate = (data: MaterialTemplateRequest) =>
  request.post<any, number>('/api/admin/material-template', data)

export const updateMaterialTemplate = (id: number, data: Partial<MaterialTemplateRequest>) =>
  request.put<any, void>(`/api/admin/material-template/${id}`, data)

export const deleteMaterialTemplate = (id: number) =>
  request.delete<any, void>(`/api/admin/material-template/${id}`)
