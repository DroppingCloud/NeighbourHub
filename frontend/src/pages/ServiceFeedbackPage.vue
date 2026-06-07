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
          @click="!isReadOnly && (rating = star)"
          @mouseenter="!isReadOnly && (hoverStar = star)"
          @mouseleave="!isReadOnly && (hoverStar = 0)"
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
          @click="!isReadOnly && toggleTag(tag)"
          :disabled="isReadOnly"
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
        :readonly="isReadOnly"
      />
    </div>

    <div class="action-buttons">
      <el-button @click="router.back()" size="large">返回</el-button>
      <el-button
        v-if="!isReadOnly"
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
import { onMounted, ref, computed } from 'vue'
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
const routeMode = computed(() => String(route.query.mode || ''))
const effectiveMode = computed(() => {
  if (!hasActualFeedback(booking.value?.feedback)) {
    return 'write'
  }
  return routeMode.value === 'view' || routeMode.value === 'read' ? routeMode.value : 'view'
})
const isReadOnly = computed(() => effectiveMode.value === 'view' || effectiveMode.value === 'read')

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

/**
 * 解析已保存的评价字符串，还原评分、标签、详细内容
 * 格式示例: "5星 【态度热情、响应及时】 服务很好"
 * 或 "4星 专业规范 还不错" (无括号)
 * 或 "3星  【】 一般般" (无标签)
 */
function hasActualFeedback(feedback?: string) {
  if (!feedback) return false
  const trimmed = feedback.trim()
  return trimmed !== '工作人员确认服务完成'
}

function parseFeedback(feedbackStr: string): { rating: number; tags: string[]; content: string } {
  // 默认值
  let rating = 0
  let tags: string[] = []
  let content = ''

  if (!feedbackStr) return { rating, tags, content }

  // 匹配开头的数字星，例如 "5星"
  const ratingMatch = feedbackStr.match(/^(\d+)星/)
  if (ratingMatch) {
    rating = parseInt(ratingMatch[1], 10)
  }

  // 提取标签部分：可能为 "【标签1、标签2】" 或没有括号
  let remaining = feedbackStr.replace(/^\d+星\s*/, '')
  const bracketMatch = remaining.match(/^【(.*?)】/)
  if (bracketMatch) {
    const tagStr = bracketMatch[1]
    if (tagStr) {
      tags = tagStr.split(/[、,，]/).map(t => t.trim()).filter(t => t)
    }
    remaining = remaining.substring(bracketMatch[0].length)
  } else {
    // 没有括号，尝试匹配第一个空格前的部分作为标签（可选）
    // 但为了避免误判，只有当剩余字符串开头是标签列表中的词时才提取
    const firstSpaceIdx = remaining.indexOf(' ')
    if (firstSpaceIdx > 0) {
      const possibleTag = remaining.substring(0, firstSpaceIdx)
      if (serviceTags.includes(possibleTag)) {
        tags = [possibleTag]
        remaining = remaining.substring(firstSpaceIdx + 1)
      }
    }
  }

  content = remaining.trim()
  return { rating, tags, content }
}

async function loadBooking() {
  const id = Number(route.query.bookingId)
  if (!id) {
    ElMessage.warning('缺少预约ID')
    router.push('/booking/list')
    return
  }
  try {
    const detail = await getBookingDetail(id)
    booking.value = detail

    // 如果已有真实评价内容，解析并回填表单
    if (hasActualFeedback(detail.feedback)) {
      const parsed = parseFeedback(detail.feedback)
      rating.value = parsed.rating
      selectedTags.value = parsed.tags
      feedbackContent.value = parsed.content
    } else {
      // 无历史评价时重置表单
      rating.value = 0
      selectedTags.value = []
      feedbackContent.value = ''
    }
  } catch (error) {
    ElMessage.error('加载预约信息失败')
    router.push('/booking/list')
  }
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
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(loadBooking)
</script>

<style scoped>
/* 原有样式保持不变 */
.feedback-container {
  max-width: 75rem;
  margin: 0 auto;
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