<template>
  <div class="home-container">
    <div class="welcome-banner">
      <div class="banner-content">
        <h2>欢迎回来，{{ userInfo?.realName || userInfo?.username }}</h2>
        <p>智慧社区服务平台，为您提供一站式政务服务</p>
      </div>
      <div class="banner-stats">
        <div class="stat-item">
          <div class="stat-value">{{ stats.applications }}</div>
          <div class="stat-label">办理中申请</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.bookings }}</div>
          <div class="stat-label">进行中预约</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ unreadCount }}</div>
          <div class="stat-label">未读消息</div>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h3 class="section-title">快捷服务</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('/guide')">
          <div class="action-icon"><el-icon><MagicStick /></el-icon></div>
          <span>AI智能导办</span>
        </div>
        <div class="action-card" @click="goTo('/application/submit')">
          <div class="action-icon"><el-icon><Document /></el-icon></div>
          <span>事项办理</span>
        </div>
        <div class="action-card" @click="goTo('/booking')">
          <div class="action-icon"><el-icon><Calendar /></el-icon></div>
          <span>服务预约</span>
        </div>
        <div class="action-card" @click="goTo('/progress')">
          <div class="action-icon"><el-icon><TrendCharts /></el-icon></div>
          <span>进度查询</span>
        </div>
      </div>
    </div>

    <div class="hot-services">
      <h3 class="section-title">热门事项</h3>
      <div class="service-list" v-loading="loadingServices">
        <div
          v-for="service in hotServices"
          :key="service.itemId"
          class="service-item"
          @click="applyService(service)"
        >
          <div class="service-info">
            <span class="service-name">{{ service.itemName }}</span>
            <span class="service-category">{{ service.category }}</span>
          </div>
          <el-icon><Right /></el-icon>
        </div>
        <el-empty v-if="!loadingServices && hotServices.length === 0" description="暂无可办理事项" />
      </div>
    </div>

    <div class="recent-applications">
      <div class="section-header">
        <h3 class="section-title">最近申请</h3>
        <router-link to="/application/list" class="view-all">查看全部 &gt;</router-link>
      </div>
      <el-table :data="recentApplications" style="width: 100%" border v-loading="loadingApplications">
        <el-table-column prop="itemName" label="事项名称" min-width="120" />
        <el-table-column prop="submitTime" label="提交时间" min-width="180" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusLabel || getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, Document, Calendar, TrendCharts, Right } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getUnreadCount } from '@/api/notice'
import { getApplicationList, type ApplicationVO } from '@/api/application'
import { getBookingList, type BookingVO } from '@/api/booking'
import { getPublicServiceItemList, type ServiceItemVO } from '@/api/serviceItem'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const unreadCount = ref(0)
const applications = ref<ApplicationVO[]>([])
const bookings = ref<BookingVO[]>([])
const services = ref<ServiceItemVO[]>([])
const loadingApplications = ref(false)
const loadingServices = ref(false)

type HotService = ServiceItemVO & { itemId: number; itemName: string; category: string }

const defaultHotServices: HotService[] = [
  {
    itemId: 1,
    itemName: '居住证办理',
    itemCode: 'ITEM_001',
    category: '证件',
    description: '为非本地户籍居民办理居住证。',
    conditions: '',
    status: 'online',
    createTime: ''
  },
  {
    itemId: 2,
    itemName: '老年补贴申请',
    itemCode: 'ITEM_002',
    category: '补贴',
    description: '为符合条件的老年居民申请高龄津贴。',
    conditions: '',
    status: 'online',
    createTime: ''
  },
  {
    itemId: 3,
    itemName: '居住证明开具',
    itemCode: 'ITEM_003',
    category: '证明',
    description: '开具在本辖区居住的证明材料。',
    conditions: '',
    status: 'online',
    createTime: ''
  },
  {
    itemId: 4,
    itemName: '便民证明',
    itemCode: 'ITEM_004',
    category: '证明',
    description: '开具常见社区便民证明材料。',
    conditions: '',
    status: 'online',
    createTime: ''
  }
]

const stats = computed(() => ({
  applications: applications.value.filter(a => !['completed', 'rejected'].includes(a.status)).length,
  bookings: bookings.value.filter(b => !['completed', 'cancelled'].includes(b.status)).length
}))

const hotServices = computed(() =>
  defaultHotServices.map(item => {
    const matched = services.value.find(service => service.itemName === item.itemName)
    return matched ? { ...item, ...matched } : item
  })
)
const recentApplications = computed(() => applications.value.slice(0, 3))

onMounted(() => {
  loadHomeData()
})

async function loadHomeData() {
  if (!authStore.token) {
    return
  }
  loadingApplications.value = true
  loadingServices.value = true
  try {
    const [appResult, bookingResult, serviceResult, countResult] = await Promise.allSettled([
      getApplicationList({ pageNum: 1, pageSize: 10 }),
      getBookingList(1, 10),
      getPublicServiceItemList({ status: 'online', pageNum: 1, pageSize: 8 }),
      getUnreadCount()
    ])
    applications.value = appResult.status === 'fulfilled' ? getPageRows<ApplicationVO>(appResult.value) : []
    bookings.value = bookingResult.status === 'fulfilled' ? getPageRows<BookingVO>(bookingResult.value) : []
    services.value = serviceResult.status === 'fulfilled' ? getPageRows<ServiceItemVO>(serviceResult.value) : []
    unreadCount.value = countResult.status === 'fulfilled' ? Number(countResult.value || 0) : 0
  } finally {
    loadingApplications.value = false
    loadingServices.value = false
  }
}

function getPageRows<T>(page: any): T[] {
  if (Array.isArray(page)) return page
  return page?.records || page?.list || page?.rows || []
}

function goTo(path: string) {
  router.push(path)
}

function applyService(service: HotService) {
  router.push({
    path: '/application/submit',
    query: {
      serviceId: String(service.itemId),
      itemName: service.itemName
    }
  })
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
    completed: '已办结',
    rejected: '已驳回'
  }
  return map[status] || status
}
</script>

<style scoped>
.home-container {
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

.banner-content h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin-bottom: 0.375rem;
}

.banner-content p {
  color: rgba(255,255,255,0.7);
}

.banner-stats {
  display: flex;
  gap: 2.5rem;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--gold-light);
}

.stat-label {
  font-size: 0.8125rem;
  color: rgba(255,255,255,0.6);
  margin-top: 0.25rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 1rem;
}

.quick-actions {
  margin-bottom: 1.75rem;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(12rem, 100%), 1fr));
  gap: 1.25rem;
}

.action-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-sm);
}

.action-card:hover {
  transform: translateY(-0.25rem);
  box-shadow: var(--shadow-md);
}

.action-icon {
  width: 3rem;
  height: 3rem;
  background: var(--bg-tertiary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 0.75rem;
  font-size: 1.5rem;
  color: var(--text-primary);
}

.hot-services,
.recent-applications {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.75rem 2rem;
  margin-bottom: 1.75rem;
  box-shadow: var(--shadow-sm);
}

.service-list {
  display: flex;
  flex-direction: column;
}

.service-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 0.0625rem solid var(--border-color);
  cursor: pointer;
  color: var(--text-primary);
  gap: 0.75rem;
  transition: all 0.25s ease;
}

.service-info {
  min-width: 0;
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.service-item:last-child {
  border-bottom: none;
}

.service-item:hover {
  background: var(--bg-tertiary);
  margin: 0 -0.5rem;
  padding: 1rem 0.5rem;
  border-radius: var(--radius-sm);
}

.service-name {
  font-weight: 500;
  margin-right: 0.25rem;
  overflow-wrap: anywhere;
}

.service-category {
  font-size: 0.75rem;
  color: var(--text-muted);
  background: var(--bg-tertiary);
  padding: 0.125rem 0.5rem;
  border-radius: 1.25rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.view-all {
  color: var(--gold);
  text-decoration: none;
  font-size: 0.8125rem;
}

.view-all:hover {
  text-decoration: underline;
}

:deep(.el-table) {
  background-color: transparent;
}

:deep(.el-table th),
:deep(.el-table td) {
  background-color: transparent;
  border-bottom-color: var(--border-color);
}

:deep(.el-table .cell) {
  white-space: normal;
  line-height: 1.6;
  word-break: break-word;
}

@media (max-width: 48rem) {
  .banner-stats {
    display: none;
  }
}
</style>
