<template>
  <div class="bookings-container">
    <div class="page-header">
      <h2>我的预约</h2>
      <p>查看您的社区服务预约记录</p>
    </div>

    <!-- 高亮提示条 -->
    <div v-if="highlightId" class="highlight-tip">
      <el-icon><Bell /></el-icon>
      <span>已为您定位到相关预约记录</span>
      <el-button link type="primary" @click="clearHighlight">取消高亮</el-button>
    </div>

    <div v-loading="loading" class="booking-list">
      <div 
        v-for="booking in bookings" 
        :key="booking.bookingId" 
        :data-id="booking.bookingId"
        class="booking-card"
        :class="{ 'highlight-flash': shouldHighlight(booking.bookingId) }"
      >
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
        <div v-if="booking.staffName" class="staff-info">
          <span class="label">服务人员：</span>
          <span>{{ booking.staffName }} ({{ booking.staffPhone }})</span>
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
import { onMounted, ref, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { cancelBooking, getBookingList, type BookingVO } from '@/api/booking'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const bookings = ref<BookingVO[]>([])
const highlightId = ref<string | null>(null)

// 获取需要高亮的ID
const getHighlightId = () => {
  const id = route.query.highlightId
  return id ? String(id) : null
}

// 判断是否需要高亮
const shouldHighlight = (id: number) => {
  return highlightId.value === String(id)
}

// 清除高亮
const clearHighlight = () => {
  highlightId.value = null
  // 移除URL中的highlightId参数
  router.replace({ path: '/booking/list', query: {} })
  // 移除所有高亮样式
  document.querySelectorAll('.booking-card.highlight-flash').forEach(el => {
    el.classList.remove('highlight-flash')
  })
}

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
  // 取消后清除高亮
  clearHighlight()
}

function goToFeedback(id: number) {
  router.push({ path: '/service-feedback', query: { bookingId: id, mode: 'write' } })
}

// 滚动到高亮项并添加高亮效果
async function scrollToHighlight() {
  const targetId = getHighlightId()
  if (!targetId) return
  
  highlightId.value = targetId
  
  await nextTick()
  
  // 查找目标元素
  const targetElement = document.querySelector(`.booking-card[data-id="${targetId}"]`) as HTMLElement
  
  if (targetElement) {
    // 滚动到目标元素
    targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    
    // 5秒后移除高亮（但保留URL参数，刷新页面后还会高亮）
    setTimeout(() => {
      const element = document.querySelector(`.booking-card[data-id="${targetId}"]`)
      if (element) {
        element.classList.remove('highlight-flash')
      }
    }, 5000)
  } else {
    // 如果没找到，可能是预约不存在，清除高亮
    console.warn('未找到要高亮的预约记录:', targetId)
    clearHighlight()
  }
}

onMounted(async () => {
  await loadBookings()
  // 加载完成后滚动到高亮项
  await scrollToHighlight()
})
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

/* 高亮提示条 */
.highlight-tip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  background: rgba(212, 168, 67, 0.12);
  border-radius: 0.5rem;
  border-left: 3px solid var(--gold);
  color: var(--text-primary);
  font-size: 0.875rem;
}

.highlight-tip .el-icon {
  color: var(--gold);
  font-size: 1rem;
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
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

/* 高亮闪烁动画 */
.booking-card.highlight-flash {
  animation: highlightBlink 0.5s ease-in-out 3, pulseGlow 1.5s ease-out;
  background: linear-gradient(90deg, 
    rgba(212, 168, 67, 0.08) 0%, 
    rgba(212, 168, 67, 0.2) 50%, 
    rgba(212, 168, 67, 0.08) 100%);
  border-left: 3px solid var(--gold);
}

@keyframes highlightBlink {
  0% {
    background: rgba(212, 168, 67, 0.08);
    transform: scale(1);
  }
  50% {
    background: rgba(212, 168, 67, 0.25);
    transform: scale(1.01);
  }
  100% {
    background: linear-gradient(90deg, 
      rgba(212, 168, 67, 0.08) 0%, 
      rgba(212, 168, 67, 0.2) 50%, 
      rgba(212, 168, 67, 0.08) 100%);
    transform: scale(1);
  }
}

@keyframes pulseGlow {
  0% {
    box-shadow: 0 0 0 0 rgba(212, 168, 67, 0.4);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(212, 168, 67, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(212, 168, 67, 0);
  }
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

.staff-info {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 0.0625rem solid var(--border-color);
  font-size: 0.875rem;
  color: var(--text-secondary);
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