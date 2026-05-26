import request from '@/utils/request'

export interface StatisticsOverview {
  totalApplications: number
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  supplementCount: number
  completedCount: number
  serviceBookingCount: number
  topItems: Array<{ itemName: string; count: number }>
  dailyTrend: Array<{ date: string; count: number }>
}

export const getOverview = () =>
  request.get<any, StatisticsOverview>('/api/statistics/overview')

export const getItemStats = () =>
  request.get<any, any[]>('/api/statistics/items')

export const getServiceStats = () =>
  request.get<any, any[]>('/api/statistics/services')
