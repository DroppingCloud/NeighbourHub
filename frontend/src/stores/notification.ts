import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import type { Notification } from '@/types'
import {
  getNoticeList,
  getUnreadCount,
  markAllRead,
  markNoticeRead,
  type NoticeVO
} from '@/api/notice'
import { ElNotification } from 'element-plus'
import { useSettingsStore } from '@/stores/settings'

// helper: play short beep using WebAudio API
function playBeep() {
  try {
    const ctx = new (window.AudioContext || (window as any).webkitAudioContext)()
    const o = ctx.createOscillator()
    const g = ctx.createGain()
    o.type = 'sine'
    o.frequency.value = 880
    o.connect(g)
    g.connect(ctx.destination)
    o.start()
    g.gain.setValueAtTime(0.0001, ctx.currentTime)
    g.gain.exponentialRampToValueAtTime(0.1, ctx.currentTime + 0.01)
    g.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.18)
    o.stop(ctx.currentTime + 0.2)
  } catch (e) {
    // ignore
  }
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])
  const unreadTotal = ref(0)
  const loading = ref(false)
  const loaded = ref(false)
  const pollTimer = ref<number | null>(null)
  const POLL_INTERVAL = 8000 // 8s

  const unreadCount = computed(() => unreadTotal.value)

  function normalizeType(type?: string): Notification['type'] {
    const map: Record<string, Notification['type']> = {
      progress: 'progress',
      audit: 'audit',
      audit_result: 'audit',
      supplement: 'supplement',
      booking: 'service',
      service: 'service',
      evaluation: 'evaluation',  // 🔴 新增：评价通知类型
      system: 'progress'
    }
    return map[type || ''] || 'progress'
  }

  function normalizeRelatedType(refType?: string): Notification['relatedType'] {
    if (refType === 'booking' || refType === 'application' || refType === 'work_order' || refType === 'proxy') {
      return refType
    }
    return undefined
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
      relatedType: normalizeRelatedType(item.refType)
    }
  }

  const settings = useSettingsStore()

  function handleIncoming(notification: Notification) {
    // show popup if enabled
    if (settings.notification) {
      ElNotification({
        title: notification.title,
        message: notification.content,
        duration: 5000,
        position: 'top-right'
      })
    }
    // play sound if enabled
    if (settings.sound) playBeep()
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

  async function pollOnce() {
    if (!localStorage.getItem('token')) return
    
    try {
      const count = await getUnreadCount()
      
      // 如果后端未读数大于本地未读数，说明有新消息
      if (count > unreadTotal.value) {
        const page = await getNoticeList(1, 5)
        const newItems = (page.records || []).map(toNotification)
        // 插入未读的新消息
        for (const it of newItems.reverse()) {
          if (!notifications.value.find(n => n.id === it.id)) {
            notifications.value.unshift(it)
            if (!it.isRead) {
              unreadTotal.value += 1
              // 对新消息触发弹窗和声音提醒
              handleIncoming(it)
            }
          }
        }
      } 
      // 如果后端未读数小于本地未读数（可能其他端已读），同步后端数据
      else if (count < unreadTotal.value) {
        // 重新加载所有通知以同步状态
        await loadNotifications(true)
      }
      // 正常情况，直接同步未读数
      else {
        unreadTotal.value = count || 0
      }
    } catch (e) {
      // ignore polling errors silently
      console.debug('Polling error:', e)
    }
  }

  function startPolling() {
    stopPolling()
    // 初始加载
    loadNotifications(true)
    // 启动轮询
    pollTimer.value = window.setInterval(() => {
      pollOnce()
    }, POLL_INTERVAL)
  }

  function stopPolling() {
    if (pollTimer.value) {
      clearInterval(pollTimer.value)
      pollTimer.value = null
    }
  }

  async function markAsRead(id: string) {
    const notification = notifications.value.find(n => n.id === id)
    if (!notification || notification.isRead) return

    try {
      await markNoticeRead(Number(id))
      // 立即更新本地状态
      notification.isRead = true
      unreadTotal.value = Math.max(0, unreadTotal.value - 1)
    } catch (error) {
      console.error('标记已读失败:', error)
      throw error
    }
  }

  async function markAllAsRead() {
    if (unreadTotal.value === 0) return

    try {
      await markAllRead()
      // 立即更新所有本地消息状态
      notifications.value.forEach(n => {
        n.isRead = true
      })
      unreadTotal.value = 0
    } catch (error) {
      console.error('全部标记已读失败:', error)
      throw error
    }
  }

  function addNotification(notification: Notification) {
    notifications.value.unshift(notification)
    if (!notification.isRead) {
      unreadTotal.value += 1
    }
    // show popup/sound for programmatically added notifications
    handleIncoming(notification)
  }

  function resetNotifications() {
    notifications.value = []
    unreadTotal.value = 0
    loaded.value = false
    stopPolling()
  }

  // 监听登录状态，自动启动/停止轮询
  const auth = useAuthStore()
  watch(
    () => auth.token,
    (token) => {
      if (token) {
        startPolling()
      } else {
        stopPolling()
        resetNotifications()
      }
    },
    { immediate: true } // 立即执行一次
  )

  return {
    notifications,
    unreadCount,
    loading,
    loaded,
    loadNotifications,
    startPolling,
    stopPolling,
    markAsRead,
    markAllAsRead,
    addNotification,
    resetNotifications
  }
})