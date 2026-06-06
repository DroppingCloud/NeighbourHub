<template>
  <div class="bookings-container">
    <div class="page-header">
      <h2>我的预约</h2>
      <p>查看您的社区服务预约记录</p>
    </div>

    <el-table :data="bookings" v-loading="loading" style="width: 100%">
      <el-table-column prop="serviceTypeLabel" label="服务类型" min-width="120" />
      <el-table-column prop="expectTime" label="预约时间" min-width="180" />
      <el-table-column prop="address" label="服务地址" min-width="180" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.statusLabel || getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" link type="danger" @click="handleCancel(row.bookingId)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && bookings.length === 0" description="暂无预约记录" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelBooking, getBookingList, type BookingVO } from '@/api/booking'

const bookings = ref<BookingVO[]>([])
const loading = ref(false)

onMounted(loadBookings)

async function loadBookings() {
  loading.value = true
  try {
    const page = await getBookingList(1, 50)
    bookings.value = Array.isArray(page) ? page : page?.records || page?.list || page?.rows || []
  } finally {
    loading.value = false
  }
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    confirmed: 'warning',
    in_progress: 'primary',
    processing: 'primary',
    completed: 'success',
    cancelled: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待调度',
    confirmed: '已确认',
    in_progress: '服务中',
    processing: '服务中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

function handleCancel(id: number) {
  ElMessageBox.confirm('确定要取消这个预约吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await cancelBooking(id)
    ElMessage.success('预约已取消')
    loadBookings()
  }).catch(() => {})
}
</script>

<style scoped>
.bookings-container {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.75rem;
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
</style>
