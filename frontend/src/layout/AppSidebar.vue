<template>
  <aside class="app-sidebar">
    <el-menu
      :default-active="activeMenu"
      class="sidebar-menu"
      @select="handleSelect"
    >
      <!-- 居民/家属菜单 -->
      <template v-if="role === 'resident' || role === 'family'">
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <span>首页仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/guide">
          <el-icon><MagicStick /></el-icon>
          <span>AI智能导办</span>
        </el-menu-item>
        <el-menu-item index="/application/submit">
          <el-icon><Document /></el-icon>
          <span>事项办理</span>
        </el-menu-item>
        <el-menu-item index="/application/list">
          <el-icon><List /></el-icon>
          <span>我的申请</span>
        </el-menu-item>
        <el-menu-item index="/booking">
          <el-icon><Calendar /></el-icon>
          <span>服务预约</span>
        </el-menu-item>
        <el-menu-item index="/booking/list">
          <el-icon><Tickets /></el-icon>
          <span>我的预约</span>
        </el-menu-item>
        <el-menu-item index="/family-binding">
          <el-icon><User /></el-icon>
          <span>家属代办</span>
        </el-menu-item>
        <el-menu-item index="/progress">
          <el-icon><TrendCharts /></el-icon>
          <span>进度查询</span>
        </el-menu-item>
      </template>

      <!-- 工作人员菜单 -->
      <template v-if="role === 'staff'">
        <el-menu-item index="/staff/workbench">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item v-if="isApplicationStaff" index="/workorder">
          <el-icon><Tickets /></el-icon>
          <span>工单处理</span>
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="menu-badge-right" />
        </el-menu-item>
        <el-menu-item v-if="isBookingStaff" index="/staff/booking">
          <el-icon><Calendar /></el-icon>
          <span>服务调度</span>
          <el-badge v-if="bookingPendingCount > 0" :value="bookingPendingCount" class="menu-badge-right" />
        </el-menu-item>
      </template>

      <!-- 管理员菜单 -->
      <template v-if="role === 'admin'">
        <el-menu-item index="/admin/dashboard">
          <el-icon><Setting /></el-icon>
          <span>后台管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/service-config">
          <el-icon><Document /></el-icon>
          <span>事项配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/workorder">
          <el-icon><Tickets /></el-icon>
          <span>工单管理</span>
        </el-menu-item>
        <!-- <el-menu-item index="/admin/service-resource">
          <el-icon><Calendar /></el-icon>
          <span>服务资源</span>
        </el-menu-item> -->
        <el-menu-item index="/admin/user-manage">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>
      </template>


      <!-- 通用菜单 - 消息通知 -->
      <el-menu-item index="/notice" class="notification-menu-item">
        <el-icon><Bell /></el-icon>
        <span>消息通知</span>
        <em class="notification-dot" v-if="unreadCount > 0"></em>
        <span class="notification-count" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  House, MagicStick, Document, List, Calendar, Tickets,
  User, TrendCharts, Setting, DataAnalysis, Bell
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { getWorkOrderList } from '@/api/workOrder'
import { getStaffBookingList } from '@/api/booking'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const pendingCount = ref(0)
const bookingPendingCount = ref(0)

const role = computed(() => normalizeRole(authStore.userInfo?.role || 'resident'))
const staffType = computed(() => authStore.userInfo?.staffType || '')
const isApplicationStaff = computed(() => role.value === 'staff' && staffType.value === 'application')
const isBookingStaff = computed(() => role.value === 'staff' && staffType.value === 'booking')
const activeMenu = computed(() => route.path)
const unreadCount = computed(() => notificationStore.unreadCount)

async function handleSelect(index: string) {
  if (!index) return

  try {
    if (index === route.path) {
      window.location.href = index
      return
    }
    await router.push(index)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    if (message.includes('Failed to fetch dynamically imported module') || message.includes('ERR_CACHE_READ_FAILURE')) {
      window.location.href = index
      return
    }
    throw error
  }
}

function normalizeRole(value: string) {
  return value.replace(/^ROLE_/, '').toLowerCase()
}

async function loadPendingWorkOrderCount() {
  if (!isApplicationStaff.value) {
    pendingCount.value = 0
    return
  }

  try {
    const page = await getWorkOrderList({ status: 'pending', pageNum: 1, pageSize: 1 })
    pendingCount.value = page?.total ?? page?.records?.length ?? 0
  } catch {
    pendingCount.value = 0
  }
}

async function loadPendingBookingCount() {
  if (!isBookingStaff.value) {
    bookingPendingCount.value = 0
    return
  }

  try {
    const page = await getStaffBookingList(1, 1, 'pending')
    bookingPendingCount.value = page?.total ?? page?.records?.length ?? 0
  } catch {
    bookingPendingCount.value = 0
  }
}

onMounted(() => {
  notificationStore.loadNotifications()
  loadPendingWorkOrderCount()
  loadPendingBookingCount()
})

</script>

<style scoped>
.app-sidebar {
  width: clamp(14rem, 22vw, 18rem);
  flex: 0 0 clamp(14rem, 22vw, 18rem);
  background: var(--sidebar-bg) !important;  /* 改为使用 CSS 变量，支持深色模式 */
  border-right: 0.0625rem solid var(--border-color);  /* 使用主题边框色 */
  height: calc(100vh - var(--app-header-height, 4rem));
  position: sticky;
  top: var(--app-header-height, 4rem);
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
  background: transparent !important;  /* 让菜单背景透明，继承侧边栏背景 */
}

.el-menu-item {
  margin: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  color: var(--text-secondary) !important;  /* 使用主题文字色 */
  white-space: normal;
  overflow-wrap: anywhere;
  min-height: 2.75rem;
  padding-right: 2.75rem;
}

.el-menu-item:hover {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
}

.el-menu-item.is-active {
  background: var(--bg-tertiary) !important;
  color: var(--gold) !important;  /* 激活状态使用金色 */
}

.el-menu-item .el-icon {
  color: inherit !important;
  flex-shrink: 0;
}

/* 消息通知菜单项的特殊样式 - 红点在文字右边 */
.notification-menu-item {
  position: relative;
  display: flex;
  align-items: center;
}

.notification-dot {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 0.5rem;
  height: 0.5rem;
  background-color: #f56c6c;
  border-radius: 50%;
  margin-left: 0.5rem;
}

.notification-count {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background-color: #f56c6c;
  color: #fff;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0 0.3125rem;
  border-radius: 0.625rem;
  min-width: 1.125rem;
  height: 1.125rem;
  line-height: 1.125rem;
  text-align: center;
  margin-left: 0.5rem;
}

.menu-badge-right {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
}

:deep(.el-badge__content) {
  background: #f56c6c;
  border: none;
}

@media (max-width: 48rem) {
  .app-sidebar {
    width: 100%;
    flex-basis: auto;
    height: auto;
    max-height: none;
    position: static;
    border-right: none;
    border-bottom: 0.0625rem solid var(--border-color);
  }

  .sidebar-menu {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(8.5rem, 1fr));
    height: auto;
    padding: 0.5rem;
  }

  .el-menu-item {
    margin: 0;
  }
}
</style>

