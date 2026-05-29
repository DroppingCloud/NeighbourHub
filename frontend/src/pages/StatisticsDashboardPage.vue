<template>
  <div class="statistics-container">
    <div class="page-header">
      <div>
        <h2>统计分析看板</h2>
        <p>平台运营数据统计与分析</p>
      </div>
      <el-button :loading="loading" @click="loadStatistics">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="stats-grid" v-loading="loading">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalApplications }}</div>
          <div class="stat-label">申请总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.pendingCount }}</div>
          <div class="stat-label">待审核申请</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.completedCount }}</div>
          <div class="stat-label">已办结申请</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.serviceBookingCount }}</div>
          <div class="stat-label">预约总数</div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card half">
        <div class="chart-header">
          <h3>最近申请趋势</h3>
          <span class="chart-subtitle">来自数据库申请提交时间</span>
        </div>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card half">
        <div class="chart-header">
          <h3>申请状态分布</h3>
          <span class="chart-subtitle">实时统计申请状态</span>
        </div>
        <div ref="statusChartRef" class="chart-container"></div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card half">
        <div class="chart-header">
          <h3>事项申请排行</h3>
          <span class="chart-subtitle">按事项 ID 聚合</span>
        </div>
        <div ref="itemChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card half">
        <div class="chart-header">
          <h3>社区服务统计</h3>
          <span class="chart-subtitle">按服务类型和状态聚合</span>
        </div>
        <div ref="serviceChartRef" class="chart-container"></div>
      </div>
    </div>

    <div class="data-table-card">
      <div class="chart-header">
        <h3>事项办理统计</h3>
      </div>
      <el-table :data="itemStats" stripe style="width: 100%">
        <el-table-column prop="itemId" label="事项 ID" min-width="120" />
        <el-table-column prop="count" label="申请数量" min-width="120" sortable />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
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

function toNumber(value: unknown) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function dayLabel(row: Record<string, any>) {
  return String(row.day || row.date || row.submitDate || '-')
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
    tooltip: { trigger: 'axis' },
    grid: { top: 24, left: 36, right: 18, bottom: 28, containLabel: true },
    xAxis: { type: 'category', data: trendRows.map(dayLabel) },
    yAxis: { type: 'value', name: '数量' },
    series: [
      {
        name: '申请数',
        type: 'line',
        smooth: true,
        data: trendRows.map(row => toNumber((row as any).count)),
        lineStyle: { color: '#2d3561', width: 2 },
        areaStyle: { opacity: 0.08 }
      }
    ]
  })

  statusChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
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
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 20, left: 48, right: 18, bottom: 28, containLabel: true },
    xAxis: { type: 'value', name: '申请数' },
    yAxis: { type: 'category', data: itemStats.value.map(row => `事项 ${row.itemId ?? '-'}`) },
    series: [{ type: 'bar', data: itemStats.value.map(row => toNumber(row.count)), itemStyle: { color: '#d4a843' } }]
  })

  serviceChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['50%', '45%'],
      data: serviceStats.value.map(row => ({
        name: `${row.serviceType || '未知'}-${row.status || '未知'}`,
        value: toNumber(row.count)
      }))
    }]
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
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.page-header p,
.chart-subtitle {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(12rem, 100%), 1fr));
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.stat-card,
.chart-card,
.data-table-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  box-shadow: var(--shadow-sm);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.stat-icon.blue {
  background: rgba(45, 53, 97, 0.1);
  color: #2d3561;
}

.stat-icon.green {
  background: rgba(39, 174, 96, 0.1);
  color: #27ae60;
}

.stat-icon.orange {
  background: rgba(230, 126, 34, 0.1);
  color: #e67e22;
}

.stat-icon.purple {
  background: rgba(155, 89, 182, 0.1);
  color: #9b59b6;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.charts-row {
  display: flex;
  gap: 1.25rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.chart-card.half {
  flex: 1;
  min-width: 18rem;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chart-header h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.chart-container {
  width: 100%;
  height: 17.5rem;
}

.data-table-card {
  margin-top: 0;
}

:deep(.el-table th) {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

:deep(.el-table td) {
  color: var(--text-secondary);
  border-bottom-color: var(--border-color);
}

@media (max-width: 48rem) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.half {
    min-width: 100%;
  }

  .chart-container {
    height: 14rem;
  }
}
</style>
