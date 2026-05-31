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

export const chatGuideStream = (data: ChatMessage) => {
  const base = import.meta.env.VITE_API_BASE_URL ?? ''
  const url = base + '/api/guide/chat/stream'
  const token = localStorage.getItem('token')
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  
  return fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  })
}