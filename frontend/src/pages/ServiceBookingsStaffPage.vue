<template>
  <div class="service-dispatch-container">
    <div class="page-header">
      <h2>服务调度</h2>
      <p>工作人员处理社区服务预约、派单与完成登记</p>
    </div>

    <div class="stats-row">
      <div class="stat-card-mini">
        <div class="stat-value">{{ pendingCount }}</div>
        <div class="stat-label">待调度</div>
      </div>
      <div class="stat-card-mini">
        <div class="stat-value">{{ confirmedCount }}</div>
        <div class="stat-label">已派单</div>
      </div>
      <div class="stat-card-mini">
        <div class="stat-value">{{ inProgressCount }}</div>
        <div class="stat-label">服务中</div>
      </div>
      <div class="stat-card-mini">
        <div class="stat-value">{{ completedCount }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="dispatch-tabs" @tab-change="loadBookings">
      <el-tab-pane label="待调度" name="pending">
        <div v-for="booking in pendingBookings" :key="booking.bookingId" class="dispatch-card">
          <BookingInfo :booking="booking" />
          <div class="card-actions">
            <el-select v-model="selectedStaff[booking.bookingId]" placeholder="服务人员" size="small">
              <el-option label="staff01" :value="2" />
            </el-select>
            <el-button type="primary" size="small" @click="dispatch(booking.bookingId)">
              <el-icon><Position /></el-icon>
              派单
            </el-button>
          </div>
        </div>
        <el-empty v-if="pendingBookings.length === 0" description="暂无待调度预约" />
      </el-tab-pane>

      <el-tab-pane label="已派单" name="confirmed">
        <div v-for="booking in confirmedBookings" :key="booking.bookingId" class="dispatch-card">
          <BookingInfo :booking="booking" />
          <div class="card-actions">
            <el-button type="warning" size="small" @click="start(booking.bookingId)">
              <el-icon><VideoPlay /></el-icon>
              开始服务
            </el-button>
          </div>
        </div>
        <el-empty v-if="confirmedBookings.length === 0" description="暂无已派单预约" />
      </el-tab-pane>

      <el-tab-pane label="服务中" name="in_progress">
        <div v-for="booking in inProgressBookings" :key="booking.bookingId" class="dispatch-card">
          <BookingInfo :booking="booking" />
          <div class="card-actions">
            <el-button type="success" size="small" @click="completeService(booking.bookingId)">
              <el-icon><CircleCheck /></el-icon>
              完成服务
            </el-button>
          </div>
        </div>
        <el-empty v-if="inProgressBookings.length === 0" description="暂无服务中预约" />
      </el-tab-pane>

      <el-tab-pane label="已完成" name="completed">
        <div v-for="booking in completedBookings" :key="booking.bookingId" class="dispatch-card">
          <BookingInfo :booking="booking" />
          <div class="card-actions">
            <el-button type="info" size="small" @click="showFeedback(booking)">
              <el-icon><ChatLineSquare /></el-icon>
              查看评价
            </el-button>
          </div>
        </div>
        <el-empty v-if="completedBookings.length === 0" description="暂无已完成预约" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Position, VideoPlay, ChatLineSquare } from '@element-plus/icons-vue'
import {
  assignBooking,
  completeBooking,
  startBooking,
  getStaffBookingList,
  type BookingVO
} from '@/api/booking'

const activeTab = ref('pending')
const bookings = ref<BookingVO[]>([])
const selectedStaff = ref<Record<number, number>>({})

// 统计数据
const pendingCount = ref(0)
const confirmedCount = ref(0)
const inProgressCount = ref(0)
const completedCount = ref(0)

const pendingBookings = computed(() => bookings.value.filter(item => item.status === 'pending'))
const confirmedBookings = computed(() => bookings.value.filter(item => item.status === 'confirmed'))
const inProgressBookings = computed(() => bookings.value.filter(item => item.status === 'in_progress'))
const completedBookings = computed(() => bookings.value.filter(item => item.status === 'completed'))

const BookingInfo = defineComponent({
  props: {
    booking: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    return () => {
      const booking = props.booking as BookingVO
      return h('div', { class: 'booking-info' }, [
        h('div', { class: 'card-header' }, [
          h('div', { class: 'service-info' }, [
            h('span', { class: 'service-name' }, booking.serviceTypeLabel || booking.serviceType),
            h('span', { class: `status-pill status-${booking.status}` }, booking.statusLabel || booking.status)
          ]),
          h('span', { class: 'create-time' }, booking.createTime)
        ]),
        h('div', { class: 'card-body' }, [
          row('预约时间', booking.expectTime),
          row('服务地址', booking.address || '-'),
          row('备注', booking.remark || '无')
        ])
      ])
    }
  }
})

function row(label: string, value: string) {
  return h('div', { class: 'info-row' }, [
    h('span', { class: 'label' }, `${label}：`),
    h('span', value)
  ])
}

async function loadBookings() {
  const status = activeTab.value === 'pending' ? 'pending' :
                 activeTab.value === 'confirmed' ? 'confirmed' :
                 activeTab.value === 'in_progress' ? 'in_progress' :
                 activeTab.value === 'completed' ? 'completed' : undefined
  const page = await getStaffBookingList(1, 100, status)
  bookings.value = page.records || []
}

async function loadStatistics() {
  const [pending, confirmed, inProgress, completed] = await Promise.all([
    getStaffBookingList(1, 1, 'pending'),
    getStaffBookingList(1, 1, 'confirmed'),
    getStaffBookingList(1, 1, 'in_progress'),
    getStaffBookingList(1, 1, 'completed')
  ])
  pendingCount.value = pending.total
  confirmedCount.value = confirmed.total
  inProgressCount.value = inProgress.total
  completedCount.value = completed.total
}

async function dispatch(bookingId: number) {
  const staffId = selectedStaff.value[bookingId]
  if (!staffId) {
    ElMessage.warning('请选择服务人员')
    return
  }
  try {
    await assignBooking(bookingId, staffId)
    ElMessage.success('派单成功')
    activeTab.value = 'confirmed'
    await loadBookings()
    await loadStatistics()
  } catch (error: any) {
    ElMessage.error(error.message || '派单失败')
  }
}

async function start(bookingId: number) {
  try {
    await ElMessageBox.confirm('确定开始服务吗？', '确认开始', { type: 'info' })
    await startBooking(bookingId)
    ElMessage.success('服务已开始')
    activeTab.value = 'in_progress'
    await loadBookings()
    await loadStatistics()
  } catch (error: any) {
    ElMessage.error(error.message || '开始服务失败')
  }
}

async function completeService(bookingId: number) {
  await ElMessageBox.confirm('确定该预约服务已完成吗？', '确认完成', { type: 'success' })
  await completeBooking(bookingId, '工作人员确认服务完成')
  ElMessage.success('服务已完成')
  await loadBookings()
  await loadStatistics()
}

function showFeedback(booking: BookingVO) {
  const feedback = booking.feedback || '暂无评价内容'
  ElMessageBox.alert(feedback, '居民评价', {
    confirmButtonText: '关闭',
    type: 'info'
  })
}

onMounted(() => {
  loadBookings()
  loadStatistics()
})
</script>

<style scoped>
/* 保持原有样式不变 */
.service-dispatch-container {
  max-width: 75rem;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(11rem, 100%), 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card-mini {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1rem;
  text-align: center;
  box-shadow: var(--shadow-sm);
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.dispatch-tabs {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.dispatch-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
  border: 0.0625rem solid var(--border-color);
}

:deep(.card-header) {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 0.0625rem solid var(--border-color);
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

:deep(.service-info) {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

:deep(.service-name) {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.create-time),
:deep(.label) {
  color: var(--text-muted);
}

:deep(.card-body) {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

:deep(.info-row) {
  display: flex;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

:deep(.label) {
  width: 5.5rem;
  flex-shrink: 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.15rem 0.5rem;
  font-size: 0.75rem;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.status-pending {
  color: #b7791f;
}

.status-confirmed {
  color: #2563eb;
}

.status-completed {
  color: #15803d;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.75rem;
  border-top: 0.0625rem solid var(--border-color);
}
</style>