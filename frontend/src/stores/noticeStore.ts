import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount, markNoticeRead, markAllRead as markAllReadApi } from '@/api/notice'

export const useNoticeStore = defineStore('notice', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    unreadCount.value = await getUnreadCount()
  }

  async function markRead(noticeId: number) {
    await markNoticeRead(noticeId)
    if (unreadCount.value > 0) unreadCount.value--
  }

  async function markAllReadStore() {
    await markAllReadApi()
    unreadCount.value = 0
  }

  return { unreadCount, fetchUnreadCount, markRead, markAllReadStore }
})
