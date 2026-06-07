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
        <div class="stat-label">已接取</div>
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

    <el-tabs v-model="activeTab" class="dispatch-tabs" @tab-change="handleTabChange">
      <!-- 待调度 -->
      <el-tab-pane label="待调度" name="pending">
        <div
          v-for="booking in pendingBookings"
          :key="booking.bookingId"
          :id="`booking-${booking.bookingId}`"
          class="dispatch-card"
          :class="{ highlighted: highlightedBookingId === String(booking.bookingId) }"
        >
          <div class="card-header" @click="toggleExpand(booking.bookingId)">
            <div class="service-info">
              <span class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</span>
              <span :class="['status-pill', `status-${booking.status}`]">
                {{ booking.statusLabel || booking.status }}
              </span>
            </div>
            <div class="header-right">
              <span class="create-time">{{ booking.createTime }}</span>
              <el-icon class="expand-icon">
                <ArrowDown v-if="!expandedIds.has(booking.bookingId)" />
                <ArrowUp v-else />
              </el-icon>
            </div>
          </div>
          <div v-show="expandedIds.has(booking.bookingId)" class="card-body">
            <div class="info-row">
              <span class="label">预约时间：</span>
              <span>{{ booking.expectTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">服务地址：</span>
              <span>{{ booking.address || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">备注：</span>
              <span>{{ booking.remark || '无' }}</span>
            </div>
            <div class="card-actions">
              <el-button type="primary" size="small" @click.stop="claim(booking.bookingId)">
                <el-icon><Position /></el-icon>
                接取
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="pendingBookings.length === 0" description="暂无待调度预约" />
      </el-tab-pane>

      <!-- 已接取 -->
      <el-tab-pane label="已接取" name="confirmed">
        <div
          v-for="booking in confirmedBookings"
          :key="booking.bookingId"
          :id="`booking-${booking.bookingId}`"
          class="dispatch-card"
          :class="{ highlighted: highlightedBookingId === String(booking.bookingId) }"
        >
          <div class="card-header" @click="toggleExpand(booking.bookingId)">
            <div class="service-info">
              <span class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</span>
              <span :class="['status-pill', `status-${booking.status}`]">
                {{ booking.statusLabel || booking.status }}
              </span>
            </div>
            <div class="header-right">
              <span class="create-time">{{ booking.createTime }}</span>
              <el-icon class="expand-icon">
                <ArrowDown v-if="!expandedIds.has(booking.bookingId)" />
                <ArrowUp v-else />
              </el-icon>
            </div>
          </div>
          <div v-show="expandedIds.has(booking.bookingId)" class="card-body">
            <div class="info-row">
              <span class="label">预约时间：</span>
              <span>{{ booking.expectTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">服务地址：</span>
              <span>{{ booking.address || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">备注：</span>
              <span>{{ booking.remark || '无' }}</span>
            </div>
            <div class="card-actions">
              <el-button type="warning" size="small" @click.stop="start(booking.bookingId)">
                <el-icon><VideoPlay /></el-icon>
                开始服务
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="confirmedBookings.length === 0" description="暂无已接取预约" />
      </el-tab-pane>

      <!-- 服务中 -->
      <el-tab-pane label="服务中" name="in_progress">
        <div
          v-for="booking in inProgressBookings"
          :key="booking.bookingId"
          :id="`booking-${booking.bookingId}`"
          class="dispatch-card"
          :class="{ highlighted: highlightedBookingId === String(booking.bookingId) }"
        >
          <div class="card-header" @click="toggleExpand(booking.bookingId)">
            <div class="service-info">
              <span class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</span>
              <span :class="['status-pill', `status-${booking.status}`]">
                {{ booking.statusLabel || booking.status }}
              </span>
            </div>
            <div class="header-right">
              <span class="create-time">{{ booking.createTime }}</span>
              <el-icon class="expand-icon">
                <ArrowDown v-if="!expandedIds.has(booking.bookingId)" />
                <ArrowUp v-else />
              </el-icon>
            </div>
          </div>
          <div v-show="expandedIds.has(booking.bookingId)" class="card-body">
            <div class="info-row">
              <span class="label">预约时间：</span>
              <span>{{ booking.expectTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">服务地址：</span>
              <span>{{ booking.address || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">备注：</span>
              <span>{{ booking.remark || '无' }}</span>
            </div>
            <div class="card-actions">
              <el-button type="success" size="small" @click.stop="completeService(booking.bookingId)">
                <el-icon><CircleCheck /></el-icon>
                完成服务
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="inProgressBookings.length === 0" description="暂无服务中预约" />
      </el-tab-pane>

      <!-- 已完成 -->
      <el-tab-pane label="已完成" name="completed">
        <div
          v-for="booking in completedBookings"
          :key="booking.bookingId"
          :id="`booking-${booking.bookingId}`"
          class="dispatch-card"
          :class="{ highlighted: highlightedBookingId === String(booking.bookingId) }"
        >
          <div class="card-header" @click="toggleExpand(booking.bookingId)">
            <div class="service-info">
              <span class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</span>
              <span :class="['status-pill', `status-${booking.status}`]">
                {{ booking.statusLabel || booking.status }}
              </span>
            </div>
            <div class="header-right">
              <span class="create-time">{{ booking.createTime }}</span>
              <el-icon class="expand-icon">
                <ArrowDown v-if="!expandedIds.has(booking.bookingId)" />
                <ArrowUp v-else />
              </el-icon>
            </div>
          </div>
          <div v-show="expandedIds.has(booking.bookingId)" class="card-body">
            <div class="info-row">
              <span class="label">预约时间：</span>
              <span>{{ booking.expectTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">服务地址：</span>
              <span>{{ booking.address || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">备注：</span>
              <span>{{ booking.remark || '无' }}</span>
            </div>
            <div class="card-actions">
              <el-button type="info" size="small" @click.stop="showFeedback(booking)">
                <el-icon><ChatLineSquare /></el-icon>
                查看详情
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="completedBookings.length === 0" description="暂无已完成预约" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Position, VideoPlay, ChatLineSquare, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { ElRate, ElTag } from 'element-plus'
import {
  assignBooking,
  completeBooking,
  startBooking,
  getStaffBookingList,
  type BookingVO
} from '@/api/booking'

const route = useRoute()
const validTabs = ['pending', 'confirmed', 'in_progress', 'completed']
const activeTab = ref(validTabs.includes(String(route.query.tab || '')) ? String(route.query.tab) : 'pending')
const bookings = ref<BookingVO[]>([])
const highlightedBookingId = computed(() => String(route.query.highlightId || ''))
const expandedIds = ref<Set<number>>(new Set())

const pendingCount = ref(0)
const confirmedCount = ref(0)
const inProgressCount = ref(0)
const completedCount = ref(0)

const pendingBookings = computed(() => bookings.value.filter(item => item.status === 'pending'))
const confirmedBookings = computed(() => bookings.value.filter(item => item.status === 'confirmed'))
const inProgressBookings = computed(() => bookings.value.filter(item => item.status === 'in_progress'))
const completedBookings = computed(() => bookings.value.filter(item => item.status === 'completed'))

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
  expandedIds.value = new Set(expandedIds.value)
}

async function loadBookings() {
  const status = activeTab.value === 'pending' ? 'pending' :
                 activeTab.value === 'confirmed' ? 'confirmed' :
                 activeTab.value === 'in_progress' ? 'in_progress' :
                 activeTab.value === 'completed' ? 'completed' : undefined
  const page = await getStaffBookingList(1, status === 'completed' ? 1000 : 100, status)
  bookings.value = page.records || []
  if (highlightedBookingId.value) {
    const idNum = Number(highlightedBookingId.value)
    expandedIds.value.add(idNum)
    expandedIds.value = new Set(expandedIds.value)
  }
  await scrollToHighlightedBooking()
}

async function loadStatistics() {
  const [pending, confirmed, inProgress, completed] = await Promise.all([
    getStaffBookingList(1, 1, 'pending'),
    getStaffBookingList(1, 1, 'confirmed'),
    getStaffBookingList(1, 1, 'in_progress'),
    getStaffBookingList(1, 1000, 'completed')
  ])
  pendingCount.value = pending.total
  confirmedCount.value = confirmed.total
  inProgressCount.value = inProgress.total
  completedCount.value = completed.total
}

async function dispatch(bookingId: number) {
  try {
    await assignBooking(bookingId)
    ElMessage.success('接取成功')
    activeTab.value = 'confirmed'
    await loadBookings()
    await loadStatistics()
  } catch (error: any) {
    const msg = error?.message || String(error)
    if (msg.includes('社区') || msg.includes('无社区')) {
      ElMessageBox.alert('派单失败：该预约未关联社区信息，系统无法确定分配范围。请检查预约来源或联系管理员补全申请的社区属性，再尝试派单。', '派单失败', { type: 'warning' })
    } else {
      ElMessage.error(msg || '派单失败')
    }
  }
}
async function claim(bookingId: number) { await dispatch(bookingId) }

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
  await completeBooking(bookingId)
  ElMessage.success('服务已完成')
  await loadBookings()
  await loadStatistics()
}

// ========== 评价解析核心函数（彻底清除星号和数字星） ==========
function parseFeedback(feedbackStr?: string) {
  if (!feedbackStr || !feedbackStr.trim()) {
    return { rating: null, tags: [], comment: '居民未做出评价', isJson: false }
  }

  // 尝试 JSON 解析
  try {
    const parsed = JSON.parse(feedbackStr)
    if (typeof parsed === 'object' && parsed !== null) {
      return {
        rating: typeof parsed.rating === 'number' ? parsed.rating : null,
        tags: Array.isArray(parsed.tags) ? parsed.tags : [],
        comment: parsed.comment || '无详细内容',
        isJson: true
      }
    }
  } catch {
    // 不是 JSON，继续文本解析
  }

  const text = feedbackStr
  let rating: number | null = null
  let tags: string[] = []
  let comment = ''

  // 提取星星数
  const starMatch = text.match(/(★+)/)
  if (starMatch) {
    rating = starMatch[1].length
  } else {
    const numMatch = text.match(/(\d+)\s*星/)
    if (numMatch) rating = parseInt(numMatch[1], 10)
  }

  // 提取标签
  const serviceEvalIndex = text.indexOf('服务评价')
  const detailIndex = text.indexOf('详细评价')
  if (serviceEvalIndex !== -1 && detailIndex > serviceEvalIndex) {
    const tagBlock = text.substring(serviceEvalIndex + '服务评价'.length, detailIndex)
    const words = tagBlock.split(/\s+/).filter(w => w.trim().length > 0)
    tags = words.filter(w => !w.includes('可多选') && !w.includes('服务评价'))
  } else {
    const commonTags = ['态度热情', '响应及时', '专业规范', '服务细致', '沟通清晰', '准时到达']
    const foundTags = commonTags.filter(tag => text.includes(tag))
    if (foundTags.length) tags = foundTags
  }

  // 提取详细评价
  if (detailIndex !== -1) {
    let rawComment = text.substring(detailIndex + '详细评价'.length)
    rawComment = rawComment.replace(/^[\s\n]+/, '')
    comment = rawComment.trim()
  } else {
    const lines = text.split(/\n/).filter(l => l.trim().length > 0)
    if (lines.length) comment = lines[lines.length - 1].trim()
  }

  // 强化清洗：删除所有评分相关文字（星星、数字星、中文数字星等）
  if (comment) {
    // 1. 删除开头的连续星星（如 ★★★★★ 或 ★★★★★ 非常满意）
    comment = comment.replace(/^[★☆]+(\s+非常满意)?\s*/, '')
    // 2. 删除开头的“数字星”（如 5星、5星等）
    comment = comment.replace(/^\d+\s*星\s*/, '')
    // 3. 删除开头的“五星”、“四星”等中文数字星
    comment = comment.replace(/^[一二三四五]星\s*/, '')
    // 4. 删除标签（基于已提取的 tags）
    if (tags.length > 0) {
      const escapedTags = tags.map(t => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
      const tagPattern = escapedTags.join('|')
      const allTagRegex = new RegExp(`[【\\[（(]?(${tagPattern})[】\\]）)]?[、，,;；\\s]*`, 'g')
      comment = comment.replace(allTagRegex, '')
    }
    // 5. 删除残留的分隔符和空格
    comment = comment.replace(/^[、，,;；\s]+/, '')
    comment = comment.trim()
  }

  if (!comment) comment = '无详细内容'

  return { rating, tags, comment, isJson: false }
}

function buildFeedbackContent(feedbackStr?: string) {
  const { rating, tags, comment, isJson } = parseFeedback(feedbackStr)

  // JSON 分支保持不变
  if (isJson && (rating !== null || tags.length > 0)) {
    const children = []
    if (rating !== null && rating >= 1 && rating <= 5) {
      children.push(
        h('div', { style: { marginBottom: '12px', display: 'flex', alignItems: 'center' } }, [
          h('span', { style: { marginRight: '8px', color: '#606266' } }, '综合评分：'),
          h(ElRate, { modelValue: rating, disabled: true, showScore: false, textColor: '#ff9900' })
        ])
      )
    }
    if (tags.length > 0) {
      children.push(
        h('div', { style: { marginBottom: '12px', display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '8px' } }, [
          h('span', { style: { marginRight: '8px', color: '#606266' } }, '服务评价：'),
          ...tags.map(tag => h(ElTag, { size: 'small', type: 'success', effect: 'plain' }, () => tag))
        ])
      )
    }
    if (comment) {
      children.push(
        h('div', { style: { marginTop: '8px', borderTop: '1px solid #e4e7ed', paddingTop: '12px' } }, [
          h('div', { style: { color: '#606266', marginBottom: '4px' } }, '详细评价：'),
          h('div', { style: { color: '#303133', lineHeight: '1.5', whiteSpace: 'pre-wrap' } }, comment)
        ])
      )
    }
    if (children.length === 0) return h('div', '暂无有效评价内容')
    return h('div', { style: { maxHeight: '60vh', overflowY: 'auto' } }, children)
  }

  // 非 JSON 情况：三行展示
  const children = []
  if (rating !== null && rating >= 1 && rating <= 5) {
    children.push(
      h('div', { style: { marginBottom: '12px', display: 'flex', alignItems: 'center' } }, [
        h('span', { style: { marginRight: '8px', color: '#606266' } }, '综合评分：'),
        h(ElRate, { modelValue: rating, disabled: true, showScore: false, textColor: '#ff9900' })
      ])
    )
  }
  if (tags.length > 0) {
    children.push(
      h('div', { style: { marginBottom: '12px', display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '8px' } }, [
        h('span', { style: { marginRight: '8px', color: '#606266' } }, '服务评价：'),
        ...tags.map(tag => h(ElTag, { size: 'small', type: 'success', effect: 'plain' }, () => tag))
      ])
    )
  }
  if (comment) {
    children.push(
      h('div', { style: { marginTop: '8px', borderTop: '1px solid #e4e7ed', paddingTop: '12px' } }, [
        h('div', { style: { color: '#606266', marginBottom: '4px' } }, '详细评价：'),
        h('div', { style: { color: '#303133', lineHeight: '1.5', whiteSpace: 'pre-wrap' } }, comment)
      ])
    )
  }
  if (children.length === 0) {
    return h('div', { style: { whiteSpace: 'pre-wrap' } }, comment || '居民未做出评价')
  }
  return h('div', { style: { maxHeight: '60vh', overflowY: 'auto' } }, children)
}

async function showFeedback(booking: BookingVO) {
  const contentVNode = buildFeedbackContent(booking.feedback)
  await ElMessageBox({
    title: '居民评价',
    message: contentVNode,
    confirmButtonText: '关闭',
    type: 'info',
    showCancelButton: false
  })
}
// ========== 评价解析结束 ==========

function handleTabChange() {
  loadBookings()
}

async function scrollToHighlightedBooking() {
  if (!highlightedBookingId.value) return
  await nextTick()
  const element = document.getElementById(`booking-${highlightedBookingId.value}`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

async function locateHighlightedBooking() {
  const id = highlightedBookingId.value
  if (!id) return
  for (const status of validTabs) {
    const page = await getStaffBookingList(1, 100, status)
    const records = page.records || []
    if (records.some((item: BookingVO) => String(item.bookingId) === id)) {
      activeTab.value = status
      bookings.value = records
      const idNum = Number(id)
      expandedIds.value.add(idNum)
      expandedIds.value = new Set(expandedIds.value)
      await scrollToHighlightedBooking()
      return
    }
  }
}

watch(() => route.query.tab, (tab) => {
  const nextTab = String(tab || '')
  if (validTabs.includes(nextTab) && activeTab.value !== nextTab) {
    activeTab.value = nextTab
    loadBookings()
  }
})
watch(() => route.query.highlightId, () => {
  locateHighlightedBooking()
})

onMounted(() => {
  if (highlightedBookingId.value) {
    locateHighlightedBooking()
  } else {
    loadBookings()
  }
  loadStatistics()
})
</script>

<style scoped>
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
  margin-bottom: 1rem;
  border: 0.0625rem solid var(--border-color);
  transition: all 0.2s;
}

.dispatch-card.highlighted {
  border-color: var(--gold);
  box-shadow: 0 0 0 0.1875rem rgba(212, 168, 67, 0.18);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1rem 1.25rem;
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
  width: 5.5rem;
  flex-shrink: 0;
  color: var(--text-muted);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 0.0625rem solid var(--border-color);
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
.status-in_progress {
  color: #e6a23c;
}
.status-completed {
  color: #15803d;
}
</style>