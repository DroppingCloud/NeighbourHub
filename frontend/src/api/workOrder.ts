import request from '@/utils/request'
import type { ApplicationMaterialVO, MaterialCompletenessVO } from './application'

export interface WorkOrderVO {
  orderId: number
  applicationId: number
  itemName: string
  category?: string
  residentName: string
  applicationStatus?: string
  applicationStatusLabel?: string
  formData?: string
  remark?: string
  submitTime?: string
  status: string
  statusLabel: string
  auditOpinion?: string
  staffName?: string
  materialCompleteness?: MaterialCompletenessVO
  materials?: ApplicationMaterialVO[]
  createTime: string
  updateTime: string
}

export interface WorkOrderQueryParams {
  status?: string
  staffUserId?: number
  pageNum?: number
  pageSize?: number
}

export interface AuditRequest {
  orderId: number
  action: 'approved' | 'rejected' | 'supplement_required' | 'completed' | 'processing'
  opinion?: string
}

export interface WorkOrderLogVO {
  logId: number
  orderId: number
  operatorId: number
  action: string
  fromStatus?: string
  toStatus?: string
  remark?: string
  createTime: string
}

export const getWorkOrderList = (params: WorkOrderQueryParams) =>
  request.get<any, any>('/api/workorder/list', { params })

export const getWorkOrderDetail = (id: number) =>
  request.get<any, WorkOrderVO>(`/api/workorder/${id}`)

export const auditWorkOrder = (data: AuditRequest) =>
  request.post<any, void>('/api/workorder/audit', data)

export const getWorkOrderLogs = (id: number) =>
  request.get<any, WorkOrderLogVO[]>(`/api/workorder/${id}/logs`)

export const deleteWorkOrder = (id: number) =>
  request.delete<any, void>(`/api/workorder/${id}`)
