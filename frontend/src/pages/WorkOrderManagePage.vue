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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditWorkOrder, getWorkOrderList, type AuditRequest, type WorkOrderVO } from '@/api/workOrder'

const activeTab = ref('pending')
//const activeTab = ref('')
const loading = ref(false)
const workOrders = ref<WorkOrderVO[]>([])

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
    const params = { pageNum: 1, pageSize: 50 }  // 注意 pageSize 改为 50
    if (activeTab.value) {
      params.status = activeTab.value
    }
    const page = await getWorkOrderList(params)
    workOrders.value = page.records || []
  } finally {
    loading.value = false
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
</style>
