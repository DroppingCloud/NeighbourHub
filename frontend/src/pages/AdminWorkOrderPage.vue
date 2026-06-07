<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-info">
        <h2>工单管理</h2>
        <p>查看和管理所有事项办理工单</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterStatus" placeholder="全部状态" clearable class="filter-select" @change="loadWorkOrders">
        <el-option label="待审核" value="pending" />
        <el-option label="已通过" value="approved" />
        <el-option label="需补件" value="supplement_required" />
        <el-option label="已办结" value="completed" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索事项名称或申请人"
        clearable
        class="filter-input"
        @clear="loadWorkOrders"
        @keyup.enter="loadWorkOrders"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 工单表格 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="filteredOrders" stripe class="order-table">
        <el-table-column prop="orderId" label="工单ID" width="80" align="center" />
        <el-table-column prop="itemName" label="事项名称" min-width="160">
          <template #default="{ row }">
            {{ row.itemName || `申请 ${row.applicationId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="residentName" label="申请人" width="100" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-tag" :class="row.status">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="staffName" label="处理人" width="100">
          <template #default="{ row }">
            {{ row.staffName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <div class="action-group">
              <button class="tbl-action" @click="openDetail(row)">详情</button>
              <button class="tbl-action danger" @click="handleDelete(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="60rem">
      <template v-if="activeOrder">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">工单ID</span>
            <span>{{ activeOrder.orderId }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">事项名称</span>
            <span>{{ activeOrder.itemName || `申请 ${activeOrder.applicationId}` }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">申请人</span>
            <span>{{ activeOrder.residentName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">状态</span>
            <span class="status-tag" :class="activeOrder.status">{{ statusLabel(activeOrder.status) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">处理人</span>
            <span>{{ activeOrder.staffName || '未分配' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">创建时间</span>
            <span>{{ activeOrder.createTime }}</span>
          </div>
          <div class="detail-item detail-item-wide">
            <span class="detail-label">审核意见</span>
            <span>{{ activeOrder.auditOpinion || '暂无' }}</span>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteWorkOrder, getWorkOrderList, type WorkOrderVO } from '@/api/workOrder'

const loading = ref(false)
const filterStatus = ref('')
const keyword = ref('')
const workOrders = ref<WorkOrderVO[]>([])
const detailVisible = ref(false)
const activeOrder = ref<WorkOrderVO | null>(null)

const filteredOrders = computed(() => {
  if (!keyword.value) return workOrders.value
  const kw = keyword.value.toLowerCase()
  return workOrders.value.filter(o =>
    (o.itemName || '').toLowerCase().includes(kw) ||
    (o.residentName || '').toLowerCase().includes(kw)
  )
})

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    supplement_required: '需补件',
    completed: '已办结',
    rejected: '已驳回'
  }
  return map[status] || status
}

async function loadWorkOrders() {
  loading.value = true
  try {
    const params: { pageNum: number; pageSize: number; status?: string } = { pageNum: 1, pageSize: 200 }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }
    const page = await getWorkOrderList(params)
    workOrders.value = page.records || []
  } finally {
    loading.value = false
  }
}

function openDetail(order: WorkOrderVO) {
  activeOrder.value = order
  detailVisible.value = true
}

async function handleDelete(order: WorkOrderVO) {
  await ElMessageBox.confirm(
    `确定删除工单 #${order.orderId}（${order.itemName || ''}）？此操作不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  try {
    await deleteWorkOrder(order.orderId)
    ElMessage.success('工单已删除')
    await loadWorkOrders()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  loadWorkOrders()
})
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

/* Filter Bar */
.filter-bar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.filter-select {
  width: 10rem;
}

.filter-input {
  width: 16rem;
}

/* Table Card */
.table-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  box-shadow: var(--shadow-sm);
}

/* Status Tags */
.status-tag {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  border-radius: 1rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-tag.pending {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.status-tag.approved {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.status-tag.supplement_required {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.status-tag.completed {
  background: rgba(39, 174, 96, 0.1);
  color: #16a34a;
}

.status-tag.rejected {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

/* Actions */
.action-group {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
}

.tbl-action {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.8125rem;
  color: var(--ink-light);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.tbl-action:hover {
  background: rgba(26, 26, 46, 0.05);
  color: var(--ink);
}

.tbl-action.danger {
  color: var(--vermilion);
}

.tbl-action.danger:hover {
  background: rgba(192, 57, 43, 0.06);
}

/* Detail Grid */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-item-wide {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-weight: 500;
}

/* Table overrides */
:deep(.el-table th) {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
  font-weight: 600;
}

:deep(.el-table td) {
  color: var(--text-secondary) !important;
}

:deep(.el-table tr:hover td) {
  background: rgba(212, 168, 67, 0.04) !important;
}

@media (max-width: 48rem) {
  .filter-bar {
    flex-direction: column;
  }
  .filter-select,
  .filter-input {
    width: 100%;
  }
}
</style>
