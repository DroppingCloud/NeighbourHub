import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Notification } from '@/types'
import {
  getNoticeList,
  getUnreadCount,
  markAllRead,
  markNoticeRead,
  type NoticeVO
} from '@/api/notice'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])
  const unreadTotal = ref(0)
  const loading = ref(false)
  const loaded = ref(false)

  const unreadCount = computed(() => unreadTotal.value)

  function normalizeType(type?: string): Notification['type'] {
    const map: Record<string, Notification['type']> = {
      progress: 'progress',
      audit: 'audit',
      audit_result: 'audit',
      supplement: 'supplement',
      booking: 'service',
      service: 'service',
      system: 'progress'
    }
    return map[type || ''] || 'progress'
  }

  function toNotification(item: NoticeVO): Notification {
    return {
      id: String(item.noticeId),
      type: normalizeType(item.type),
      title: item.title,
      content: item.content,
      isRead: item.isRead === 1,
      sendTime: item.createTime,
      relatedId: item.refId ? String(item.refId) : undefined,
      relatedType: item.refType === 'booking' ? 'booking' : item.refType === 'application' ? 'application' : undefined
    }
  }

  async function loadNotifications(force = false) {
    if (!localStorage.getItem('token')) {
      notifications.value = []
      unreadTotal.value = 0
      loaded.value = false
      return
    }
    if (loading.value || (loaded.value && !force)) return

    loading.value = true
    try {
      const [page, count] = await Promise.all([
        getNoticeList(1, 20),
        getUnreadCount()
      ])
      notifications.value = (page.records || []).map(toNotification)
      unreadTotal.value = count || 0
      loaded.value = true
    } catch {
      notifications.value = []
      unreadTotal.value = 0
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(id: string) {
    const notification = notifications.value.find(n => n.id === id)
    if (!notification || notification.isRead) return

    await markNoticeRead(Number(id))
    notification.isRead = true
    unreadTotal.value = Math.max(0, unreadTotal.value - 1)
  }

  async function markAllAsRead() {
    if (unreadTotal.value === 0) return

    await markAllRead()
    notifications.value.forEach(n => {
      n.isRead = true
    })
    unreadTotal.value = 0
  }

  function addNotification(notification: Notification) {
    notifications.value.unshift(notification)
    if (!notification.isRead) {
      unreadTotal.value += 1
    }
  }

  function resetNotifications() {
    notifications.value = []
    unreadTotal.value = 0
    loaded.value = false
  }

  return {
    notifications,
    unreadCount,
    loading,
    loadNotifications,
    markAsRead,
    markAllAsRead,
    addNotification,
    resetNotifications
  }
})
