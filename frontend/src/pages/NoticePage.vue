<template>
  <div class="notifications-container">
    <div class="page-header">
      <h2>消息通知</h2>
      <el-button v-if="unreadCount > 0" link type="primary" @click="markAllAsRead">
        全部已读
      </el-button>
    </div>

    <div v-if="notifications.length > 0" class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleClick(item)"
      >
        <div class="notification-icon" :class="item.type">
          <el-icon>
            <Bell v-if="item.type === 'progress'" />
            <Warning v-if="item.type === 'supplement'" />
            <CircleCheck v-if="item.type === 'audit'" />
            <Service v-if="item.type === 'service'" />
          </el-icon>
        </div>
        <div class="notification-content">
          <div class="notification-title">{{ item.title }}</div>
          <div class="notification-desc">{{ item.content }}</div>
          <div class="notification-time">{{ item.sendTime }}</div>
        </div>
        <div v-if="!item.isRead" class="unread-dot"></div>
      </div>
    </div>

    <el-empty v-else description="暂无通知" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Warning, CircleCheck, Service } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notificationStore = useNotificationStore()
const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

async function handleClick(item: any) {
  await notificationStore.markAsRead(item.id)
  
  // 根据消息类型跳转到对应页面，并携带高亮ID
  if (item.relatedType === 'application') {
    router.push({
      path: '/application/list',
      query: { highlightId: item.relatedId }
    })
  } else if (item.relatedType === 'booking') {
    router.push({
      path: '/booking/list',
      query: { highlightId: item.relatedId }
    })
  } else {
    router.push('/notice')
  }
}

async function markAllAsRead() {
  await notificationStore.markAllAsRead()
}

onMounted(() => {
  notificationStore.loadNotifications(true)
})
</script>

<style scoped>
.notifications-container {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.75rem;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-item {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  border: 0.0625rem solid var(--border-color);
}

.notification-item:hover {
  background: var(--bg-tertiary);
  transform: translateX(0.125rem);
}

.notification-item.unread {
  background: rgba(212, 168, 67, 0.08);
  border-left: 0.1875rem solid var(--gold);
}

.notification-icon {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon.progress {
  background: rgba(39, 174, 96, 0.12);
  color: var(--jade);
}

.notification-icon.supplement {
  background: rgba(231, 76, 60, 0.12);
  color: var(--vermilion);
}

.notification-icon.audit {
  background: rgba(212, 168, 67, 0.12);
  color: var(--gold);
}

.notification-icon.service {
  background: rgba(212, 168, 67, 0.12);
  color: var(--gold);
}

.notification-icon .el-icon {
  font-size: 1.375rem;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.notification-desc {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.notification-time {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.unread-dot {
  width: 0.5rem;
  height: 0.5rem;
  background: var(--vermilion);
  border-radius: 50%;
  position: absolute;
  top: 1.125rem;
  right: 1rem;
}
</style>