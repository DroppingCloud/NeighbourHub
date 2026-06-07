<template>
  <div class="statistics-container">
    <div class="page-header">
      <div class="header-info">
        <h2>统计分析看板</h2>
        <p>平台运营数据统计与分析</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadStatistics" class="refresh-btn">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="kpi-grid" v-loading="loading">
      <div class="kpi-card">
        <div class="kpi-visual blue">
          <div class="kpi-icon"><el-icon><Document /></el-icon></div>
          <div class="kpi-ring"></div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.totalApplications }}</div>
          <div class="kpi-label">申请总数</div>
          <div class="kpi-trend">累计办理事项</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-visual orange">
          <div class="kpi-icon"><el-icon><Tickets /></el-icon></div>
          <div class="kpi-ring"></div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.pendingCount }}</div>
          <div class="kpi-label">待审核</div>
          <div class="kpi-trend warning">需及时处理</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-visual green">
          <div class="kpi-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="kpi-ring"></div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.completedCount }}</div>
          <div class="kpi-label">已办结</div>
          <div class="kpi-trend success">服务完成</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-visual purple">
          <div class="kpi-icon"><el-icon><Calendar /></el-icon></div>
          <div class="kpi-ring"></div>
        </div>
        <div class="kpi-body">
          <div class="kpi-value">{{ overview.serviceBookingCount }}</div>
          <div class="kpi-label">预约总数</div>
          <div class="kpi-trend">便民服务</div>
        </div>
      </div>
    </div>

    <!-- 图表区 第一行 -->
    <div class="charts-row">
      <div class="chart-card chart-primary">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3>申请趋势</h3>
            <span class="chart-badge">近期数据</span>
          </div>
          <span class="chart-subtitle">基于提交时间统计</span>
        </div>
        <div ref="trendChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card chart-secondary">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3>状态分布</h3>
            <span class="chart-badge">实时</span>
          </div>
          <span class="chart-subtitle">各状态占比</span>
        </div>
        <div ref="statusChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- 图表区 第二行 -->
    <div class="charts-row">
      <div class="chart-card chart-secondary">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3>事项排行</h3>
            <span class="chart-badge gold">热门</span>
          </div>
          <span class="chart-subtitle">按申请量排序</span>
        </div>
        <div ref="itemChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card chart-primary">
        <div class="chart-header">
          <div class="chart-title-group">
            <h3>服务统计</h3>
            <span class="chart-badge">预约类</span>
          </div>
          <span class="chart-subtitle">按服务类型聚合</span>
        </div>
        <div ref="serviceChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- 数据明细表 -->
    <div class="detail-card">
      <div class="chart-header">
        <div class="chart-title-group">
          <h3>事项办理明细</h3>
          <span class="chart-badge">汇总</span>
        </div>
      </div>
      <el-table :data="itemStats" stripe style="width: 100%" class="detail-table">
        <el-table-column prop="itemId" label="事项 ID" min-width="120" />
        <el-table-column prop="count" label="申请数量" min-width="120" sortable>
          <template #default="{ row }">
            <div class="count-cell">
              <span class="count-value">{{ row.count }}</span>
              <div class="count-bar" :style="{ width: getBarWidth(row.count) }"></div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Calendar, CircleCheck, Document, Refresh, Tickets } from '@element-plus/icons-vue'
import { getItemStats, getOverview, getServiceStats, type StatisticsOverview } from '@/api/statistics'

type ItemStat = { itemId?: number | string; count?: number | string }
type ServiceStat = { serviceType?: string; status?: string; count?: number | string }

const loading = ref(false)
const trendChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()
const itemChartRef = ref<HTMLElement>()
const serviceChartRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null
let itemChart: echarts.ECharts | null = null
let serviceChart: echarts.ECharts | null = null

const overview = ref<StatisticsOverview>({
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
const itemStats = ref<ItemStat[]>([])
const serviceStats = ref<ServiceStat[]>([])

const maxCount = computed(() => Math.max(...itemStats.value.map(r => toNumber(r.count)), 1))

function getBarWidth(count: number | string | undefined) {
  return `${(toNumber(count) / maxCount.value) * 100}%`
}

function toNumber(value: unknown) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function dayLabel(row: Record<string, any>) {
  return String(row.day || row.date || row.submitDate || '-')
}

// Color palette
const palette = {
  ink: '#1a1a2e',
  inkLight: '#2d3561',
  gold: '#d4a843',
  goldLight: '#f0c96e',
  jade: '#27ae60',
  vermilion: '#c0392b',
  blue: '#3b82f6',
  purple: '#8b5cf6',
  orange: '#f59e0b'
}

function initCharts() {
  if (trendChartRef.value && !trendChart) trendChart = echarts.init(trendChartRef.value)
  if (statusChartRef.value && !statusChart) statusChart = echarts.init(statusChartRef.value)
  if (itemChartRef.value && !itemChart) itemChart = echarts.init(itemChartRef.value)
  if (serviceChartRef.value && !serviceChart) serviceChart = echarts.init(serviceChartRef.value)
}

function renderCharts() {
  const trendRows = overview.value.dailyTrend || []

  trendChart?.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 26, 46, 0.92)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    grid: { top: 20, left: 12, right: 20, bottom: 20, containLabel: true },
    xAxis: {
      type: 'category',
      data: trendRows.map(dayLabel),
      axisLine: { lineStyle: { color: 'rgba(26,26,46,0.1)' } },
      axisLabel: { color: '#9090aa', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(26,26,46,0.06)', type: 'dashed' } },
      axisLabel: { color: '#9090aa', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      name: '申请数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: trendRows.map(row => toNumber((row as any).count)),
      lineStyle: { color: palette.ink, width: 2.5 },
      itemStyle: { color: palette.ink, borderWidth: 2, borderColor: '#fff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(45, 53, 97, 0.15)' },
          { offset: 1, color: 'rgba(45, 53, 97, 0.02)' }
        ])
      }
    }]
  })

  statusChart?.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(26, 26, 46, 0.92)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      bottom: 8,
      textStyle: { color: '#9090aa', fontSize: 11 },
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16
    },
    color: [palette.orange, palette.jade, palette.gold, palette.blue, palette.vermilion],
    series: [{
      type: 'pie',
      radius: ['44%', '68%'],
      center: ['50%', '42%'],
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 600 },
        itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.15)' }
      },
      data: [
        { name: '待审核', value: overview.value.pendingCount },
        { name: '已通过', value: overview.value.approvedCount },
        { name: '待补件', value: overview.value.supplementCount },
        { name: '已办结', value: overview.value.completedCount },
        { name: '已驳回', value: overview.value.rejectedCount }
      ].filter(item => toNumber(item.value) > 0)
    }]
  })

  itemChart?.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(26, 26, 46, 0.92)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    grid: { top: 12, left: 12, right: 20, bottom: 12, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(26,26,46,0.06)', type: 'dashed' } },
      axisLabel: { color: '#9090aa', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'category',
      data: itemStats.value.map(row => `事项 ${row.itemId ?? '-'}`),
      axisLine: { lineStyle: { color: 'rgba(26,26,46,0.1)' } },
      axisLabel: { color: '#5a5a7a', fontSize: 11 },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      barWidth: 18,
      data: itemStats.value.map(row => toNumber(row.count)),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: palette.gold },
          { offset: 1, color: palette.goldLight }
        ]),
        borderRadius: [0, 4, 4, 0]
      }
    }]
  })

  const serviceTypeMap: Record<string, string> = {
    dining: '助餐', accompany: '陪诊', home_visit: '上门'
  }
  const statusMap: Record<string, string> = {
    pending: '待确认', confirmed: '已确认', in_progress: '进行中',
    completed: '已完成', cancelled: '已取消'
  }
  const statusColors: Record<string, string> = {
    pending: palette.orange,
    confirmed: palette.blue,
    in_progress: palette.purple,
    completed: palette.jade,
    cancelled: '#adb5bd'
  }

  // 按服务类型聚合数据，构建堆叠柱状图
  const serviceTypes = ['dining', 'accompany', 'home_visit']
  const statuses = ['pending', 'confirmed', 'in_progress', 'completed', 'cancelled']

  // 构建数据矩阵: statusCountMap[status][serviceType] = count
  const statusCountMap: Record<string, Record<string, number>> = {}
  for (const s of statuses) {
    statusCountMap[s] = {}
    for (const t of serviceTypes) {
      statusCountMap[s][t] = 0
    }
  }
  for (const row of serviceStats.value) {
    const t = row.serviceType || ''
    const s = row.status || ''
    if (statusCountMap[s] && statusCountMap[s][t] !== undefined) {
      statusCountMap[s][t] = toNumber(row.count)
    }
  }

  serviceChart?.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(26, 26, 46, 0.92)',
      borderColor: 'transparent',
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      bottom: 8,
      textStyle: { color: '#9090aa', fontSize: 11 },
      itemWidth: 14,
      itemHeight: 10,
      itemGap: 16
    },
    grid: { top: 20, left: 12, right: 20, bottom: 48, containLabel: true },
    xAxis: {
      type: 'category',
      data: serviceTypes.map(t => serviceTypeMap[t] || t),
      axisLine: { lineStyle: { color: 'rgba(26,26,46,0.1)' } },
      axisLabel: { color: '#5a5a7a', fontSize: 12, fontWeight: 500 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(26,26,46,0.06)', type: 'dashed' } },
      axisLabel: { color: '#9090aa', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: statuses.map(s => ({
      name: statusMap[s] || s,
      type: 'bar',
      stack: 'total',
      barWidth: 36,
      data: serviceTypes.map(t => statusCountMap[s][t]),
      itemStyle: {
        color: statusColors[s] || '#ccc',
        borderRadius: 0
      },
      emphasis: {
        itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.12)' }
      }
    }))
  })
}

async function loadStatistics() {
  loading.value = true
  try {
    const [overviewData, itemData, serviceData] = await Promise.all([
      getOverview(),
      getItemStats(),
      getServiceStats()
    ])
    overview.value = overviewData
    itemStats.value = itemData || []
    serviceStats.value = serviceData || []
    await nextTick()
    initCharts()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function handleResize() {
  trendChart?.resize()
  statusChart?.resize()
  itemChart?.resize()
  serviceChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initCharts()
  await loadStatistics()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
  itemChart?.dispose()
  serviceChart?.dispose()
})
</script>

<style scoped>
.statistics-container {
  max-width: 75rem;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  margin-bottom: 1.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.page-header p {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.refresh-btn {
  border-radius: 2rem !important;
  padding: 0.5rem 1.25rem !important;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(14rem, 100%), 1fr));
  gap: 1.25rem;
  margin-bottom: 1.75rem;
}

.kpi-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.kpi-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 5rem;
  height: 5rem;
  border-radius: 50%;
  opacity: 0.04;
  transform: translate(30%, -30%);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.kpi-visual {
  position: relative;
  width: 3.5rem;
  height: 3.5rem;
  flex-shrink: 0;
}

.kpi-icon {
  position: relative;
  z-index: 1;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.kpi-ring {
  position: absolute;
  inset: -3px;
  border-radius: 1.125rem;
  opacity: 0.15;
  animation: kpi-pulse 3s ease-in-out infinite;
}

@keyframes kpi-pulse {
  0%, 100% { transform: scale(1); opacity: 0.15; }
  50% { transform: scale(1.08); opacity: 0.08; }
}

.kpi-visual.blue .kpi-icon { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.kpi-visual.blue .kpi-ring { background: #3b82f6; }
.kpi-visual.blue + .kpi-body .kpi-value { color: #2d3561; }

.kpi-visual.orange .kpi-icon { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.kpi-visual.orange .kpi-ring { background: #f59e0b; }

.kpi-visual.green .kpi-icon { background: rgba(39, 174, 96, 0.1); color: #27ae60; }
.kpi-visual.green .kpi-ring { background: #27ae60; }

.kpi-visual.purple .kpi-icon { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
.kpi-visual.purple .kpi-ring { background: #8b5cf6; }

.kpi-body {
  flex: 1;
  min-width: 0;
}

.kpi-value {
  font-size: 1.875rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
  margin-bottom: 0.25rem;
}

.kpi-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.kpi-trend {
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.kpi-trend.warning { color: #f59e0b; }
.kpi-trend.success { color: #27ae60; }

/* Charts */
.charts-row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 1.25rem;
  margin-bottom: 1.25rem;
}

.charts-row:nth-child(4) {
  grid-template-columns: 0.8fr 1.2fr;
}

.chart-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem 1.75rem;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.3s;
}

.chart-card:hover {
  box-shadow: var(--shadow-md);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chart-title-group {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.chart-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.chart-badge {
  font-size: 0.625rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  padding: 0.2rem 0.5rem;
  border-radius: 1rem;
  background: rgba(45, 53, 97, 0.08);
  color: var(--text-muted);
  text-transform: uppercase;
}

.chart-badge.gold {
  background: rgba(212, 168, 67, 0.12);
  color: var(--gold);
}

.chart-subtitle {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.chart-body {
  width: 100%;
  height: 18rem;
}

/* Detail Table */
.detail-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem 1.75rem;
  box-shadow: var(--shadow-sm);
}

.detail-table {
  margin-top: 0.5rem;
}

.count-cell {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.count-value {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 2rem;
}

.count-bar {
  height: 0.375rem;
  background: linear-gradient(90deg, var(--gold), var(--gold-light));
  border-radius: 0.25rem;
  transition: width 0.6s ease;
  max-width: 12rem;
}

:deep(.el-table) {
  background: transparent !important;
}

:deep(.el-table th) {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
  font-weight: 600;
  border-bottom: none !important;
}

:deep(.el-table td) {
  color: var(--text-secondary) !important;
  border-bottom-color: var(--border-color) !important;
}

:deep(.el-table tr:hover td) {
  background: rgba(212, 168, 67, 0.04) !important;
}

/* Responsive */
@media (max-width: 48rem) {
  .kpi-grid {
    grid-template-columns: 1fr 1fr;
  }

  .charts-row,
  .charts-row:nth-child(4) {
    grid-template-columns: 1fr;
  }

  .chart-body {
    height: 14rem;
  }

  .kpi-card {
    padding: 1.25rem;
  }

  .kpi-value {
    font-size: 1.5rem;
  }
}

@media (max-width: 30rem) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
