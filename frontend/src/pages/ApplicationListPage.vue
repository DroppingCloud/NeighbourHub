<template>
  <div class="applications-container">
    <div class="page-header">
      <h2>我的申请</h2>
      <p>查看您提交的事项申请记录</p>
    </div>

    <div class="filter-tabs">
      <el-radio-group v-model="statusFilter" size="large" @change="loadApplications">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待审核</el-radio-button>
        <el-radio-button value="approved">已通过</el-radio-button>
        <el-radio-button value="supplement_required">待补件</el-radio-button>
        <el-radio-button value="completed">已办结</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="application-list">
      <div v-for="app in applications" :key="app.applicationId" class="application-card">
        <div class="card-header">
          <div class="service-info">
            <span class="service-name">{{ app.itemName }}</span>
            <el-tag :type="getStatusType(app.status)" size="small">
              {{ app.statusLabel || getStatusText(app.status) }}
            </el-tag>
          </div>
          <span class="submit-time">{{ app.submitTime }}</span>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="label">事项分类</span>
            <span>{{ app.category || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">办理备注</span>
            <span>{{ cleanRemark(app.remark, app.itemName) }}</span>
          </div>
          <div class="info-row">
            <span class="label">代办申请</span>
            <span>{{ app.isProxy ? '是' : '否' }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && applications.length === 0" description="暂无申请记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getApplicationList, type ApplicationVO } from '@/api/application'

const statusFilter = ref('')
const loading = ref(false)
const applications = ref<ApplicationVO[]>([])

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    supplement_required: 'danger',
    completed: 'success',
    rejected: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    supplement_required: '待补件',
    completed: '已办结',
    rejected: '已驳回'
  }
  return map[status] || status
}

function cleanRemark(remark?: string, itemName?: string) {
  if (!remark || /^\?+$/.test(remark)) {
    return itemName ? `${itemName}申请` : '暂无'
  }
  return remark
}

async function loadApplications() {
  loading.value = true
  try {
    const page = await getApplicationList({
      status: statusFilter.value || undefined,
      pageNum: 1,
      pageSize: 50
    })
    applications.value = page.records || []
  } finally {
    loading.value = false
  }
}

onMounted(loadApplications)
</script>

<style scoped>
.applications-container {
  max-width: 56.25rem;
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

.filter-tabs {
  margin-bottom: 1.5rem;
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 12rem;
}

.application-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  box-shadow: var(--shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
  padding-bottom: 0.75rem;
  border-bottom: 0.0625rem solid var(--border-color);
  margin-bottom: 0.75rem;
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

.submit-time,
.label {
  color: var(--text-muted);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-row {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.label {
  width: 5rem;
  flex-shrink: 0;
}
</style>
