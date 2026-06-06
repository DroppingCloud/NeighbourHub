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
            <span>
              <el-tag v-if="app.isProxy" type="info" size="small">代他人办理</el-tag>
              <span v-else>否</span>
            </span>
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

        <div class="card-actions">
          <el-button plain @click="openDetail(app.applicationId)">
            查看详情
          </el-button>
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

    <el-dialog
      v-model="detailVisible"
      class="application-detail-dialog"
      title="申请详情"
      width="72rem"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="currentApplication">
          <section class="detail-section">
            <div class="detail-section-title">基本信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">事项名称</span>
                <span>{{ currentApplication.itemName || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">事项分类</span>
                <span>{{ currentApplication.category || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">申请状态</span>
                <el-tag :type="getStatusType(currentApplication.status)" size="small">
                  {{ currentApplication.statusLabel || getStatusText(currentApplication.status) }}
                </el-tag>
              </div>
              <div class="detail-item">
                <span class="detail-label">提交时间</span>
                <span>{{ currentApplication.submitTime || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">工单状态</span>
                <span>{{ currentApplication.workOrderStatusLabel || currentApplication.workOrderStatus || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">代办申请</span>
                <span>
                  <el-tag v-if="currentApplication.isProxy" type="info" size="small">代他人办理</el-tag>
                  <span v-else>否</span>
                </span>
              </div>
              <div class="detail-item detail-item-wide">
                <span class="detail-label">办理备注</span>
                <span>{{ cleanRemark(currentApplication.remark, currentApplication.itemName) }}</span>
              </div>
              <div class="detail-item detail-item-wide">
                <span class="detail-label">审核意见</span>
                <span>{{ currentApplication.auditOpinion || '-' }}</span>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title">填写资料</div>
            <div v-if="formRows.length" class="form-data-table">
              <div v-for="row in formRows" :key="row.key" class="form-data-row">
                <span class="detail-label">{{ formatFieldLabel(row.key) }}</span>
                <span>{{ formatFieldValue(row.value) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无填写资料" />
          </section>

          <section class="detail-section">
            <div class="detail-section-title">提交材料</div>
            <div v-if="currentApplication.materialCompleteness" class="material-summary">
              必填材料 {{ currentApplication.materialCompleteness.uploadedRequiredCount }}/{{ currentApplication.materialCompleteness.requiredCount }} 已通过
              <template v-if="!currentApplication.materialCompleteness.complete">
                ，缺少：{{ currentApplication.materialCompleteness.missingMaterialNames.join('、') || '必填材料' }}
              </template>
            </div>
            <div v-if="currentApplication.materials?.length" class="material-detail-list">
              <div v-for="material in currentApplication.materials" :key="material.materialId" class="material-detail-card">
                <div class="material-main">
                  <div class="material-title">
                    <span>{{ material.materialName }}</span>
                    <el-tag :type="getPrecheckType(material.precheckStatus)" size="small">
                      {{ getPrecheckText(material.precheckStatus) }}
                    </el-tag>
                  </div>
                  <div class="material-meta">
                    <span>{{ material.fileName || '-' }}</span>
                    <span>{{ formatFileSize(material.fileSize) }}</span>
                    <span>{{ material.uploadTime || '-' }}</span>
                  </div>
                  <div v-if="material.precheckRemark" class="material-remark">
                    {{ material.precheckRemark }}
                  </div>
                </div>
                <div class="material-actions">
                  <el-button size="small" plain @click="previewMaterial(material)">
                    预览
                  </el-button>
                  <el-button size="small" plain @click="downloadMaterial(material)">
                    下载
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无提交材料" />
          </section>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getApplicationDetail,
  getApplicationList,
  getApplicationMaterialFileUrl,
  withdrawApplication,
  type ApplicationMaterialVO,
  type ApplicationVO
} from '@/api/application'

const router = useRouter()
const statusFilter = ref('')
const loading = ref(false)
const applications = ref<ApplicationVO[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentApplication = ref<ApplicationVO | null>(null)

const formRows = computed(() => {
  const data = parseFormData(currentApplication.value?.formData)
  return Object.entries(data)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => ({ key, value }))
})

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

async function openDetail(applicationId: number) {
  detailVisible.value = true
  detailLoading.value = true
  currentApplication.value = null
  try {
    currentApplication.value = await getApplicationDetail(applicationId)
  } catch {
    ElMessage.error('申请详情加载失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
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

function parseFormData(formData?: string) {
  if (!formData) {
    return {}
  }
  try {
    const parsed = JSON.parse(formData)
    return parsed && typeof parsed === 'object' ? parsed as Record<string, unknown> : {}
  } catch {
    return {}
  }
}

function formatFieldLabel(key: string) {
  const map: Record<string, string> = {
    applicantName: '申请人',
    applicant: '申请人',
    realName: '真实姓名',
    name: '姓名',
    idCard: '身份证号',
    phone: '联系电话',
    contactPhone: '联系电话',
    address: '居住地址',
    residenceAddress: '居住地址',
    residenceStartDate: '居住起始日期',
    residenceEndDate: '居住结束日期',
    residenceCondition: '申请条件',
    housingType: '居住情况',
    reason: '申请事由',
    remark: '备注'
  }
  return map[key] || key
}

function formatFieldValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value.map(formatFieldValue).join('、')
  }
  if (value && typeof value === 'object') {
    return Object.entries(value as Record<string, unknown>)
      .map(([key, val]) => `${formatFieldLabel(key)}：${formatFieldValue(val)}`)
      .join('；')
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  return value === undefined || value === null || value === '' ? '-' : String(value)
}

function getPrecheckType(status?: string) {
  const map: Record<string, string> = {
    passed: 'success',
    failed: 'danger',
    pending: 'warning'
  }
  return map[status || ''] || 'info'
}

function getPrecheckText(status?: string) {
  const map: Record<string, string> = {
    passed: '预审通过',
    failed: '预审不通过',
    pending: '待预审'
  }
  return map[status || ''] || '未知'
}

function formatFileSize(size?: number) {
  if (!size || size <= 0) {
    return '-'
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

async function fetchMaterialBlob(materialId: number) {
  const token = localStorage.getItem('token')
  const response = await fetch(getApplicationMaterialFileUrl(materialId), {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    throw new Error('文件读取失败')
  }
  return response.blob()
}

async function previewMaterial(material: ApplicationMaterialVO) {
  try {
    const blob = await fetchMaterialBlob(material.materialId)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60 * 1000)
  } catch {
    ElMessage.error('材料预览失败')
  }
}

async function downloadMaterial(material: ApplicationMaterialVO) {
  try {
    const blob = await fetchMaterialBlob(material.materialId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = material.fileName || `${material.materialName || '材料'}.${material.fileType || 'file'}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('材料下载失败')
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

.detail-content {
  min-height: 18rem;
}

.detail-section {
  margin-bottom: 1.5rem;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.75rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
  gap: 0.75rem 1rem;
}

.detail-item,
.form-data-row {
  display: flex;
  gap: 0.75rem;
  line-height: 1.6;
  color: var(--text-secondary);
  word-break: break-word;
}

.detail-item-wide {
  grid-column: 1 / -1;
}

.detail-label {
  width: 6rem;
  flex-shrink: 0;
  color: var(--text-muted);
}

.form-data-table {
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.form-data-row {
  padding: 0.75rem 1rem;
  border-bottom: 0.0625rem solid var(--border-color);
}

.form-data-row:last-child {
  border-bottom: 0;
}

.material-summary {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 0.75rem;
}

.material-detail-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.material-detail-card {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--card-bg);
}

.material-main {
  min-width: 0;
  flex: 1;
}

.material-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.material-meta {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  color: var(--text-muted);
  font-size: 0.875rem;
  line-height: 1.5;
}

.material-remark {
  margin-top: 0.5rem;
  color: var(--text-secondary);
  line-height: 1.6;
  word-break: break-word;
}

.material-actions {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
  flex-wrap: wrap;
}

@media (max-width: 48rem) {
  .material-detail-card,
  .detail-item,
  .form-data-row {
    flex-direction: column;
  }

  .detail-label {
    width: auto;
  }
}
</style>
