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

    <!-- <div class="stats-row">
      <div v-if="isApplicationStaff" class="stat-card" @click="goTo('/workorder', 'pending')">
        <div class="stat-icon warning"><el-icon><Tickets /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingWorkOrders }}</div>
          <div class="stat-label">待处理工单</div>
          <div class="stat-trend">需及时处理</div>
        </div>
      </div>
      <div v-if="isApplicationStaff" class="stat-card" @click="goTo('/workorder', 'processing')">
        <div class="stat-icon primary"><el-icon><Loading /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ processingWorkOrders }}</div>
          <div class="stat-label">处理中工单</div>
          <div class="stat-trend">进行中</div>
        </div>
      </div>
      <div v-if="isBookingStaff" class="stat-card" @click="goTo('/staff/booking', 'pending')">
        <div class="stat-icon info"><el-icon><Calendar /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingDispatches }}</div>
          <div class="stat-label">待调度预约</div>
          <div class="stat-trend">需派单</div>
        </div>
      </div>
      <div v-if="isBookingStaff" class="stat-card" @click="goTo('/staff/booking', 'in_progress')">
        <div class="stat-icon success"><el-icon><Check /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ processingServices }}</div>
          <div class="stat-label">服务中</div>
          <div class="stat-trend">进行中</div>
        </div>
      </div>
    </div> -->

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tickets, Loading, Calendar, Check, Position, Bell } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getWorkOrderList, auditWorkOrder, type WorkOrderVO } from '@/api/workOrder'
import { assignBooking, type BookingVO, getStaffBookingList } from '@/api/booking'
import { getUnreadCount } from '@/api/notice'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const staffType = computed(() => authStore.userInfo?.staffType || 'application')
const isApplicationStaff = computed(() => staffType.value === 'application')
const isBookingStaff = computed(() => staffType.value === 'booking')
const workOrders = ref<WorkOrderVO[]>([])
const bookings = ref<BookingVO[]>([])
const loading = ref(false)
const unreadCount = ref(0)

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
      const [bookingPage, count] = await Promise.all([
        getStaffBookingList(1, 100),
        unreadPromise
      ])
      bookings.value = getRows<BookingVO>(bookingPage)
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

/* .stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(12rem, 100%), 1fr));
  gap: 1.25rem;
  margin-bottom: 1.75rem;
} */

.stat-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-0.125rem);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex: 0 0 auto;
}

.stat-icon.warning,
.action-icon.warning {
  background: rgba(230, 126, 34, 0.1);
  color: #e67e22;
}

.stat-icon.primary {
  background: rgba(45, 53, 97, 0.1);
  color: #2d3561;
}

.stat-icon.info,
.action-icon.info {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.stat-icon.success {
  background: rgba(39, 174, 96, 0.1);
  color: #27ae60;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-card .stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-card .stat-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.stat-trend {
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
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
}
</style>
