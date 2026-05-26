import request from '@/utils/request'

export interface GuideRequest {
  residentType?: string
  age?: number
  needType?: string
  description: string
}

export interface GuideItem {
  itemId: number
  itemName: string
  category: string
  description: string
}

export interface GuideResult {
  items: GuideItem[]
  materials: string[]
  steps: string[]
  tips: string
  isFallback: boolean
}

export interface ChatMessage {
  message: string
  sessionId?: string
}

export const recommendGuide = (data: GuideRequest) =>
  request.post<any, GuideResult>('/api/guide/recommend', data)

export const chatGuide = (data: ChatMessage) =>
  request.post<any, string>('/api/guide/chat', data)
