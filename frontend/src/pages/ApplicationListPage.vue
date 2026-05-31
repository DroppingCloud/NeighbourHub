<template>
  <div class="applications-container">
    <div class="page-header">
      <h2>我的申请</h2>
      <p>查看您提交的事项申请记录，待补件和已撤回申请可继续修改后重新提交。</p>
    </div>

    <div class="filter-tabs">
      <el-radio-group v-model="statusFilter" size="large" @change="loadApplications">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待审核</el-radio-button>
        <el-radio-button value="approved">已通过</el-radio-button>
        <el-radio-button value="supplement_required">待补件</el-radio-button>
        <el-radio-button value="cancelled">已撤回</el-radio-button>
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
          <div v-if="app.materialCompleteness" class="info-row">
            <span class="label">材料状态</span>
            <span>
              {{ app.materialCompleteness.uploadedRequiredCount }}/{{ app.materialCompleteness.requiredCount }} 已上传
              <template v-if="!app.materialCompleteness.complete">
                ，缺少：{{ app.materialCompleteness.missingMaterialNames.join('、') || '必填材料' }}
              </template>
            </span>
          </div>
        </div>

        <div v-if="hasActions(app)" class="card-actions">
          <el-button v-if="app.status === 'supplement_required'" type="primary" @click="goSupplement(app.applicationId)">
            去补件
          </el-button>
          <el-button v-if="app.status === 'cancelled'" type="primary" @click="goResubmit(app.applicationId)">
            修改并重新提交
          </el-button>
          <el-button v-if="canWithdraw(app.status)" plain type="danger" @click="withdraw(app)">
            撤回申请
          </el-button>
        </div>
      </div>
      <el-empty v-if="!loading && applications.length === 0" description="暂无申请记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApplicationList, withdrawApplication, type ApplicationVO } from '@/api/application'

const router = useRouter()
const statusFilter = ref('')
const loading = ref(false)
const applications = ref<ApplicationVO[]>([])

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    supplement_required: 'danger',
    completed: 'success',
    rejected: 'info',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    supplement_required: '待补件',
    completed: '已办结',
    rejected: '已驳回',
    cancelled: '已撤回'
  }
  return map[status] || status
}

function cleanRemark(remark?: string, itemName?: string) {
  if (!remark || /^\?+$/.test(remark)) {
    return itemName ? `${itemName}申请` : '暂无'
  }
  return remark
}

function canWithdraw(status: string) {
  return ['pending', 'approved', 'supplement_required'].includes(status)
}

function hasActions(app: ApplicationVO) {
  return app.status === 'supplement_required' || app.status === 'cancelled' || canWithdraw(app.status)
}

function goSupplement(applicationId: number) {
  router.push({
    path: '/material-upload',
    query: {
      mode: 'supplement',
      applicationId
    }
  })
}

function goResubmit(applicationId: number) {
  router.push({
    path: '/material-upload',
    query: {
      mode: 'resubmit',
      applicationId
    }
  })
}

async function withdraw(app: ApplicationVO) {
  try {
    await ElMessageBox.confirm(
      `确认撤回“${app.itemName || '该事项'}”申请吗？撤回后可在“已撤回”分类中修改材料并重新提交。`,
      '撤回申请',
      {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await withdrawApplication(app.applicationId)
    ElMessage.success('申请已撤回')
    await loadApplications()
  } catch {
    // 用户取消撤回时无需提示。
  }
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
  } catch {
    applications.value = []
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
  line-height: 1.6;
}

.filter-tabs {
  margin-bottom: 1.5rem;
  overflow-x: auto;
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
  word-break: break-word;
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

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}
</style>
