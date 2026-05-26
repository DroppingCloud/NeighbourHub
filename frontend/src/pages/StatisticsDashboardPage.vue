<template>
  <div class="dashboard">
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="4" v-for="stat in overviewCards" :key="stat.label">
        <el-card class="stat-card">
          <div class="stat-num" :style="{color: stat.color}">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card header="申请状态分布">
          <StatisticsChart :options="pieOptions" style="height:300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="近7天申请趋势">
          <StatisticsChart :options="lineOptions" style="height:300px" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card header="热门事项排行">
          <StatisticsChart :options="barOptions" style="height:260px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getOverview } from '@/api/statistics'
import StatisticsChart from '@/components/StatisticsChart.vue'

const loading = ref(false)
const overview = ref<any>({})

const overviewCards = computed(() => [
  { label: '总申请量', value: overview.value.totalApplications || 0, color: '#409eff' },
  { label: '待审核', value: overview.value.pendingCount || 0, color: '#e6a23c' },
  { label: '已通过', value: overview.value.approvedCount || 0, color: '#67c23a' },
  { label: '已驳回', value: overview.value.rejectedCount || 0, color: '#f56c6c' },
  { label: '需补件', value: overview.value.supplementCount || 0, color: '#909399' },
  { label: '服务预约', value: overview.value.serviceBookingCount || 0, color: '#53a8ff' }
])

const pieOptions = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: '60%', data: [
    { name: '待审核', value: overview.value.pendingCount || 0 },
    { name: '已通过', value: overview.value.approvedCount || 0 },
    { name: '已驳回', value: overview.value.rejectedCount || 0 },
    { name: '需补件', value: overview.value.supplementCount || 0 },
    { name: '已办结', value: overview.value.completedCount || 0 }
  ]}]
}))

const lineOptions = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (overview.value.dailyTrend || []).map((d: any) => d.date) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', data: (overview.value.dailyTrend || []).map((d: any) => d.count), smooth: true }]
}))

const barOptions = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (overview.value.topItems || []).map((d: any) => d.itemName) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: (overview.value.topItems || []).map((d: any) => d.count) }]
}))

onMounted(async () => {
  loading.value = true
  try { overview.value = await getOverview() }
  finally { loading.value = false }
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-num { font-size: 28px; font-weight: bold; }
.stat-label { color: #909399; margin-top: 6px; }
</style>
