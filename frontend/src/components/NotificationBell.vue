<template>
  <el-popover
    ref="popoverRef"
    placement="bottom-end"
    :width="'min(92vw, 28rem)'"
    trigger="click"
    popper-class="notification-popover-popper"
  >
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :offset="[8, 8]">
        <el-button class="notification-btn" circle>
          <el-icon :size="24"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>
    
    <div class="notification-popover">
      <div class="popover-header">
        <span class="header-title">
          <el-icon :size="18"><Bell /></el-icon>
          消息通知
        </span>
        <el-button 
          v-if="unreadCount > 0" 
          link 
          type="primary" 
          size="default"
          @click="handleMarkAllRead"
        >
          全部已读
        </el-button>
      </div>
      
      <div class="popover-list">
        <template v-if="recentNotifications.length > 0">
          <div
            v-for="item in recentNotifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.isRead }"
            @click="handleNotificationClick(item)"
          >
            <div class="notification-icon" :class="item.type">
              <el-icon :size="20">
                <Bell v-if="item.type === 'progress'" />
                <Warning v-if="item.type === 'supplement'" />
                <CircleCheck v-if="item.type === 'audit'" />
                <Service v-if="item.type === 'service'" />
              </el-icon>
            </div>
            <div class="notification-content">
              <div class="notification-title">{{ item.title }}</div>
              <div class="notification-desc">{{ item.content }}</div>
              <div class="notification-time">{{ formatTime(item.sendTime) }}</div>
            </div>
            <div v-if="!item.isRead" class="unread-dot"></div>
          </div>
        </template>
        <div v-else class="empty-notification">
          <el-empty description="暂无通知" :image-size="80" />
        </div>
      </div>
      
      <div class="popover-footer">
        <router-link to="/notice" class="footer-link" @click="closePopover">
          查看全部消息
          <el-icon><Right /></el-icon>
        </router-link>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Warning, CircleCheck, Service, Right } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const notificationStore = useNotificationStore()
const popoverRef = ref()

const unreadCount = computed(() => notificationStore.unreadCount)
const notifications = computed(() => notificationStore.notifications)
const recentNotifications = computed(() => notifications.value)

function formatTime(time: string) {
  if (!time) return ''
  return time.replace(/(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})/, '$1 $2')
}

async function handleNotificationClick(item: any) {
  await notificationStore.markAsRead(item.id)
  closePopover()
  router.push('/notice')
}

async function handleMarkAllRead() {
  await notificationStore.markAllAsRead()
  ElMessage.success('已标记全部为已读')
}

function closePopover() {
  if (popoverRef.value) {
    popoverRef.value.hide()
  }
}

onMounted(() => {
  notificationStore.startPolling()
})

onUnmounted(() => {
  notificationStore.stopPolling && notificationStore.stopPolling()
})

// restart polling when login state changes
const auth = useAuthStore()
watch(() => auth.token, (val) => {
  if (val) notificationStore.startPolling()
  else notificationStore.stopPolling && notificationStore.stopPolling()
})
</script>

<style scoped>
.notification-btn {
  border: none;
  background: transparent;
  width: 3rem;
  height: 3rem;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.notification-btn:hover {
  background: var(--bg-tertiary);
  transform: scale(1.05);
}

:deep(.el-badge__content) {
  background-color: var(--vermilion);
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  height: 1.125rem;
  line-height: 1.125rem;
  padding: 0 0.375rem;
  border-radius: 1.125rem;
}
</style>

<style>
/* 弹窗全局样式 - 使用 CSS 变量 */
.notification-popover-popper {
  padding: 0 !important;
  border-radius: 1rem !important;
  box-shadow: var(--shadow-md) !important;
  background: var(--card-bg) !important;
  border: 0.0625rem solid var(--border-color) !important;
}

.notification-popover-popper .el-popover__title {
  display: none;
}
</style>

<style scoped>
.notification-popover {
  width: 100%;
  max-height: 28.75rem;
  display: flex;
  flex-direction: column;
}

.popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-bottom: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
  border-radius: 1rem 1rem 0 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.header-title .el-icon {
  color: var(--gold);
}

.popover-list {
  flex: 1;
  max-height: 23.75rem;
  overflow-y: auto;
  background: var(--card-bg);
}

.notification-item {
  display: flex;
  gap: 0.875rem;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 0.0625rem solid var(--border-color);
  position: relative;
  min-width: 0;
}

.notification-item:hover {
  background: var(--bg-tertiary);
}

.notification-item.unread {
  background: rgba(212, 168, 67, 0.08);
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
  margin-bottom: 0.375rem;
  line-height: 1.45;
  overflow: visible;
  text-overflow: clip;
  display: block;
  white-space: normal;
  overflow-wrap: anywhere;
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
  top: 1.25rem;
  right: 1.25rem;
  box-shadow: 0 0 0 0.125rem var(--card-bg);
}

.popover-footer {
  padding: 0.875rem 1.25rem;
  text-align: center;
  border-top: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
  border-radius: 0 0 1rem 1rem;
}

.footer-link {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--gold);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: color 0.2s;
}

.footer-link:hover {
  color: var(--vermilion);
}

.footer-link .el-icon {
  font-size: 0.75rem;
}

.empty-notification {
  padding: 2.5rem 1.25rem;
  text-align: center;
}

.popover-list::-webkit-scrollbar {
  width: 0.25rem;
}

.popover-list::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 0.25rem;
}

.popover-list::-webkit-scrollbar-thumb {
  background: var(--gold);
  border-radius: 0.25rem;
}
</style>
