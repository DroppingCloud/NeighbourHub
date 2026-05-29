<template>
  <div class="feedback-container">
    <div class="page-header">
      <h2>服务评价</h2>
      <p>感谢您使用社区服务，请对本次服务进行评价</p>
    </div>

    <div v-if="booking" class="service-card">
      <div class="service-info">
        <div class="service-name">{{ booking.serviceTypeLabel || booking.serviceType }}</div>
        <div class="service-time">预约时间：{{ booking.expectTime }}</div>
        <div class="service-staff">服务地址：{{ booking.address || '-' }}</div>
      </div>
    </div>

    <div class="rating-section">
      <div class="section-title"><span class="required">*</span>综合评分</div>
      <div class="rating-stars">
        <span
          v-for="star in 5"
          :key="star"
          class="star"
          :class="{ active: star <= (hoverStar || rating) }"
          @click="rating = star"
          @mouseenter="hoverStar = star"
          @mouseleave="hoverStar = 0"
        >
          <el-icon>
            <StarFilled v-if="star <= (hoverStar || rating)" />
            <Star v-else />
          </el-icon>
        </span>
        <span class="rating-label">{{ ratingLabels[rating] }}</span>
      </div>
    </div>

    <div class="tags-section">
      <div class="section-title">服务评价（可多选）</div>
      <div class="tags-grid">
        <button
          v-for="tag in serviceTags"
          :key="tag"
          type="button"
          class="tag-item"
          :class="{ active: selectedTags.includes(tag) }"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <div class="content-section">
      <div class="section-title">详细评价</div>
      <el-input
        v-model="feedbackContent"
        type="textarea"
        :rows="4"
        placeholder="请描述您的服务体验"
        maxlength="500"
        show-word-limit
      />
    </div>

    <div class="action-buttons">
      <el-button @click="router.back()" size="large">稍后评价</el-button>
      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        :disabled="rating === 0 || !booking"
        @click="submitFeedback"
      >
        提交评价
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { getBookingDetail, submitBookingFeedback, type BookingVO } from '@/api/booking'

const router = useRouter()
const route = useRoute()
const booking = ref<BookingVO | null>(null)
const rating = ref(0)
const hoverStar = ref(0)
const selectedTags = ref<string[]>([])
const feedbackContent = ref('')
const submitting = ref(false)

const ratingLabels: Record<number, string> = {
  0: '请选择',
  1: '很差',
  2: '较差',
  3: '一般',
  4: '满意',
  5: '非常满意'
}

const serviceTags = ['态度热情', '响应及时', '专业规范', '服务细致', '沟通清晰', '准时到达']

function toggleTag(tag: string) {
  const index = selectedTags.value.indexOf(tag)
  if (index >= 0) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

async function loadBooking() {
  const id = Number(route.query.bookingId)
  if (!id) {
    ElMessage.warning('缺少预约ID')
    router.push('/booking/list')
    return
  }
  booking.value = await getBookingDetail(id)
}

async function submitFeedback() {
  if (!booking.value || rating.value === 0) {
    ElMessage.warning('请先选择评分')
    return
  }
  await ElMessageBox.confirm('确认提交评价吗？', '提示', { type: 'info' })
  const tagText = selectedTags.value.length ? `【${selectedTags.value.join('、')}】` : ''
  const fullFeedback = `${rating.value}星 ${tagText} ${feedbackContent.value}`.trim()

  submitting.value = true
  try {
    await submitBookingFeedback(booking.value.bookingId, fullFeedback)
    ElMessage.success('评价提交成功')
    router.push('/booking/list')
  } finally {
    submitting.value = false
  }
}

onMounted(loadBooking)
</script>

<style scoped>
.feedback-container {
  max-width: 56.25rem;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header,
.service-card,
.rating-section,
.tags-section,
.content-section {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  color: var(--text-muted);
}

.service-card,
.rating-section,
.tags-section,
.content-section {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  box-shadow: var(--shadow-sm);
}

.service-name,
.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.75rem;
}

.service-time,
.service-staff {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.required {
  color: var(--vermilion);
  margin-right: 0.25rem;
}

.rating-stars,
.tags-grid,
.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.star {
  font-size: 2rem;
  cursor: pointer;
  color: var(--text-muted);
}

.star.active {
  color: #f5a623;
}

.rating-label {
  color: var(--text-secondary);
}

.tag-item {
  border: none;
  border-radius: 999px;
  padding: 0.5rem 1rem;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  cursor: pointer;
}

.tag-item.active {
  background: var(--gold);
  color: #fff;
}

.action-buttons {
  justify-content: flex-end;
  margin-bottom: 2rem;
}
</style>
