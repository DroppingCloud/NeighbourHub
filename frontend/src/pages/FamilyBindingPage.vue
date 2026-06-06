<template>
  <div class="page-container">
    <div class="page-header">
      <h2>家属代办</h2>
      <p>管理家属代办授权关系</p>
    </div>

    <div v-if="proxyStore.currentTarget" class="current-proxy-banner">
      <span>当前正在为 <strong>{{ proxyStore.currentTarget.name }}</strong> 代办事务</span>
      <el-button size="small" @click="exitProxy">退出代办</el-button>
    </div>

    <el-tabs v-model="activeTab" class="proxy-tabs">
      <el-tab-pane label="我的家属绑定" name="bindings">
        <div class="binding-list">
          <div v-for="binding in familyBindings" :key="binding.id" class="binding-card">
            <div class="binding-info">
              <div class="resident-name">{{ bindingDisplayName(binding) }}</div>
              <div class="binding-detail">
                <el-tag size="small">{{ binding.relation || '亲属' }}</el-tag>
                <el-tag size="small" :type="binding.status === 'active' ? 'success' : 'info'">
                  {{ statusText(binding.status) }}
                </el-tag>
                <span class="bind-time">授权范围：{{ binding.authorizedActions || 'apply,booking,query,notice' }}</span>
              </div>
            </div>
            <div class="card-actions">
              <el-button v-if="canSwitchToProxy(binding)" type="primary" link @click="switchToProxy(binding)">
                切换
              </el-button>
              <el-button v-if="binding.status === 'active'" type="danger" link @click="revoke(binding.id)">
                解绑
              </el-button>
            </div>
          </div>
          <el-empty v-if="!loadingBindings && familyBindings.length === 0" description="暂无家属绑定关系" />
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="isResident" label="待确认的申请" name="requests">
        <div class="request-list">
          <div v-for="req in pendingRequests" :key="req.id" class="request-card">
            <div class="request-info">
              <span class="request-user">申请人：{{ req.proxyUserName || `用户 ${req.proxyUserId}` }}</span>
              <span class="request-relation">关系：{{ req.relation || '亲属' }}</span>
              <span class="request-actions">授权范围：{{ req.authorizedActions || 'apply,booking,query,notice' }}</span>
            </div>
            <div class="request-buttons">
              <el-button type="primary" size="small" @click="confirm(req.id)">同意</el-button>
              <el-button type="danger" size="small" @click="reject(req.id)">拒绝</el-button>
            </div>
          </div>
          <el-empty v-if="!loadingRequests && pendingRequests.length === 0" description="暂无待确认申请" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <div v-if="activeTab === 'bindings' && isFamily" class="add-binding">
      <el-button type="primary" plain @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加家属绑定
      </el-button>
    </div>
    <el-alert
      v-else-if="activeTab === 'bindings' && isResident"
      class="role-tip"
      title="居民用户无需发起绑定申请，请等待家属用户提交绑定申请后在“待确认的申请”中处理。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-dialog v-model="showAddDialog" title="添加家属绑定" width="500px">
      <el-form :model="newBinding" label-width="7rem">
        <el-form-item label="真实姓名" required>
          <el-input v-model="newBinding.realName" placeholder="被代理人的姓名" />
        </el-form-item>
        <el-form-item label="身份证号" required>
          <el-input v-model="newBinding.idCard" placeholder="被代理人的身份证号" />
        </el-form-item>
        <el-form-item label="亲属关系">
          <el-select v-model="newBinding.relation">
            <el-option label="子女" value="子女" />
            <el-option label="配偶" value="配偶" />
            <el-option label="父母" value="父母" />
            <el-option label="兄弟姐妹" value="兄弟姐妹" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权范围">
          <el-input v-model="newBinding.authorizedActions" placeholder="apply,booking,query,notice" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="addBinding">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  applyProxy,
  confirmProxy,
  getPendingRequests,
  getProxyRelations,
  rejectProxy,
  revokeProxyRelation,
  type ProxyRelationVO
} from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useProxyStore } from '@/stores/proxy'

const router = useRouter()
const proxyStore = useProxyStore()
const authStore = useAuthStore()

const currentRole = computed(() => normalizeRole(authStore.userInfo?.role || ''))
const currentUserId = computed(() => Number(authStore.userInfo?.userId || 0))
const isFamily = computed(() => currentRole.value === 'family')
const isResident = computed(() => currentRole.value === 'resident')

const activeTab = ref('bindings')
const familyBindings = ref<ProxyRelationVO[]>([])
const pendingRequests = ref<ProxyRelationVO[]>([])
const loadingBindings = ref(false)
const loadingRequests = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)

const newBinding = ref({
  realName: '',
  idCard: '',
  relation: '子女',
  authorizedActions: 'apply,booking,query,notice'
})

function normalizeRole(role: string) {
  return role.replace(/^ROLE_/, '').toLowerCase()
}

function statusText(status: string) {
  const map: Record<string, string> = {
    active: '已生效',
    pending: '待确认',
    revoked: '已撤销',
    rejected: '已拒绝'
  }
  return map[status] || status
}

function bindingDisplayName(binding: ProxyRelationVO) {
  if (Number(binding.proxyUserId) === currentUserId.value) {
    return binding.targetProfileName || `居民 ${binding.targetUserId || binding.targetProfileId || ''}`
  }
  return binding.proxyUserName || `家属 ${binding.proxyUserId}`
}

function canSwitchToProxy(binding: ProxyRelationVO) {
  return binding.status === 'active' && isFamily.value && Number(binding.proxyUserId) === currentUserId.value
}

function toProxyTarget(binding: ProxyRelationVO) {
  return {
    id: binding.id,
    profileId: binding.targetProfileId!,
    targetUserId: binding.targetUserId!,
    name: binding.targetProfileName || `档案 ${binding.targetProfileId}`,
    relation: binding.relation || '亲属',
    authorizedActions: binding.authorizedActions || ''
  }
}

async function loadBindings() {
  loadingBindings.value = true
  try {
    familyBindings.value = await getProxyRelations()
    await proxyStore.loadTargets()
  } catch (error: any) {
    ElMessage.error(error.message || '加载绑定关系失败')
  } finally {
    loadingBindings.value = false
  }
}

async function loadRequests() {
  if (!isResident.value) {
    pendingRequests.value = []
    return
  }
  loadingRequests.value = true
  try {
    pendingRequests.value = await getPendingRequests()
  } catch (error: any) {
    ElMessage.error(error.message || '加载待确认申请失败')
  } finally {
    loadingRequests.value = false
  }
}

async function addBinding() {
  if (!isFamily.value) {
    ElMessage.warning('只有家属用户可以发起绑定申请')
    return
  }
  if (!newBinding.value.realName || !newBinding.value.idCard) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await applyProxy(newBinding.value)
    ElMessage.success('绑定申请已提交，等待对方确认')
    showAddDialog.value = false
    newBinding.value = {
      realName: '',
      idCard: '',
      relation: '子女',
      authorizedActions: 'apply,booking,query,notice'
    }
    await loadBindings()
  } catch (error: any) {
    ElMessage.error(error.message || '提交申请失败')
  } finally {
    submitting.value = false
  }
}

async function revoke(id: number) {
  await ElMessageBox.confirm('确定解除该家属绑定关系吗？', '确认解绑', {
    type: 'warning',
    confirmButtonText: '解绑',
    cancelButtonText: '取消'
  })
  try {
    await revokeProxyRelation(id)
    ElMessage.success('已解除绑定')
    if (proxyStore.currentTarget?.id === id) {
      proxyStore.setCurrentTarget(null)
      ElMessage.info('已退出代办模式')
    }
    await loadBindings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '解绑失败')
    }
  }
}

async function confirm(id: number) {
  await ElMessageBox.confirm('同意该家属绑定申请吗？', '确认同意', {
    type: 'info',
    confirmButtonText: '同意',
    cancelButtonText: '取消'
  })
  try {
    await confirmProxy(id)
    ElMessage.success('已同意绑定')
    await loadRequests()
    await loadBindings()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

async function reject(id: number) {
  await ElMessageBox.confirm('拒绝该家属绑定申请吗？', '确认拒绝', {
    type: 'warning',
    confirmButtonText: '拒绝',
    cancelButtonText: '取消'
  })
  try {
    await rejectProxy(id)
    ElMessage.success('已拒绝绑定')
    await loadRequests()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

function switchToProxy(binding: ProxyRelationVO) {
  const target = toProxyTarget(binding)
  proxyStore.setCurrentTarget(target)
  ElMessage.success(`已切换至为 ${target.name} 代办`)
  router.push('/')
}

function exitProxy() {
  proxyStore.setCurrentTarget(null)
  ElMessage.success('已退出代办模式，当前为本人办理')
  router.push('/')
}

onMounted(() => {
  loadBindings()
  loadRequests()
})
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
}

.current-proxy-banner {
  background: var(--bg-tertiary);
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  margin-bottom: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.proxy-tabs {
  margin-bottom: 1.5rem;
}

.binding-list,
.request-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.binding-card,
.request-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
}

.resident-name {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.375rem;
  color: var(--text-primary);
}

.binding-detail,
.request-info {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.bind-time {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.request-user,
.request-relation,
.request-actions {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.request-buttons,
.card-actions {
  display: flex;
  gap: 0.5rem;
}

.add-binding {
  text-align: center;
  padding: 1.25rem;
}

.role-tip {
  margin-top: 1rem;
}
</style>
