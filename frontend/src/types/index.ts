// 用户信息
export interface UserInfo {
  userId: string
  username: string
  phone: string
  role: 'resident' | 'family' | 'staff' | 'admin'
  realName?: string
  avatar?: string
}

// 政务事项
export interface GovService {
  id: string
  name: string
  category: string
  description: string
  conditions: string
  materials: string[]
  process: string[]
  estimatedDays: number
  isActive: boolean
}

// 申请单
export interface Application {
  id: string
  serviceId: string
  serviceName: string
  residentName: string
  submitterName: string
  submitTime: string
  status: 'pending' | 'reviewing' | 'supplement' | 'approved' | 'rejected' | 'completed'
  aiPrecheck: 'passed' | 'need_supplement' | 'failed'
  missingMaterials: string[]
  remark: string
  lastUpdate: string
}

// 工单
export interface WorkOrder {
  id: string
  applicationId: string
  serviceName: string
  applicantName: string
  staffName?: string
  status: 'pending' | 'processing' | 'completed' | 'rejected'
  assignTime?: string
  finishTime?: string
  result?: string
}

// 服务预约
export interface ServiceBooking {
  id: string
  serviceType: 'meal' | 'escort' | 'housekeeping' | 'medical'
  serviceTypeName: string
  preferredTime: string
  address: string
  status: 'pending' | 'dispatched' | 'processing' | 'completed' | 'cancelled'
  staffName?: string
  staffId?: string        // 新增：服务人员ID
  createTime: string
  completeTime?: string   // 新增：完成时间
  startTime?: string      // 新增：开始服务时间
  remark?: string         // 新增：特殊需求/备注
  feedback?: string
  rating?: number
}

// 通知消息
export interface Notification {
  id: string
  type: 'progress' | 'supplement' | 'audit' | 'service'
  title: string
  content: string
  isRead: boolean
  sendTime: string
  relatedId?: string
  relatedType?: 'application' | 'booking'
}

// 家属绑定
export interface FamilyBinding {
  id: string
  residentId: string
  residentName: string
  relation: string
  bindTime: string
  authorizedActions: string[]
}