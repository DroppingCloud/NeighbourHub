<template>
  <div class="bookings-container">
    <div class="page-header">
      <h2>我的预约</h2>
      <p>查看您的社区服务预约记录</p>
    </div>

    <div v-loading="loading" class="booking-list">
      <div v-for="booking in bookings" :key="booking.bookingId" class="booking-card">
        <div class="card-header">
          <div class="service-info">
            <span class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</span>
            <el-tag :type="getStatusType(booking.status)" size="small">
              {{ booking.statusLabel || booking.status }}
            </el-tag>
          </div>
          <span class="create-time">{{ booking.createTime }}</span>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="label">预约时间</span>
            <span>{{ booking.expectTime }}</span>
          </div>
          <div class="info-row">
            <span class="label">服务地址</span>
            <span>{{ booking.address || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">备注</span>
            <span>{{ booking.remark || '无' }}</span>
          </div>
        </div>

        <div class="card-footer">
          <el-button
            v-if="booking.status === 'pending' || booking.status === 'confirmed'"
            type="danger"
            size="small"
            @click="cancel(booking.bookingId)"
          >
            取消预约
          </el-button>
          <el-button
            v-if="booking.status === 'completed'"
            type="primary"
            size="small"
            @click="goToFeedback(booking.bookingId)"
          >
            去评价
          </el-button>
        </div>
      </div>
      <el-empty v-if="!loading && bookings.length === 0" description="暂无预约记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelBooking, getBookingList, type BookingVO } from '@/api/booking'

const router = useRouter()
const loading = ref(false)
const bookings = ref<BookingVO[]>([])

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    confirmed: 'warning',
    in_progress: 'primary',
    completed: 'success',
    cancelled: 'danger'
  }
  return map[status] || 'info'
}

async function loadBookings() {
  loading.value = true
  try {
    const page = await getBookingList(1, 50)
    bookings.value = page.records || []
  } finally {
    loading.value = false
  }
}

async function cancel(id: number) {
  await ElMessageBox.confirm('确定要取消这个预约吗？', '提示', { type: 'warning' })
  await cancelBooking(id)
  ElMessage.success('预约已取消')
  await loadBookings()
}

function goToFeedback(id: number) {
  router.push({ path: '/service-feedback', query: { bookingId: id, mode: 'write' } })
}

onMounted(loadBookings)
</script>

<style scoped>
.bookings-container {
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

.booking-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 12rem;
}

.booking-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  box-shadow: var(--shadow-sm);
}

.card-header,
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.card-header {
  padding-bottom: 0.75rem;
  border-bottom: 0.0625rem solid var(--border-color);
  margin-bottom: 0.75rem;
}

.service-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.service-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.create-time,
.label {
  color: var(--text-muted);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.info-row {
  display: flex;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.label {
  width: 5rem;
  flex-shrink: 0;
}
</style>
