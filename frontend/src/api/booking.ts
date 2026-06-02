import request from '@/utils/request'

export interface BookingRequest {
  serviceType: string
  expectTime: string
  address?: string
  remark?: string
}

export interface BookingVO {
  bookingId: number
  serviceType: string
  serviceTypeLabel: string
  expectTime: string
  status: string
  statusLabel: string
  address?: string
  remark?: string
  createTime: string
}

export const createBooking = (data: BookingRequest) =>
  request.post<any, number>('/api/booking/create', data)

export const getBookingList = (pageNum = 1, pageSize = 10) =>
  request.get<any, any>('/api/booking/list', { params: { pageNum, pageSize } })

export const getBookingDetail = (id: number) =>
  request.get<any, BookingVO>(`/api/booking/${id}`)

export const cancelBooking = (id: number) =>
  request.put<any, void>(`/api/booking/${id}/cancel`)

export const assignBooking = (id: number, staffUserId: number) =>
  request.put<any, void>(`/api/booking/${id}/assign`, { staffUserId })

export function startBooking(bookingId: number) {
    return request.put(`/api/booking/${bookingId}/start`); }

export const completeBooking = (id: number, feedback?: string) =>
  request.put<any, void>(`/api/booking/${id}/complete`, { feedback })

export const submitBookingFeedback = (id: number, feedback: string) =>
  request.post<any, void>(`/api/booking/${id}/feedback`, { feedback })

export function getStaffBookingList(pageNum: number, pageSize: number, status?: string) {
  const params: any = { pageNum, pageSize }
  if (status) params.status = status
  return request.get('/api/booking/staff/list', { params })
}

