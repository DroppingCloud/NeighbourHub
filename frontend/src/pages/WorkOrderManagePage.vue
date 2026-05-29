<template>
  <div class="page-container">
    <div class="page-header">
      <h2>工单处理</h2>
      <p>工作人员审核事项申请工单</p>
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
        </div>
        <div class="card-actions">
          <el-button v-if="order.status === 'pending'" type="primary" size="small" @click="audit(order.orderId, 'approved')">审核通过</el-button>
          <el-button v-if="order.status === 'pending'" type="warning" size="small" @click="audit(order.orderId, 'supplement_required')">要求补件</el-button>
          <el-button v-if="order.status === 'pending'" type="danger" size="small" @click="audit(order.orderId, 'rejected')">驳回</el-button>
          <el-button v-if="order.status === 'approved'" type="success" size="small" @click="audit(order.orderId, 'completed')">办结</el-button>
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

async function loadWorkOrders() {
  loading.value = true
  try {
    const page = await getWorkOrderList({
      status: activeTab.value,
      pageNum: 1,
      pageSize: 50
    })
    workOrders.value = page.records || []
  } finally {
    loading.value = false
  }
}

async function audit(orderId: number, action: AuditRequest['action']) {
  const opinion = action === 'approved'
    ? '材料齐全，审核通过'
    : action === 'supplement_required'
      ? '请补充必要材料'
      : action === 'completed'
        ? '事项已办结'
        : '不符合办理条件'

  await ElMessageBox.confirm('确定执行该工单操作吗？', '提示')
  await auditWorkOrder({ orderId, action, opinion })
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
}

.card-body {
  margin-bottom: 1rem;
}

.info-row {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.375rem;
}

.card-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}
</style>
