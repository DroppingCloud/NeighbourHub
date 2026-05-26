<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>事项配置管理</span>
        <el-button type="primary" @click="showDialog(null)">新增事项</el-button>
      </div>
    </template>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="itemCode" label="事项编码" width="120" />
      <el-table-column prop="itemName" label="事项名称" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'online' ? 'success' : 'info'">{{ row.status === 'online' ? '上线' : '下线' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除此事项？" @confirm="handleDelete(row.itemId)">
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editItem ? '编辑事项' : '新增事项'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="事项编码"><el-input v-model="form.itemCode" /></el-form-item>
        <el-form-item label="事项名称"><el-input v-model="form.itemName" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category">
            <el-option label="证明" value="证明" />
            <el-option label="补贴" value="补贴" />
            <el-option label="证件" value="证件" />
            <el-option label="服务" value="服务" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="online">上线</el-radio>
            <el-radio value="offline">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getServiceItemList, createServiceItem, updateServiceItem, deleteServiceItem } from '@/api/serviceItem'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const list = ref<any[]>([])
const editItem = ref<any>(null)
const form = ref<any>({ itemCode: '', itemName: '', category: '证明', description: '', status: 'online' })

const showDialog = (item: any) => {
  editItem.value = item
  form.value = item ? { ...item } : { itemCode: '', itemName: '', category: '证明', description: '', status: 'online' }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    if (editItem.value) { await updateServiceItem(editItem.value.itemId, form.value) }
    else { await createServiceItem(form.value) }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

const handleDelete = async (id: number) => {
  await deleteServiceItem(id)
  ElMessage.success('删除成功')
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getServiceItemList({ pageSize: 100 })
    list.value = res.records || []
  } finally { loading.value = false }
}

onMounted(loadData)
</script>
