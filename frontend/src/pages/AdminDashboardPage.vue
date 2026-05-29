<template>
  <div class="page-container">
    <div class="page-header">
      <h2>后台管理</h2>
      <p>平台配置与业务数据概览</p>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalApplications }}</div>
          <div class="stat-label">申请总数</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.pendingCount }}</div>
          <div class="stat-label">待处理申请</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.serviceBookingCount }}</div>
          <div class="stat-label">预约总数</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.completedCount }}</div>
          <div class="stat-label">已办结申请</div>
        </div>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="事项概览" name="services">
        <el-table :data="services" style="width: 100%" v-loading="loading">
          <el-table-column prop="itemName" label="事项名称" min-width="180" />
          <el-table-column prop="category" label="分类" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'enabled' ? 'success' : 'danger'">
                {{ row.status === 'enabled' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="editService(row.itemId)">配置</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" style="margin-top: 1rem" @click="addService">新增事项</el-button>
      </el-tab-pane>

      <el-tab-pane label="统计分析" name="statistics">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <div class="panel">
              <h3>热门事项</h3>
              <el-table :data="stats.topItems" size="small">
                <el-table-column prop="itemName" label="事项" />
                <el-table-column prop="count" label="申请量" width="100" />
              </el-table>
            </div>
          </el-col>
          <el-col :xs="24" :md="12">
            <div class="panel">
              <h3>最近趋势</h3>
              <el-table :data="stats.dailyTrend" size="small">
                <el-table-column prop="date" label="日期" />
                <el-table-column prop="count" label="申请量" width="100" />
              </el-table>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOverview, type StatisticsOverview } from '@/api/statistics'
import { getServiceItemList, type ServiceItemVO } from '@/api/serviceItem'

const router = useRouter()
const activeTab = ref('services')
const loading = ref(false)
const services = ref<ServiceItemVO[]>([])
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

onMounted(() => {
  loadDashboard()
})

async function loadDashboard() {
  loading.value = true
  try {
    const [overview, servicePage] = await Promise.all([
      getOverview(),
      getServiceItemList({ pageNum: 1, pageSize: 20 })
    ])
    stats.value = overview
    services.value = getPageRows<ServiceItemVO>(servicePage)
  } finally {
    loading.value = false
  }
}

function getPageRows<T>(page: any): T[] {
  if (Array.isArray(page)) return page
  return page?.records || page?.list || page?.rows || []
}

function editService(id: number) {
  router.push({ path: '/admin/service-config', query: { id } })
}

function addService() {
  router.push('/admin/service-config')
}
</script>

<style scoped>
.page-container {
  max-width: 75rem;
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

.stat-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  text-align: center;
  box-shadow: var(--shadow-sm);
  margin-bottom: 1.25rem;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 0.5rem;
}

.admin-tabs {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  margin-top: 1.25rem;
}

.panel {
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1rem;
  margin-bottom: 1rem;
}

.panel h3 {
  font-size: 1rem;
  color: var(--text-primary);
  margin-bottom: 1rem;
}
</style>
