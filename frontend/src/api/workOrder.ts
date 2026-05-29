import request from '@/utils/request'

export interface WorkOrderVO {
  orderId: number
  applicationId: number
  itemName: string
  residentName: string
  status: string
  statusLabel: string
  auditOpinion?: string
  staffName?: string
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
