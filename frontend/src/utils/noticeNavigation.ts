import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Notification } from '@/types'

function normalizeRole(role?: string) {
  return (role || '').replace(/^ROLE_/, '').toLowerCase()
}

function withQuery(highlightId?: string, tab?: string) {
  const query: Record<string, string> = {}
  if (highlightId) query.highlightId = highlightId
  if (tab) query.tab = tab
  return Object.keys(query).length ? query : undefined
}

export function resolveNoticeRoute(item: Notification) {
  const authStore = useAuthStore()
  const role = normalizeRole(authStore.userInfo?.role)
  const staffType = authStore.userInfo?.staffType || ''
  const highlightId = item.relatedId

  if (role === 'admin') {
    return null
  }

  if (item.relatedType === 'application') {
    if (role === 'staff') {
      return { path: staffType === 'booking' ? '/staff/booking' : '/workorder', query: withQuery(highlightId) }
    }
    return { path: '/application/list', query: withQuery(highlightId) }
  }

  if (item.relatedType === 'booking') {
    if (role === 'staff') return { path: '/staff/booking', query: withQuery(highlightId) }
    return { path: '/booking/list', query: withQuery(highlightId) }
  }

  if (item.relatedType === 'work_order') {
    if (role === 'staff') {
      return { path: staffType === 'booking' ? '/staff/booking' : '/workorder', query: withQuery(highlightId) }
    }
    return { path: '/progress' }
  }

  if (item.relatedType === 'proxy') {
    return { path: '/family-binding', query: withQuery(highlightId) }
  }

  if (role === 'staff') return { path: '/staff/workbench' }
  return { path: '/notice' }
}

export async function openNoticeTarget(router: Router, item: Notification) {
  const route = resolveNoticeRoute(item)
  if (route) {
    await router.push(route)
  }
}
