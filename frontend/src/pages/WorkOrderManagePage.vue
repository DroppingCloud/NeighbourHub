<template>
  <div class="page-container">
    <div class="page-header">
      <h2>工单处理</h2>
      <p>工作人员审核事项申请工单，并核对必填材料是否齐全</p>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadWorkOrders">
      <el-tab-pane label="待处理" name="pending" />
      <el-tab-pane label="已通过" name="approved" />
      <el-tab-pane label="需补件" name="supplement_required" />
      <el-tab-pane label="已完成" name="completed" />
    </el-tabs>

    <div v-loading="loading" class="work-list">
      <div v-for="order in workOrders" :key="order.orderId" class="work-card">
        <div class="card-header">
          <span class="title">{{ order.itemName || `申请 ${order.applicationId}` }}</span>
          <el-tag :type="getStatusType(order.status)">{{ order.statusLabel || order.status }}</el-tag>
        </div>

        <div class="card-body">
          <div class="info-row">申请人：{{ order.residentName || '-' }}</div>
          <div class="info-row">提交时间：{{ order.createTime }}</div>
          <div class="info-row">审核意见：{{ order.auditOpinion || '暂无' }}</div>

          <div class="material-row">
            <span>材料完整性：</span>
            <el-tag :type="order.materialCompleteness?.complete ? 'success' : 'warning'">
              {{ materialSummary(order) }}
            </el-tag>
          </div>

          <div
            v-if="order.materialCompleteness && !order.materialCompleteness.complete"
            class="missing-materials"
          >
            缺少材料：{{ order.materialCompleteness.missingMaterialNames.join('、') || '未上传必填材料' }}
          </div>
        </div>

        <div class="card-actions">
          <el-button
            size="small"
            plain
            @click="openDetailDialog(order)"
          >
            查看详情
          </el-button>
          <el-button
            v-if="order.status === 'pending'"
            type="primary"
            size="small"
            :disabled="!order.materialCompleteness?.complete"
            @click="audit(order, 'approved')"
          >
            审核通过
          </el-button>
          <el-button
            v-if="order.status === 'pending'"
            type="warning"
            size="small"
            @click="audit(order, 'supplement_required')"
          >
            要求补件
          </el-button>
          <el-button
            v-if="order.status === 'pending'"
            type="danger"
            size="small"
            @click="audit(order, 'rejected')"
          >
            驳回
          </el-button>
          <el-button
            v-if="order.status === 'approved'"
            type="success"
            size="small"
            :disabled="!order.materialCompleteness?.complete"
            @click="audit(order, 'completed')"
          >
            办结
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && workOrders.length === 0" description="暂无工单" />
    </div>

    <el-dialog v-model="detailDialogVisible" title="申请详情" width="72rem">
      <template v-if="activeOrder">
        <section class="detail-section">
          <div class="detail-section-title">基本信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">事项名称</span>
              <span>{{ activeOrder.itemName || `申请 ${activeOrder.applicationId}` }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">事项分类</span>
              <span>{{ activeOrder.category || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请人</span>
              <span>{{ activeOrder.residentName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请状态</span>
              <span>{{ activeOrder.applicationStatusLabel || activeOrder.applicationStatus || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">工单状态</span>
              <span>{{ activeOrder.statusLabel || activeOrder.status }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">提交时间</span>
              <span>{{ activeOrder.submitTime || activeOrder.createTime || '-' }}</span>
            </div>
            <div class="detail-item detail-item-wide">
              <span class="detail-label">办理备注</span>
              <span>{{ cleanText(activeOrder.remark) }}</span>
            </div>
            <div class="detail-item detail-item-wide">
              <span class="detail-label">审核意见</span>
              <span>{{ cleanText(activeOrder.auditOpinion) }}</span>
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

          <el-empty
            v-if="!activeOrder?.materials?.length"
            description="该申请暂无已提交材料"
          />

          <div v-else class="submitted-material-list">
            <div
              v-for="material in activeOrder.materials"
              :key="material.materialId"
              class="submitted-material-card"
            >
              <div class="submitted-material-main">
                <div class="submitted-material-name">{{ material.materialName }}</div>
                <div class="submitted-material-meta">
                  {{ material.fileName || '未命名文件' }}
                  <span v-if="material.fileSize"> · {{ formatFileSize(Number(material.fileSize)) }}</span>
                  <span v-if="material.precheckStatus"> · {{ precheckLabel(material.precheckStatus) }}</span>
                </div>
                <div v-if="material.precheckRemark" class="submitted-material-remark">
                  预审备注：{{ material.precheckRemark }}
                </div>
              </div>
              <div class="submitted-material-actions">
                <el-button size="small" type="primary" link @click="previewMaterial(material.materialId)">
                  预览
                </el-button>
                <el-button size="small" type="primary" link @click="downloadMaterial(material)">
                  下载
                </el-button>
              </div>
            </div>
          </div>
        </section>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditWorkOrder, getWorkOrderList, type AuditRequest, type WorkOrderVO } from '@/api/workOrder'
import { getApplicationMaterialFileUrl, type ApplicationMaterialVO } from '@/api/application'

const activeTab = ref('pending')
//const activeTab = ref('')
const loading = ref(false)
const workOrders = ref<WorkOrderVO[]>([])
const detailDialogVisible = ref(false)
const activeOrder = ref<WorkOrderVO | null>(null)

const formRows = computed(() => {
  const data = parseFormData(activeOrder.value?.formData)
  return Object.entries(data)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => ({ key, value }))
})

function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'primary',
    supplement_required: 'danger',
    completed: 'success',
    rejected: 'info'
  }
  return map[status] || 'info'
}

function materialSummary(order: WorkOrderVO) {
  const completeness = order.materialCompleteness
  if (!completeness) return '未校验'
  return `${completeness.uploadedRequiredCount}/${completeness.requiredCount} 已上传`
}


async function loadWorkOrders() {
  loading.value = true
  try {
    const params: { pageNum: number; pageSize: number; status?: string } = { pageNum: 1, pageSize: 50 }
    if (activeTab.value) {
      params.status = activeTab.value
    }
    const page = await getWorkOrderList(params)
    workOrders.value = page.records || []
  } finally {
    loading.value = false
  }
}

function openDetailDialog(order: WorkOrderVO) {
  activeOrder.value = order
  detailDialogVisible.value = true
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

function cleanText(value?: string) {
  if (!value || /^\?+$/.test(value)) {
    return '暂无'
  }
  return value
}

function precheckLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待预审',
    passed: '预审通过',
    failed: '预审未通过'
  }
  return map[status] || status
}

function formatFileSize(size: number) {
  if (!Number.isFinite(size) || size <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex++
  }
  return `${value.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

async function previewMaterial(materialId: number) {
  const blob = await fetchMaterialBlob(materialId)
  if (!blob) return
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank')
  setTimeout(() => URL.revokeObjectURL(url), 5000)
}

async function downloadMaterial(material: ApplicationMaterialVO) {
  const blob = await fetchMaterialBlob(material.materialId)
  if (!blob) return
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = material.fileName || `${material.materialName || '材料'}.${material.fileType || 'dat'}`
  link.click()
  URL.revokeObjectURL(link.href)
}

async function fetchMaterialBlob(materialId: number) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.error('登录已过期，请重新登录')
    return null
  }
  try {
    const response = await fetch(getApplicationMaterialFileUrl(materialId), {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    if (!response.ok) {
      ElMessage.error(response.status === 401 ? '登录已过期，请重新登录' : '材料文件读取失败')
      return null
    }
    return await response.blob()
  } catch {
    ElMessage.error('材料文件读取失败')
    return null
  }
}

async function audit(order: WorkOrderVO, action: AuditRequest['action']) {
  if ((action === 'approved' || action === 'completed') && !order.materialCompleteness?.complete) {
    ElMessage.warning('必填材料不完整，不能审核通过或办结')
    return
  }

  const opinion = action === 'approved'
    ? '材料齐全，审核通过'
    : action === 'supplement_required'
      ? `请补充必填材料：${order.materialCompleteness?.missingMaterialNames.join('、') || '待补充材料'}`
      : action === 'completed'
        ? '事项已办结'
        : '不符合办理条件'

  await ElMessageBox.confirm('确定执行该工单操作吗？', '提示')
  await auditWorkOrder({ orderId: order.orderId, action, opinion })
  ElMessage.success('工单已更新')
  await loadWorkOrders()
}

onMounted(loadWorkOrders)
</script>

<style scoped>
.page-container {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.75rem;
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

.work-list {
  min-height: 12rem;
}

.work-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
  box-shadow: var(--shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-word;
}

.card-body {
  margin-bottom: 1rem;
}

.info-row,
.material-row {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.375rem;
  line-height: 1.6;
}

.material-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.missing-materials {
  margin-top: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  background: rgba(230, 162, 60, 0.12);
  color: #8a5a00;
  font-size: 0.875rem;
  line-height: 1.6;
  word-break: break-word;
}

.card-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  flex-wrap: wrap;
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

.submitted-material-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.submitted-material-card {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
}

.submitted-material-main {
  min-width: 0;
}

.submitted-material-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.submitted-material-meta,
.submitted-material-remark {
  font-size: 0.8125rem;
  color: var(--text-muted);
  line-height: 1.5;
  word-break: break-word;
}

.submitted-material-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 0 0 auto;
}

@media (max-width: 48rem) {
  .submitted-material-card,
  .detail-item,
  .form-data-row {
    flex-direction: column;
  }

  .detail-label {
    width: auto;
  }
}
</style>
