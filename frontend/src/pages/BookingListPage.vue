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
        :class="[
          `status-${booking.status}`,
          { 'is-proxy': booking.isProxy },
          { 'expanded': expandedIds.has(booking.bookingId) },
          { 'highlight-flash': shouldHighlight(booking.bookingId) }
        ]"
      >
        <!-- 折叠头部 -->
        <div class="card-header" @click="toggleExpand(booking.bookingId)">
          <div class="service-info">
            <span class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</span>
            <el-tag :type="getStatusType(booking.status)" size="small">
              {{ booking.statusLabel || booking.status }}
            </el-tag>
            <el-tag v-if="booking.isProxy" type="info" size="small" style="margin-left:8px">
              代办: {{ booking.proxyUserName || '他人代办' }}
            </el-tag>
          </div>
          <div class="header-right">
            <span class="create-time">{{ booking.createTime }}</span>
            <el-icon class="expand-icon">
              <ArrowDown v-if="!expandedIds.has(booking.bookingId)" />
              <ArrowUp v-else />
            </el-icon>
          </div>
        </div>

        <!-- 折叠内容 -->
        <div v-show="expandedIds.has(booking.bookingId)" class="card-body">
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
          <div v-if="booking.staffName" class="staff-info">
            <span class="label">服务人员：</span>
            <span>{{ booking.staffName }} ({{ booking.staffPhone }})</span>
          </div>
          <div class="card-footer">
            <el-button
              v-if="booking.status === 'pending' || booking.status === 'confirmed'"
              type="danger"
              size="small"
              @click.stop="cancel(booking.bookingId)"
            >
              取消预约
            </el-button>
            <el-button
              v-if="booking.status === 'completed'"
              type="primary"
              size="small"
              @click.stop="goToFeedback(booking.bookingId, booking.feedback)"
            >
              {{ booking.feedback && booking.feedback.trim() ? '查看评价' : '去评价' }}
            </el-button>
          </div>
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
import { Bell, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { cancelBooking, getBookingList, type BookingVO } from '@/api/booking'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const bookings = ref<BookingVO[]>([])
const highlightId = ref<string | null>(null)
const expandedIds = ref<Set<number>>(new Set())

const getHighlightId = () => {
  const id = route.query.highlightId
  return id ? String(id) : null
}

const shouldHighlight = (id: number) => {
  return highlightId.value === String(id)
}

const clearHighlight = () => {
  highlightId.value = null
  router.replace({ path: '/booking/list', query: {} })
  document.querySelectorAll('.booking-card.highlight-flash').forEach(el => {
    el.classList.remove('highlight-flash')
  })
}

const toggleExpand = (id: number) => {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
  expandedIds.value = new Set(expandedIds.value)
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
  clearHighlight()
}

function goToFeedback(id: number, feedback?: string) {
  const hasFeedback = feedback?.trim() && feedback.trim() !== '工作人员确认服务完成'
  router.push({ path: '/service-feedback', query: { bookingId: id, mode: hasFeedback ? 'view' : 'write' } })
}

async function scrollToHighlight() {
  const targetId = getHighlightId()
  if (!targetId) return
  
  highlightId.value = targetId
  const idNum = Number(targetId)
  expandedIds.value.add(idNum)
  expandedIds.value = new Set(expandedIds.value)
  
  await nextTick()
  
  const targetElement = document.querySelector(`.booking-card[data-id="${targetId}"]`) as HTMLElement
  if (targetElement) {
    targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setTimeout(() => {
      const element = document.querySelector(`.booking-card[data-id="${targetId}"]`)
      if (element) {
        element.classList.remove('highlight-flash')
      }
    }, 5000)
  } else {
    console.warn('未找到要高亮的预约记录:', targetId)
    clearHighlight()
  }
}

onMounted(async () => {
  await loadBookings()
  await scrollToHighlight()
})
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
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  color: var(--text-muted);
}

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

/* 卡片基础样式 - 左边框加粗至8px */
.booking-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  border-left: 8px solid transparent;
}

/* 状态颜色标识（左边框 + 浅色背景） */
.booking-card.status-cancelled {
  border-left-color: #f56c6c;
  background: #fff0f0;
}

.booking-card.status-completed {
  border-left-color: #67c23a;
  background: #f0f9eb;
}

.booking-card.status-in_progress {
  border-left-color: #e6a23c;
  background: #fdf6ec;
}

.booking-card.status-confirmed {
  border-left-color: #409eff;
  background: #ecf5ff;
}

.booking-card.status-pending {
  border-left-color: #909399;
  background: #f4f4f5;
}

/* 代办预约 - 更醒目的蓝色边框 + 额外阴影 + 特殊背景 */
.booking-card.is-proxy {
  border-left-color: #3b82f6;
  box-shadow: 0 0 0 1px rgba(59,130,246,0.3), inset 0 0 0 1px rgba(59,130,246,0.1);
  background: #f0f7ff;
}

/* 已取消的代办：保持红色优先（覆盖代办样式） */
.booking-card.status-cancelled.is-proxy {
  border-left-color: #f56c6c;
  background: #fff0f0;
  box-shadow: none;
}

/* 折叠头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1.25rem;
  cursor: pointer;
  transition: background 0.2s;
}

.card-header:hover {
  background: var(--bg-tertiary);
}

.service-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.service-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.create-time {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.expand-icon {
  color: var(--text-muted);
  font-size: 1rem;
  transition: transform 0.2s;
}

/* 折叠内容 */
.card-body {
  padding: 0 1.25rem 1.25rem 1.25rem;
  border-top: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
}

.info-row {
  display: flex;
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
}

.label {
  width: 5rem;
  flex-shrink: 0;
  color: var(--text-muted);
}

.staff-info {
  margin-top: 0.75rem;
  padding-top: 0.5rem;
  border-top: 0.0625rem solid var(--border-color);
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}

/* 高亮闪烁动画（覆盖状态颜色） */
.booking-card.highlight-flash {
  animation: highlightBlink 0.5s ease-in-out 3, pulseGlow 1.5s ease-out;
  background: linear-gradient(90deg,
    rgba(212, 168, 67, 0.08) 0%,
    rgba(212, 168, 67, 0.2) 50%,
    rgba(212, 168, 67, 0.08) 100%);
  border-left-color: var(--gold) !important;
  border-left-width: 8px;
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
</style>