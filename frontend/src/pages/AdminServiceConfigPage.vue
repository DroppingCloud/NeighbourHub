<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>事项配置</h2>
      <el-button type="primary" @click="openCreate">新增事项</el-button>
    </div>

    <el-table v-loading="loading" :data="items" border>
      <el-table-column prop="itemId" label="ID" width="80" />
      <el-table-column prop="itemName" label="事项名称" />
      <el-table-column prop="itemCode" label="编码" width="160" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'online' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openMaterials(row)">材料</el-button>
          <el-button link type="danger" @click="remove(row.itemId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑事项' : '新增事项'" width="600px">
      <el-form :model="form" label-width="6rem">
        <el-form-item label="名称"><el-input v-model="form.itemName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.itemCode" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="online" value="online" />
            <el-option label="offline" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="条件"><el-input v-model="form.conditions" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="materialVisible" title="材料模板" width="700px">
      <div class="material-toolbar">
        <el-input v-model="materialForm.materialName" placeholder="材料名称" />
        <el-input v-model="materialForm.materialType" placeholder="类型编码" />
        <el-button type="primary" @click="addMaterial">添加</el-button>
      </div>
      <el-table :data="materials" border>
        <el-table-column prop="materialName" label="材料名称" />
        <el-table-column prop="materialType" label="类型编码" />
        <el-table-column prop="isRequired" label="必填" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeMaterial(row.templateId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: var(--text-primary);
}

.material-toolbar {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
</style>
