<template>
  <div class="staff-workbench">
    <div class="welcome-banner">
      <div class="banner-content">
        <h2>欢迎回来，{{ userInfo?.realName || userInfo?.username }}</h2>
        <p>{{ workbenchSubtitle }}</p>
      </div>
      <div class="banner-stats">
        <div
          v-for="item in workbenchStats"
          :key="item.label"
          class="stat-item"
          @click="goTo(item.path, item.tab)"
        >
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 服务得分模块（仅服务调度人员可见，且放在快捷操作上方） -->
    <div v-if="isBookingStaff" class="score-section">
      <div class="score-card">
        <div class="score-icon">
          <el-icon><StarFilled /></el-icon>
        </div>
        <div class="score-main">
          <div class="score-title">服务综合评分</div>
          <div class="score-average">
            <span class="score-number">{{ scoreLabel === '暂无评分' ? '—' : scoreLabel }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="score-stars">
            <el-rate :model-value="bookingRatingAverage" disabled show-score :text-color="'#ff9900'" />
          </div>
        </div>
        <div class="score-count">
          <el-icon><User /></el-icon>
          <span>{{ bookingRatingCount }} 次评价</span>
        </div>
      </div>
    </div>

    <div class="quick-actions-section">
      <h3 class="section-title">快捷操作</h3>
      <div class="action-grid">
        <div v-if="isApplicationStaff" class="action-card" @click="goTo('/workorder', 'pending')">
          <div class="action-icon warning"><el-icon><Tickets /></el-icon></div>
          <span>工单审核</span>
          <span class="action-badge" v-if="pendingWorkOrders > 0">{{ pendingWorkOrders }}</span>
        </div>
        <div v-if="isBookingStaff" class="action-card" @click="goTo('/staff/booking', 'pending')">
          <div class="action-icon info"><el-icon><Position /></el-icon></div>
          <span>待调度</span>
          <span class="action-badge" v-if="pendingDispatches > 0">{{ pendingDispatches }}</span>
        </div>
        <div class="action-card" @click="goTo('/notice')">
          <div class="action-icon"><el-icon><Bell /></el-icon></div>
          <span>查看通知</span>
        </div>
      </div>
    </div>

    <div v-if="isApplicationStaff" class="recent-section">
      <div class="section-header">
        <h3 class="section-title">待处理工单</h3>
        <router-link to="/workorder" class="view-all">查看全部 &gt;</router-link>
      </div>
      <el-table :data="recentWorkOrders" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="itemName" label="事项名称" min-width="150" />
        <el-table-column prop="residentName" label="申请人" width="120" />
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="quickApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="quickReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && recentWorkOrders.length === 0" description="暂无待处理工单" />
    </div>

    <div v-if="isBookingStaff" class="recent-section">
      <div class="section-header">
        <h3 class="section-title">待调度预约</h3>
        <router-link to="/staff/booking" class="view-all">查看全部 &gt;</router-link>
      </div>
      <el-table :data="recentBookings" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="serviceTypeLabel" label="服务类型" width="120" />
        <el-table-column prop="address" label="服务地址" min-width="200" />
        <el-table-column prop="expectTime" label="预约时间" width="180" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="quickDispatch(row)">接取</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && recentBookings.length === 0" description="暂无待调度预约" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElRate } from 'element-plus'
import { Tickets, Loading, Calendar, Check, Position, Bell, StarFilled, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getWorkOrderList, auditWorkOrder, type WorkOrderVO } from '@/api/workOrder'
import { assignBooking, type BookingVO, getStaffBookingList } from '@/api/booking'
import { getUnreadCount } from '@/api/notice'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const staffType = computed(() => authStore.userInfo?.staffType || 'application')
const currentStaffUserId = computed(() => Number(authStore.userInfo?.userId || 0))
const isApplicationStaff = computed(() => staffType.value === 'application')
const isBookingStaff = computed(() => staffType.value === 'booking')
const workOrders = ref<WorkOrderVO[]>([])
const bookings = ref<BookingVO[]>([])
const loading = ref(false)
const unreadCount = ref(0)
const bookingRatingAverage = ref(0)
const bookingRatingCount = ref(0)
const scoreLabel = computed(() => bookingRatingCount.value > 0 ? bookingRatingAverage.value.toFixed(1) : '暂无评分')

const pendingWorkOrders = computed(() => workOrders.value.filter(o => o.status === 'pending').length)
const processingWorkOrders = computed(() => workOrders.value.filter(o => o.status === 'processing').length)
const completedWorkOrders = computed(() => workOrders.value.filter(o => o.status === 'completed').length)
const pendingDispatches = computed(() => bookings.value.filter(b => b.status === 'pending').length)
const processingServices = computed(() => bookings.value.filter(b => b.status === 'in_progress').length)
const recentWorkOrders = computed(() => workOrders.value.filter(o => o.status === 'pending').slice(0, 5))
const recentBookings = computed(() => bookings.value.filter(b => b.status === 'pending').slice(0, 5))
const workbenchSubtitle = computed(() => {
  if (isBookingStaff.value) return '社区服务预约工作台，请及时处理服务调度与进行中事项'
  return '事项办理工单工作台，请及时处理居民提交的申请'
})
const workbenchStats = computed(() => {
  if (isBookingStaff.value) {
    return [
      { label: '待调度', value: pendingDispatches.value, path: '/staff/booking', tab: 'pending' },
      { label: '进行中', value: processingServices.value, path: '/staff/booking', tab: 'in_progress' },
      { label: '未读消息', value: unreadCount.value, path: '/notice' }
    ]
  }
  return [
    { label: '待处理', value: pendingWorkOrders.value, path: '/workorder', tab: 'pending' },
    { label: '已完成', value: completedWorkOrders.value, path: '/workorder', tab: 'completed' },
    { label: '未读消息', value: unreadCount.value, path: '/notice' }
  ]
})

onMounted(() => {
  loadWorkbench()
})

async function loadWorkbench() {
  loading.value = true
  try {
    const unreadPromise = getUnreadCount().catch(() => 0)
    if (isApplicationStaff.value) {
      const [workOrderPage, completedPage, count] = await Promise.all([
        getWorkOrderList({ pageNum: 1, pageSize: 50 }),
        getWorkOrderList({ status: 'completed', pageNum: 1, pageSize: 50 }),
        unreadPromise
      ])
      workOrders.value = [
        ...getRows<WorkOrderVO>(workOrderPage),
        ...getRows<WorkOrderVO>(completedPage)
      ]
      bookings.value = []
      unreadCount.value = Number(count || 0)
    } else if (isBookingStaff.value) {
      const [bookingPage, completedPage, count] = await Promise.all([
        getStaffBookingList(1, 100, undefined, currentStaffUserId.value),
        currentStaffUserId.value > 0
          ? getStaffBookingList(1, 1000, 'completed', currentStaffUserId.value)
          : Promise.resolve({ records: [] }),
        unreadPromise
      ])
      bookings.value = getRows<BookingVO>(bookingPage)
      updateBookingRatingStats(getRows<BookingVO>(completedPage))
      workOrders.value = []
      unreadCount.value = Number(count || 0)
    }
  } finally {
    loading.value = false
  }
}

function getRows<T>(page: any): T[] {
  if (Array.isArray(page)) return page
  return page?.records || page?.list || page?.rows || []
}

function parseRatingFromFeedback(feedback?: string): number | null {
  if (!feedback) return null
  const trimmed = feedback.trim()
  const numericMatch = trimmed.match(/^(\d+)\s*星/)
  if (numericMatch) return Number(numericMatch[1])
  const starMatch = trimmed.match(/^(★+)/)
  if (starMatch) return starMatch[1].length
  return null
}

function updateBookingRatingStats(completedBookings: BookingVO[]) {
  const ratings = completedBookings
    .map(booking => parseRatingFromFeedback(booking.feedback))
    .filter((rating): rating is number => rating !== null)
  bookingRatingCount.value = ratings.length
  bookingRatingAverage.value = ratings.length
    ? ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length
    : 0
}

function goTo(path: string, tab?: string) {
  router.push(tab ? { path, query: { tab } } : path)
}

function quickApprove(order: WorkOrderVO) {
  ElMessageBox.confirm(`确定通过「${order.itemName}」的申请吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await auditWorkOrder({ orderId: order.orderId, action: 'approved', opinion: '审核通过' })
    ElMessage.success('已通过')
    loadWorkbench()
  }).catch(() => {})
}

function quickReject(order: WorkOrderVO) {
  ElMessageBox.confirm(`确定驳回「${order.itemName}」的申请吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await auditWorkOrder({ orderId: order.orderId, action: 'rejected', opinion: '不符合办理条件' })
    ElMessage.success('已驳回')
    loadWorkbench()
  }).catch(() => {})
}

async function quickDispatch(booking: BookingVO) {
  try {
    await assignBooking(booking.bookingId)
    ElMessage.success('已接取')
    loadWorkbench()
  } catch (err: any) {
    const msg = err?.message || String(err)
    if (msg.includes('社区') || msg.includes('无社区')) {
      ElMessageBox.alert('无法派单：该工单/预约未关联社区信息，系统无法完成自动分配。请联系管理员或在申请详情补全社区后再试。', '派单失败', { type: 'warning' })
    } else {
      ElMessage.error(msg || '派单失败')
    }
  }
}
</script>

<style scoped>
.staff-workbench {
  max-width: 75rem;
  margin: 0 auto;
}

.welcome-banner {
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%);
  border-radius: var(--radius-lg);
  padding: 2rem 2.5rem;
  margin-bottom: 1.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.banner-content {
  min-width: 0;
}

.banner-content h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin-bottom: 0.375rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.banner-content p {
  color: rgba(255,255,255,0.7);
  line-height: 1.5;
}

.banner-stats {
  display: flex;
  gap: 2.5rem;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
  min-width: 4.5rem;
  cursor: pointer;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--gold-light);
}

.stat-label {
  font-size: 0.8125rem;
  color: rgba(255,255,255,0.68);
  margin-top: 0.25rem;
}

/* 服务得分卡片样式（优化后） */
.score-section {
  margin-bottom: 1.75rem;
}

.score-card {
  background: linear-gradient(135deg, #fff9e8 0%, #fff4dd 100%);
  border-radius: 1.5rem;
  padding: 1.5rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
  box-shadow: 0 8px 20px rgba(212, 168, 67, 0.12);
  border: 1px solid rgba(212, 168, 67, 0.3);
  transition: transform 0.2s;
}

.score-card:hover {
  transform: translateY(-2px);
}

.score-icon {
  width: 3.5rem;
  height: 3.5rem;
  background: rgba(212, 168, 67, 0.15);
  border-radius: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: var(--gold);
}

.score-main {
  flex: 1;
  min-width: 160px;
}

.score-title {
  font-size: 0.875rem;
  color: var(--text-muted);
  letter-spacing: 1px;
  margin-bottom: 0.25rem;
}

.score-average {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
}

.score-number {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--gold-dark, #b88a2c);
  line-height: 1;
}

.score-unit {
  font-size: 1rem;
  color: var(--text-muted);
}

.score-stars {
  margin-top: 0.25rem;
}

.score-count {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-secondary);
  background: rgba(0,0,0,0.03);
  padding: 0.5rem 1rem;
  border-radius: 2rem;
  font-size: 0.875rem;
}

.score-count .el-icon {
  font-size: 1rem;
  color: var(--gold);
}

.quick-actions-section {
  margin-bottom: 1.75rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 1rem;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(12rem, 100%), 1fr));
  gap: 1.25rem;
}

.action-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: var(--shadow-sm);
  position: relative;
}

.action-card:hover {
  transform: translateY(-0.125rem);
  box-shadow: var(--shadow-md);
}

.action-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 0.5rem;
  font-size: 1.25rem;
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.action-icon.warning {
  background: rgba(230, 126, 34, 0.1);
  color: #e67e22;
}

.action-icon.info {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.action-badge {
  position: absolute;
  top: 0.5rem;
  right: 0.75rem;
  background: #e74c3c;
  color: #fff;
  font-size: 0.6875rem;
  padding: 0.125rem 0.375rem;
  border-radius: 1rem;
  min-width: 1.25rem;
}

.recent-section {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.75rem 2rem;
  margin-bottom: 1.75rem;
  box-shadow: var(--shadow-sm);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.view-all {
  color: var(--gold);
  text-decoration: none;
  font-size: 0.8125rem;
}

:deep(.el-table) {
  background: transparent;
}

:deep(.el-table th) {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

:deep(.el-table td) {
  color: var(--text-secondary);
  border-bottom-color: var(--border-color);
}

:deep(.el-table .cell) {
  white-space: normal;
  line-height: 1.6;
  word-break: break-word;
}

@media (max-width: 48rem) {
  .action-grid {
    grid-template-columns: 1fr;
  }

  .banner-stats {
    width: 100%;
    justify-content: space-between;
    gap: 1rem;
  }

  .welcome-banner {
    padding: 1.5rem;
  }

  .score-card {
    padding: 1rem 1.25rem;
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }

  .score-icon {
    margin: 0 auto;
  }

  .score-count {
    justify-content: center;
  }
}
</style>