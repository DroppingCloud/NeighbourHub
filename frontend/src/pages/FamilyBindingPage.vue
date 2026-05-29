<template>
  <div class="page-container">
    <div class="page-header">
      <h2>家属代办</h2>
      <p>管理家属代办授权关系</p>
    </div>

    <div class="binding-list">
      <div v-for="binding in familyBindings" :key="binding.id" class="binding-card">
        <div class="binding-info">
          <div class="resident-name">
            {{ binding.targetUserId ? `用户 ${binding.targetUserId}` : `档案 ${binding.targetProfileId}` }}
          </div>
          <div class="binding-detail">
            <el-tag size="small">{{ binding.relation || '亲属' }}</el-tag>
            <el-tag size="small" :type="binding.status === 'active' ? 'success' : 'info'">
              {{ statusText(binding.status) }}
            </el-tag>
            <span class="bind-time">授权范围：{{ binding.authorizedActions || 'application,booking,notice' }}</span>
          </div>
        </div>
        <el-button
          v-if="binding.status === 'active'"
          type="danger"
          link
          @click="revoke(binding.id)"
        >
          撤销
        </el-button>
      </div>
      <el-empty v-if="!loading && familyBindings.length === 0" description="暂无家属代办关系" />
    </div>

    <div class="add-binding">
      <el-button type="primary" plain @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加家属绑定
      </el-button>
    </div>

    <el-dialog v-model="showAddDialog" title="添加家属绑定" width="500px">
      <el-form :model="newBinding" label-width="7rem" label-position="right">
        <el-form-item label="被代理用户ID">
          <el-input-number v-model="newBinding.targetUserId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="亲属关系">
          <el-select v-model="newBinding.relation" placeholder="请选择" style="width: 100%">
            <el-option label="子女" value="子女" />
            <el-option label="配偶" value="配偶" />
            <el-option label="父母" value="父母" />
            <el-option label="兄弟姐妹" value="兄弟姐妹" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权范围">
          <el-input v-model="newBinding.authorizedActions" placeholder="application,booking,notice" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="addBinding">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  bindProxy,
  getProxyRelations,
  revokeProxyRelation,
  type ProxyRelationVO
} from '@/api/user'

const familyBindings = ref<ProxyRelationVO[]>([])
const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)

const newBinding = ref({
  targetUserId: 3,
  relation: '子女',
  authorizedActions: 'application,booking,notice'
})

function statusText(status: string) {
  const map: Record<string, string> = {
    active: '已生效',
    pending: '待确认',
    revoked: '已撤销',
    rejected: '已拒绝'
  }
  return map[status] || status
}

async function loadBindings() {
  loading.value = true
  try {
    familyBindings.value = await getProxyRelations()
  } finally {
    loading.value = false
  }
}

async function addBinding() {
  if (!newBinding.value.targetUserId || !newBinding.value.relation) {
    ElMessage.warning('请填写被代理用户ID和亲属关系')
    return
  }
  submitting.value = true
  try {
    await bindProxy(newBinding.value)
    ElMessage.success('家属绑定已创建')
    showAddDialog.value = false
    await loadBindings()
  } finally {
    submitting.value = false
  }
}

async function revoke(id: number) {
  await ElMessageBox.confirm('确定撤销该家属代办授权吗？', '确认撤销', {
    type: 'warning'
  })
  await revokeProxyRelation(id)
  ElMessage.success('已撤销授权')
  await loadBindings()
}

onMounted(loadBindings)
</script>

<style scoped>
.page-container {
  max-width: 50rem;
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

.binding-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.binding-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
}

.resident-name {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.375rem;
  color: var(--text-primary);
}

.binding-detail {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.bind-time {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.add-binding {
  text-align: center;
  padding: 1.25rem;
}
</style>
