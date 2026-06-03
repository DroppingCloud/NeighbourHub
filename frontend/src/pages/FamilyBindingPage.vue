<template>
  <div class="page-container">
    <div class="page-header">
      <h2>家属代办</h2>
      <p v-if="isFamily">管理家属代办授权关系</p>
      <p v-else>管理他人代办授权</p>
    </div>

    <!-- 显示当前代办状态（仅家属） -->
    <div v-if="isFamily && proxyStore.currentTarget" class="current-proxy-banner">
      <span>当前正在为 <strong>{{ proxyStore.currentTarget.name }}</strong> 代办事务</span>
      <el-button size="small" @click="exitProxy">退出代办</el-button>
    </div>

    <!-- ========== 家属视角：已绑定列表 + 发起绑定 ========== -->
    <template v-if="isFamily">
      <div class="binding-list">
        <div v-for="binding in familyBindings" :key="binding.id" class="binding-card">
          <div class="binding-info">
            <div class="resident-name">
              {{ binding.targetProfileName || `用户 ${binding.targetProfileId}` }}
            </div>
            <div class="binding-detail">
              <el-tag size="small">{{ binding.relation || '亲属' }}</el-tag>
              <el-tag size="small" :type="statusTagType(binding.status)">
                {{ statusText(binding.status) }}
              </el-tag>
              <span class="bind-time">授权范围：{{ binding.authorizedActions || 'application,booking,notice' }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button v-if="binding.status === 'active'" type="primary" link @click="switchToProxy(binding)">
              切换
            </el-button>
            <el-button
              v-if="binding.status === 'active'"
              type="danger"
              link
              @click="revoke(binding.id)"
            >
              解绑
            </el-button>
          </div>
        </div>
        <el-empty v-if="!loading && familyBindings.length === 0" description="暂无家属代办关系" />
      </div>

      <div class="add-binding">
        <el-button type="primary" plain @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加家属绑定
        </el-button>
      </div>
    </template>

    <!-- ========== 居民视角：待确认的绑定请求 ========== -->
    <template v-else>
      <h3 class="section-title">待确认的绑定请求</h3>
      <div class="binding-list">
        <div v-for="req in pendingRequests" :key="req.id" class="binding-card">
          <div class="binding-info">
            <div class="resident-name">
              来自：{{ req.proxyUserName || `用户 ${req.proxyUserId}` }}
            </div>
            <div class="binding-detail">
              <el-tag size="small">{{ req.relation || '亲属' }}</el-tag>
              <el-tag size="small" type="warning">待确认</el-tag>
              <span class="bind-time">申请范围：{{ req.authorizedActions || 'application,booking,notice' }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="confirmRequest(req.id)">同意</el-button>
            <el-button type="danger" size="small" plain @click="rejectRequest(req.id)">拒绝</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && pendingRequests.length === 0" description="暂无待确认的绑定请求" />
      </div>

      <h3 class="section-title" style="margin-top: 2rem;">已授权的代办人</h3>
      <div class="binding-list">
        <div v-for="binding in myProxies" :key="binding.id" class="binding-card">
          <div class="binding-info">
            <div class="resident-name">
              {{ binding.proxyUserName || `用户 ${binding.proxyUserId}` }}
            </div>
            <div class="binding-detail">
              <el-tag size="small">{{ binding.relation || '亲属' }}</el-tag>
              <el-tag size="small" :type="statusTagType(binding.status)">
                {{ statusText(binding.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && myProxies.length === 0" description="暂无已授权的代办人" />
      </div>
    </template>

    <!-- 添加绑定对话框（仅家属） -->
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
          <el-input v-model="newBinding.authorizedActions" placeholder="application,booking,notice" />
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
  getProxyRelations,
  getPendingRequests,
  confirmProxy,
  rejectProxy,
  revokeProxyRelation,
  type ProxyRelationVO
} from '@/api/user'
import { useProxyStore } from '@/stores/proxy'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const proxyStore = useProxyStore()
const authStore = useAuthStore()

const isFamily = computed(() => {
  const role = authStore.userInfo?.role || ''
  return role === 'family' || role === 'ROLE_FAMILY'
})

const familyBindings = ref<ProxyRelationVO[]>([])
const pendingRequests = ref<ProxyRelationVO[]>([])
const myProxies = ref<ProxyRelationVO[]>([])
const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)

const newBinding = ref({
  realName: '',
  idCard: '',
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

function statusTagType(status: string) {
  const map: Record<string, string> = {
    active: 'success',
    pending: 'warning',
    revoked: 'info',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    if (isFamily.value) {
      familyBindings.value = await getProxyRelations()
      const activeBindings = familyBindings.value.filter(b => b.status === 'active')
      proxyStore.targets = activeBindings.map(b => ({
        id: b.id,
        profileId: b.targetProfileId!,
        targetUserId: b.targetUserId!,
        name: b.targetProfileName || `用户 ${b.targetProfileId}`,
        relation: b.relation || '家属',
        authorizedActions: b.authorizedActions || ''
      }))
    } else {
      // 居民：加载待确认请求和已授权代办人
      pendingRequests.value = await getPendingRequests()
      // 复用 pending-requests 接口无法拿到已生效列表，这里用 list 接口（居民视角看到的是空的，因为 list 只查 proxyUserId=当前用户）
      // 需要额外接口，暂时从 pendingRequests 中过滤
      myProxies.value = []
    }
  } finally {
    loading.value = false
  }
}

async function addBinding() {
  if (!newBinding.value.realName || !newBinding.value.idCard) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await applyProxy(newBinding.value)
    ElMessage.success('绑定申请已提交，等待对方确认')
    showAddDialog.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function revoke(id: number) {
  await ElMessageBox.confirm('确定解绑该家属代办授权吗？', '确认解绑', {
    type: 'warning'
  })
  await revokeProxyRelation(id)
  ElMessage.success('已解绑')
  await loadData()
}

async function confirmRequest(id: number) {
  await ElMessageBox.confirm('确定同意该家属绑定申请吗？', '确认同意', {
    type: 'info'
  })
  await confirmProxy(id)
  ElMessage.success('已同意绑定')
  await loadData()
}

async function rejectRequest(id: number) {
  await ElMessageBox.confirm('确定拒绝该家属绑定申请吗？', '确认拒绝', {
    type: 'warning'
  })
  await rejectProxy(id)
  ElMessage.success('已拒绝')
  await loadData()
}

async function switchToProxy(binding: ProxyRelationVO) {
  const target = {
    id: binding.id,
    profileId: binding.targetProfileId!,
    targetUserId: binding.targetUserId!,
    name: binding.targetProfileName || `用户 ${binding.targetProfileId}`,
    relation: binding.relation || '家属',
    authorizedActions: binding.authorizedActions || ''
  }
  proxyStore.setCurrentTarget(target)
  ElMessage.success(`已切换至为 ${target.name} 代办`)
  router.push('/')
}

function exitProxy() {
  proxyStore.setCurrentTarget(null)
  ElMessage.success('已退出代办模式，当前为本人办理')
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  max-width: 50rem;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 1.5rem;
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
.binding-info {
  flex: 1;
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
.card-actions {
  display: flex;
  gap: 0.5rem;
}
.add-binding {
  text-align: center;
  padding: 1.25rem;
}
</style>
