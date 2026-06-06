<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2>事项配置</h2>
        <p>管理政务服务事项、办理条件与材料模板</p>
      </div>
      <el-button class="btn-add" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增事项
      </el-button>
    </div>

    <!-- 事项列表 -->
    <div class="table-card">
      <el-table v-loading="loading" :data="items">
        <el-table-column prop="itemId" label="ID" width="64" />
        <el-table-column prop="itemName" label="事项名称" min-width="140">
          <template #default="{ row }">
            <span class="item-name">{{ row.itemName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemCode" label="编码" width="130">
          <template #default="{ row }">
            <code class="item-code">{{ row.itemCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" align="center">
          <template #default="{ row }">
            <span class="category-label">{{ row.category }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status">{{ row.status === 'online' ? '上线' : '下线' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <div class="action-group">
              <button class="tbl-action" @click="openEdit(row)">编辑</button>
              <button class="tbl-action" @click="openMaterials(row)">材料</button>
              <button class="tbl-action danger" @click="remove(row.itemId)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑事项' : '新增事项'" width="560px" class="admin-dialog" append-to-body>
      <el-form :model="form" label-position="top" class="dialog-form">
        <el-form-item label="名称">
          <el-input v-model="form.itemName" placeholder="事项名称" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.itemCode" placeholder="唯一编码，如 ITEM_005" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="证件/补贴/证明/服务" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="选择状态" style="width: 100%">
            <el-option label="上线" value="online" />
            <el-option label="下线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="事项简要说明" />
        </el-form-item>
        <el-form-item label="条件">
          <el-input v-model="form.conditions" type="textarea" :rows="2" placeholder="办理条件" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="formVisible = false">取消</el-button>
          <el-button class="btn-submit" @click="save">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 材料模板弹窗 -->
    <el-dialog v-model="materialVisible" title="材料模板" width="660px" class="admin-dialog" append-to-body>

      <div class="material-add-row">
        <el-input v-model="materialForm.materialName" placeholder="材料名称" class="mat-inp" />
        <el-input v-model="materialForm.materialType" placeholder="类型编码" class="mat-inp" />
        <el-button class="btn-submit btn-sm" @click="addMaterial">添加</el-button>
      </div>

      <div class="material-list" v-if="materials.length > 0">
        <div v-for="mat in materials" :key="mat.templateId" class="material-item">
          <div class="mat-info">
            <span class="mat-name">{{ mat.materialName }}</span>
            <code class="mat-type">{{ mat.materialType }}</code>
          </div>
          <div class="mat-meta">
            <span class="mat-required" :class="{ yes: mat.isRequired }">{{ mat.isRequired ? '必填' : '可选' }}</span>
            <button class="tbl-action danger" @click="removeMaterial(mat.templateId)">删除</button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无材料模板" :image-size="64" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createMaterialTemplate,
  createServiceItem,
  deleteMaterialTemplate,
  deleteServiceItem,
  getMaterialTemplates,
  getServiceItemList,
  updateServiceItem,
  type MaterialTemplateVO,
  type ServiceItemRequest,
  type ServiceItemVO
} from '@/api/serviceItem'

const loading = ref(false)
const items = ref<ServiceItemVO[]>([])
const formVisible = ref(false)
const materialVisible = ref(false)
const editingId = ref<number | null>(null)
const currentItemId = ref<number | null>(null)
const materials = ref<MaterialTemplateVO[]>([])

const form = ref<ServiceItemRequest>({
  itemName: '',
  itemCode: '',
  category: '',
  description: '',
  conditions: '',
  status: 'online'
})

const materialForm = ref({
  materialName: '',
  materialType: ''
})

async function loadItems() {
  loading.value = true
  try {
    const page = await getServiceItemList({ pageNum: 1, pageSize: 100 })
    items.value = page.records || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { itemName: '', itemCode: '', category: '', description: '', conditions: '', status: 'online' }
  formVisible.value = true
}

function openEdit(row: ServiceItemVO) {
  editingId.value = row.itemId
  form.value = {
    itemName: row.itemName,
    itemCode: row.itemCode,
    category: row.category,
    description: row.description,
    conditions: row.conditions,
    status: row.status
  }
  formVisible.value = true
}

async function save() {
  if (editingId.value) {
    await updateServiceItem(editingId.value, form.value)
    ElMessage.success('事项已更新')
  } else {
    await createServiceItem(form.value)
    ElMessage.success('事项已创建')
  }
  formVisible.value = false
  await loadItems()
}

async function remove(id: number) {
  await ElMessageBox.confirm('确定删除该事项吗？', '提示', { type: 'warning' })
  await deleteServiceItem(id)
  ElMessage.success('事项已删除')
  await loadItems()
}

async function openMaterials(row: ServiceItemVO) {
  currentItemId.value = row.itemId
  materialVisible.value = true
  materials.value = await getMaterialTemplates(row.itemId)
}

async function addMaterial() {
  if (!currentItemId.value || !materialForm.value.materialName) return
  await createMaterialTemplate({
    itemId: currentItemId.value,
    materialName: materialForm.value.materialName,
    materialType: materialForm.value.materialType,
    isRequired: 1,
    sortOrder: materials.value.length + 1
  })
  ElMessage.success('材料模板已添加')
  materials.value = await getMaterialTemplates(currentItemId.value)
  materialForm.value = { materialName: '', materialType: '' }
}

async function removeMaterial(id: number) {
  await deleteMaterialTemplate(id)
  ElMessage.success('材料模板已删除')
  if (currentItemId.value) materials.value = await getMaterialTemplates(currentItemId.value)
}

onMounted(loadItems)
</script>

<style scoped>
.admin-page {
  max-width: 75rem;
  margin: 0 auto;
}

/* ====== Header ====== */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 0.25rem;
}

.page-header p {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin: 0;
}

.btn-add {
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 0.5rem !important;
  padding: 0.5rem 1.125rem !important;
  font-weight: 600 !important;
  font-size: 0.8125rem !important;
  letter-spacing: 0.02em !important;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1) !important;
  box-shadow: 0 2px 6px rgba(26, 26, 46, 0.15) !important;
}

.btn-add:hover {
  transform: translateY(-2px) scale(1.02) !important;
  box-shadow: 0 6px 20px rgba(26, 26, 46, 0.22) !important;
}

.btn-add.btn-sm {
  padding: 0.375rem 0.875rem !important;
  font-size: 0.75rem !important;
}

/* ====== Table ====== */
.table-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

:deep(.el-table) {
  background: transparent !important;
  font-size: 0.8125rem !important;
}

:deep(.el-table th) {
  background: var(--bg-tertiary) !important;
  color: var(--text-muted) !important;
  font-weight: 500 !important;
  font-size: 0.75rem !important;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border-color) !important;
  padding: 0.625rem 0 !important;
}

:deep(.el-table td) {
  border-bottom-color: var(--border-color) !important;
  padding: 0.75rem 0 !important;
  vertical-align: middle !important;
}

:deep(.el-table .cell) {
  display: flex;
  align-items: center;
  min-height: 2.25rem;
}

:deep(.el-table th .cell) {
  min-height: auto;
}

:deep(.el-table tr) {
  background: transparent !important;
}

:deep(.el-table tr:hover td) {
  background: rgba(212, 168, 67, 0.02) !important;
}

.item-name {
  font-weight: 600;
  color: var(--text-primary);
}

.item-code {
  font-size: 0.6875rem;
  font-family: 'SF Mono', 'Fira Code', monospace;
  background: var(--bg-tertiary);
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  color: var(--text-secondary);
}

.category-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: rgba(212, 168, 67, 0.06);
  padding: 0.15rem 0.5rem;
  border-radius: 0.25rem;
}

.status-dot {
  font-size: 0.6875rem;
  font-weight: 600;
}

.status-dot.online {
  color: var(--jade);
}

.status-dot.offline {
  color: var(--text-muted);
}

/* Action buttons */
.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.tbl-action {
  background: none;
  border: none;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--gold);
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.tbl-action:hover {
  color: var(--ink);
}

.tbl-action.danger {
  color: var(--text-muted);
}

.tbl-action.danger:hover {
  color: var(--vermilion);
}

/* ====== Dialog System ====== */
:deep(.admin-dialog .el-dialog) {
  border-radius: 1rem !important;
  overflow: hidden;
  box-shadow:
    0 24px 80px rgba(26, 26, 46, 0.12),
    0 8px 24px rgba(26, 26, 46, 0.06),
    0 0 0 1px rgba(26, 26, 46, 0.04) !important;
}

:deep(.admin-dialog .el-dialog__header) {
  padding: 1rem 1.5rem !important;
  margin-right: 0 !important;
  border-bottom: 1px solid var(--border-color);
}

:deep(.admin-dialog .el-dialog__title) {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

:deep(.admin-dialog .el-dialog__headerbtn) {
  top: 1rem;
  right: 1.25rem;
}

:deep(.admin-dialog .el-dialog__body) {
  padding: 1.25rem 1.5rem 0.75rem !important;
}

:deep(.admin-dialog .el-dialog__footer) {
  padding: 0 !important;
}

.dialog-form {
  padding: 0;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 0.875rem !important;
}

.dialog-form :deep(.el-form-item__label) {
  font-size: 0.8125rem !important;
  color: var(--text-secondary) !important;
  font-weight: 500 !important;
  padding-bottom: 0.125rem !important;
  line-height: 1.4 !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.625rem;
  padding: 0.75rem 1.5rem 1.25rem;
}

.btn-cancel {
  background: transparent !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text-muted) !important;
  border-radius: 0.5rem !important;
  padding: 0.5rem 1.25rem !important;
  font-size: 0.8125rem !important;
  transition: all 0.2s !important;
}

.btn-cancel:hover {
  border-color: var(--text-secondary) !important;
  color: var(--text-primary) !important;
}

.btn-submit {
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 0.5rem !important;
  padding: 0.5rem 1.5rem !important;
  font-weight: 600 !important;
  font-size: 0.8125rem !important;
  letter-spacing: 0.03em !important;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1) !important;
  box-shadow: 0 2px 8px rgba(26, 26, 46, 0.15) !important;
}

.btn-submit:hover {
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 20px rgba(26, 26, 46, 0.22) !important;
}

.btn-submit.btn-sm {
  padding: 0.375rem 0.875rem !important;
  font-size: 0.75rem !important;
}

/* ====== Material Dialog ====== */
.material-add-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  align-items: center;
}

.mat-inp {
  flex: 1;
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.material-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  transition: border-color 0.2s;
}

.material-item:hover {
  border-color: var(--gold);
}

.mat-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.mat-name {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-primary);
}

.mat-type {
  font-size: 0.625rem;
  font-family: 'SF Mono', 'Fira Code', monospace;
  background: var(--bg-tertiary);
  padding: 0.1rem 0.3rem;
  border-radius: 0.2rem;
  color: var(--text-muted);
}

.mat-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.mat-required {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.mat-required.yes {
  color: var(--gold);
  font-weight: 600;
}

@media (max-width: 48rem) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .material-add-row {
    flex-direction: column;
  }
}
</style>
