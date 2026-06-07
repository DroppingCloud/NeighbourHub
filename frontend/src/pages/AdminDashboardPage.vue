<template>
  <div class="admin-dashboard">
    <!-- 顶部欢迎区 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>管理控制台</h2>
          <p class="welcome-sub">{{ greeting }}，{{ userName }}。以下是平台运营概况。</p>
        </div>
        <div class="welcome-actions">
          <el-button class="btn-refresh" :loading="loading" @click="loadDashboard">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
      <div class="welcome-deco"></div>
    </div>

    <!-- 核心运营指标 -->
    <div class="metrics-grid" v-loading="loading">
      <div class="metric-card" v-for="m in metrics" :key="m.key">
        <div class="metric-icon-wrap" :class="m.color">
          <el-icon :size="22"><component :is="m.icon" /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
        <div class="metric-accent" :class="m.color"></div>
      </div>
    </div>

    <!-- 主内容双栏 -->
    <div class="dashboard-columns">
      <!-- 左栏：快捷操作 + 待办 -->
      <div class="col-left">
        <div class="card shortcuts-card">
          <div class="card-head">
            <h3>快捷操作</h3>
          </div>
          <div class="shortcuts-grid">
            <button class="shortcut-item" @click="$router.push('/admin/service-config')">
              <div class="shortcut-icon teal"><el-icon><Document /></el-icon></div>
              <span>事项配置</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/user-manage')">
              <div class="shortcut-icon indigo"><el-icon><User /></el-icon></div>
              <span>用户管理</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/statistics')">
              <div class="shortcut-icon amber"><el-icon><DataAnalysis /></el-icon></div>
              <span>统计分析</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/workorder')">
              <div class="shortcut-icon rose"><el-icon><Tickets /></el-icon></div>
              <span>工单管理</span>
            </button>
          </div>
        </div>

        <div class="card pending-card">
          <div class="card-head">
            <h3>待办事项</h3>
            <span class="badge" v-if="pendingItems.length">{{ pendingItems.length }}</span>
          </div>
          <div class="pending-list" v-if="pendingItems.length">
            <div class="pending-row" v-for="item in pendingItems" :key="item.label">
              <div class="pending-dot" :class="item.urgency"></div>
              <span class="pending-text">{{ item.label }}</span>
              <span class="pending-count">{{ item.count }}</span>
            </div>
          </div>
          <div class="empty-state" v-else>
            <el-icon :size="32" color="var(--text-muted)"><CircleCheck /></el-icon>
            <p>暂无紧急待办</p>
          </div>
        </div>
      </div>

      <!-- 右栏：热门事项 + 近期趋势 -->
      <div class="col-right">
        <div class="card rank-card">
          <div class="card-head">
            <h3>热门事项</h3>
            <span class="card-hint">按申请量排序</span>
          </div>
          <div class="rank-list" v-if="topItems.length">
            <div class="rank-row" v-for="(item, idx) in topItems" :key="idx">
              <span class="rank-num" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
              <span class="rank-name">{{ item.itemName }}</span>
              <div class="rank-bar-wrap">
                <div class="rank-bar" :style="{ width: getBarWidth(item.count) }"></div>
              </div>
              <span class="rank-count">{{ item.count }}</span>
            </div>
          </div>
          <div class="empty-state" v-else>
            <p>暂无数据</p>
          </div>
        </div>

        <div class="card trend-card">
          <div class="card-head">
            <h3>近期趋势</h3>
            <span class="card-hint">每日申请量</span>
          </div>
          <div ref="trendChartRef" class="trend-chart-body"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { CircleCheck, DataAnalysis, Document, Refresh, Tickets, User } from '@element-plus/icons-vue'
import { getOverview, type StatisticsOverview } from '@/api/statistics'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const userName = computed(() => authStore.userInfo?.realName || authStore.userInfo?.username || '管理员')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const loading = ref(false)
const stats = ref<StatisticsOverview>({
  totalApplications: 0,
  pendingCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  supplementCount: 0,
  completedCount: 0,
  serviceBookingCount: 0,
  topItems: [],
  dailyTrend: []
})

const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const metrics = computed(() => [
  { key: 'total', label: '申请总数', value: stats.value.totalApplications, icon: Document, color: 'blue' },
  { key: 'pending', label: '待处理', value: stats.value.pendingCount, icon: Tickets, color: 'orange' },
  { key: 'completed', label: '已办结', value: stats.value.completedCount, icon: CircleCheck, color: 'green' },
  { key: 'booking', label: '服务预约', value: stats.value.serviceBookingCount, icon: DataAnalysis, color: 'purple' }
])

const pendingItems = computed(() => {
  const items: { label: string; count: number; urgency: string }[] = []
  if (stats.value.pendingCount > 0) {
    items.push({ label: '待审核申请', count: stats.value.pendingCount, urgency: 'high' })
  }
  if (stats.value.supplementCount > 0) {
    items.push({ label: '待补件申请', count: stats.value.supplementCount, urgency: 'medium' })
  }
  return items
})

const topItems = computed(() => (stats.value.topItems || []).slice(0, 5))
const maxTopCount = computed(() => Math.max(...topItems.value.map(i => i.count), 1))
function getBarWidth(count: number) {
  return `${(count / maxTopCount.value) * 100}%`
}

async function loadDashboard() {
  loading.value = true
  try {
    stats.value = await getOverview()
    await nextTick()
    renderTrend()
  } finally {
    loading.value = false
  }
}

function renderTrend() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const rows = stats.value.dailyTrend || []
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 26, 46, 0.9)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    grid: { top: 12, left: 8, right: 16, bottom: 16, containLabel: true },
    xAxis: {
      type: 'category',
      data: rows.map((r: any) => r.day || r.date || r.submitDate || ''),
      axisLine: { lineStyle: { color: 'rgba(26,26,46,0.08)' } },
      axisLabel: { color: '#9090aa', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(26,26,46,0.05)', type: 'dashed' } },
      axisLabel: { color: '#9090aa', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      barWidth: 16,
      data: rows.map((r: any) => Number(r.count) || 0),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#d4a843' },
          { offset: 1, color: 'rgba(212, 168, 67, 0.3)' }
        ]),
        borderRadius: [3, 3, 0, 0]
      }
    }]
  })
}

function handleResize() {
  trendChart?.resize()
}

onMounted(async () => {
  await loadDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 75rem;
  margin: 0 auto;
}

/* Welcome Banner */
.welcome-banner {
  position: relative;
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%);
  border-radius: var(--radius-lg);
  padding: 2rem 2.25rem;
  margin-bottom: 1.75rem;
  overflow: hidden;
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h2 {
  font-family: var(--font-serif);
  font-size: 1.375rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.375rem;
}

.welcome-sub {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
}

.btn-refresh {
  background: rgba(255, 255, 255, 0.12) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
  border-radius: 2rem !important;
  padding: 0.5rem 1.25rem !important;
  backdrop-filter: blur(4px);
  transition: all 0.2s;
}

.btn-refresh:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.35) !important;
}

.welcome-deco {
  position: absolute;
  right: -2rem;
  top: -2rem;
  width: 10rem;
  height: 10rem;
  border-radius: 50%;
  background: rgba(212, 168, 67, 0.12);
  pointer-events: none;
}

.welcome-deco::after {
  content: '';
  position: absolute;
  right: 2.5rem;
  bottom: -1rem;
  width: 5rem;
  height: 5rem;
  border-radius: 50%;
  background: rgba(212, 168, 67, 0.08);
}

/* Metrics Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.75rem;
}

.metric-card {
  position: relative;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.375rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: transform 0.25s, box-shadow 0.25s;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.metric-accent {
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  border-radius: 0 2px 2px 0;
}

.metric-accent.blue { background: #3b82f6; }
.metric-accent.orange { background: #f59e0b; }
.metric-accent.green { background: #27ae60; }
.metric-accent.purple { background: #8b5cf6; }

.metric-icon-wrap {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-icon-wrap.blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.metric-icon-wrap.orange { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.metric-icon-wrap.green { background: rgba(39, 174, 96, 0.1); color: #27ae60; }
.metric-icon-wrap.purple { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }

.metric-info {
  flex: 1;
  min-width: 0;
}

.metric-value {
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.metric-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-top: 0.125rem;
}

/* Dashboard Columns */
.dashboard-columns {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 1.25rem;
}

.col-left, .col-right {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Card Base */
.card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem 1.75rem;
  box-shadow: var(--shadow-sm);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.card-head h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.card-hint {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.badge {
  background: var(--vermilion);
  color: #fff;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.125rem 0.5rem;
  border-radius: 1rem;
  min-width: 1.25rem;
  text-align: center;
}

/* Shortcuts */
.shortcuts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
  color: var(--text-secondary);
  text-align: left;
}

.shortcut-item:hover {
  border-color: var(--gold);
  background: rgba(212, 168, 67, 0.04);
  color: var(--text-primary);
}

.shortcut-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 1.125rem;
}

.shortcut-icon.teal { background: rgba(20, 184, 166, 0.1); color: #14b8a6; }
.shortcut-icon.indigo { background: rgba(99, 102, 241, 0.1); color: #6366f1; }
.shortcut-icon.amber { background: rgba(212, 168, 67, 0.1); color: #d4a843; }
.shortcut-icon.rose { background: rgba(244, 63, 94, 0.1); color: #f43f5e; }

/* Pending List */
.pending-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.pending-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-sm);
  background: var(--paper);
}

.pending-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  flex-shrink: 0;
}

.pending-dot.high { background: var(--vermilion); }
.pending-dot.medium { background: #f59e0b; }
.pending-dot.low { background: var(--jade); }

.pending-text {
  flex: 1;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.pending-count {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

/* Rank List */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.rank-num {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--paper);
  flex-shrink: 0;
}

.rank-num.top {
  background: var(--gold);
  color: #fff;
}

.rank-name {
  font-size: 0.875rem;
  color: var(--text-secondary);
  min-width: 5rem;
  flex-shrink: 0;
}

.rank-bar-wrap {
  flex: 1;
  height: 0.375rem;
  background: var(--paper);
  border-radius: 0.25rem;
  overflow: hidden;
}

.rank-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--gold), var(--gold-light));
  border-radius: 0.25rem;
  transition: width 0.6s ease;
}

.rank-count {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 2rem;
  text-align: right;
}

/* Trend Chart */
.trend-chart-body {
  width: 100%;
  height: 12rem;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem 0;
}

.empty-state p {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

/* Responsive */
@media (max-width: 64rem) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 30rem) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .shortcuts-grid {
    grid-template-columns: 1fr;
  }
  .welcome-banner {
    padding: 1.5rem;
  }
}

/* Dark mode support */
:root.dark .welcome-banner {
  background: linear-gradient(135deg, #0d0d1a 0%, #1a1a2e 100%);
}

:root.dark .metric-card,
:root.dark .card {
  border: 1px solid rgba(255, 255, 255, 0.06);
}

:root.dark .pending-row {
  background: rgba(255, 255, 255, 0.04);
}

:root.dark .rank-num {
  background: rgba(255, 255, 255, 0.06);
}

:root.dark .rank-bar-wrap {
  background: rgba(255, 255, 255, 0.06);
}
</style>
