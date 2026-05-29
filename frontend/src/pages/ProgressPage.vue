<template>
  <div class="page-container">
    <div class="page-header">
      <h2>进度查询</h2>
      <p>实时查看您的申请和预约进度</p>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="申请进度" name="application">
        <div v-loading="loadingApplications">
          <div v-for="app in applications" :key="app.applicationId" class="progress-card">
            <div class="card-header">
              <span class="title">{{ app.itemName }}</span>
              <el-tag :type="getStatusType(app.status)">{{ app.statusLabel || getStatusText(app.status) }}</el-tag>
            </div>

            <div class="custom-steps">
              <div
                v-for="(step, index) in stepTitles"
                :key="step"
                class="step-item"
                :class="{
                  active: index + 1 <= getStepActive(app.status),
                  completed: index + 1 < getStepActive(app.status)
                }"
              >
                <div class="step-icon">
                  <el-icon v-if="index + 1 < getStepActive(app.status)"><Check /></el-icon>
                  <span v-else class="step-number">{{ index + 1 }}</span>
                </div>
                <div class="step-title">{{ step }}</div>
                <div v-if="index < stepTitles.length - 1" class="step-line" :class="{ active: index + 1 < getStepActive(app.status) }"></div>
              </div>
            </div>

            <div class="remark">提交时间：{{ app.submitTime }}<span v-if="app.remark">；备注：{{ app.remark }}</span></div>
          </div>
          <el-empty v-if="applications.length === 0" description="暂无申请记录" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="预约进度" name="booking">
        <div v-loading="loadingBookings">
          <div v-for="booking in bookings" :key="booking.bookingId" class="progress-card">
            <div class="card-header">
              <span class="title">{{ booking.serviceTypeLabel }}</span>
              <el-tag :type="getBookingStatusType(booking.status)">{{ booking.statusLabel || getBookingStatusText(booking.status) }}</el-tag>
            </div>

            <div class="custom-steps">
              <div
                v-for="(step, index) in bookingStepTitles"
                :key="step"
                class="step-item"
                :class="{
                  active: index + 1 <= getBookingStepActive(booking.status),
                  completed: index + 1 < getBookingStepActive(booking.status)
                }"
              >
                <div class="step-icon">
                  <el-icon v-if="index + 1 < getBookingStepActive(booking.status)"><Check /></el-icon>
                  <span v-else class="step-number">{{ index + 1 }}</span>
                </div>
                <div class="step-title">{{ step }}</div>
                <div v-if="index < bookingStepTitles.length - 1" class="step-line" :class="{ active: index + 1 < getBookingStepActive(booking.status) }"></div>
              </div>
            </div>

            <div class="time">预约时间：{{ booking.expectTime }}</div>
          </div>
          <el-empty v-if="bookings.length === 0" description="暂无预约记录" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { getApplicationList, type ApplicationVO } from '@/api/application'
import { getBookingList, type BookingVO } from '@/api/booking'

const activeTab = ref('application')
const applications = ref<ApplicationVO[]>([])
const bookings = ref<BookingVO[]>([])
const loadingApplications = ref(false)
const loadingBookings = ref(false)

const stepTitles = ['提交申请', '审核中', '办理中', '已完成']
const bookingStepTitles = ['提交预约', '调度中', '服务中', '已完成']

onMounted(() => {
  loadProgress()
})

async function loadProgress() {
  loadingApplications.value = true
  loadingBookings.value = true
  try {
    const [appPage, bookingPage] = await Promise.all([
      getApplicationList({ pageNum: 1, pageSize: 50 }),
      getBookingList(1, 50)
    ])
    applications.value = getPageRows<ApplicationVO>(appPage)
    bookings.value = getPageRows<BookingVO>(bookingPage)
  } finally {
    loadingApplications.value = false
    loadingBookings.value = false
  }
}

function getPageRows<T>(page: any): T[] {
  if (Array.isArray(page)) return page
  return page?.records || page?.list || page?.rows || []
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning',
    reviewing: 'primary',
    supplement: 'danger',
    approved: 'success',
    completed: 'success',
    rejected: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    reviewing: '审核中',
    supplement: '待补件',
    approved: '已通过',
    completed: '已完成',
    rejected: '已驳回'
  }
  return map[status] || status
}

function getStepActive(status: string): number {
  const map: Record<string, number> = {
    pending: 1,
    reviewing: 2,
    supplement: 1,
    approved: 3,
    completed: 4,
    rejected: 4
  }
  return map[status] || 0
}

function getBookingStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    confirmed: 'warning',
    processing: 'primary',
    completed: 'success',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

function getBookingStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待调度',
    confirmed: '已确认',
    processing: '服务中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

function getBookingStepActive(status: string): number {
  const map: Record<string, number> = {
    pending: 1,
    confirmed: 2,
    processing: 3,
    completed: 4,
    cancelled: 4
  }
  return map[status] || 0
}
</script>

<style scoped>
.page-container {
  max-width: 56.25rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  color: var(--text-muted);
}

.progress-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  margin-bottom: 1rem;
  box-shadow: var(--shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-word;
}

.custom-steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 1rem 0;
  padding: 0.5rem 0;
}

.step-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-width: 0;
}

.step-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  background: var(--card-bg);
  border: 0.125rem solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-muted);
  transition: all 0.3s ease;
  z-index: 2;
}

.step-item.active .step-icon {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.step-item.completed .step-icon {
  background: var(--jade);
  border-color: var(--jade);
  color: #fff;
}

.step-title {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.5rem;
  text-align: center;
  word-break: break-word;
}

.step-item.active .step-title {
  color: var(--text-primary);
  font-weight: 500;
}

.step-line {
  position: absolute;
  top: 1.25rem;
  left: calc(50% + 1.25rem);
  right: calc(-50% + 1.25rem);
  height: 0.125rem;
  background: var(--border-color);
  z-index: 1;
}

.step-line.active {
  background: var(--jade);
}

.remark,
.time {
  margin-top: 1rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
  padding-top: 0.75rem;
  border-top: 0.0625rem solid var(--border-color);
  line-height: 1.6;
  word-break: break-word;
}

@media (max-width: 48rem) {
  .custom-steps {
    overflow-x: auto;
    gap: 1rem;
    justify-content: flex-start;
  }

  .step-item {
    min-width: 5rem;
  }
}
</style>
