<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
      <p>查看和编辑您的个人信息</p>
    </div>

    <section class="profile-card" v-loading="loading">
      <div class="avatar-block">
        <div class="avatar-wrap">
          <el-avatar :size="88" :icon="UserFilled" class="avatar" />
          <button class="camera-btn" type="button" aria-label="更换头像">
            <el-icon><Camera /></el-icon>
          </button>
        </div>
        <h3>{{ form.realName || '用户' }}</h3>
        <el-tag size="large">{{ roleText }}</el-tag>
      </div>

      <el-form ref="formRef" :model="form" label-width="6rem" class="profile-form">
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号码">
          <el-input v-model="form.phone" placeholder="手机号不可修改" disabled />
        </el-form-item>
        <el-form-item label="电子邮箱">
          <el-input v-model="form.email" placeholder="请输入电子邮箱" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="male">男</el-radio>
            <el-radio value="female">女</el-radio>
            <el-radio value="private">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="form.birthday"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="居住地址">
          <el-input v-model="form.address" placeholder="请输入居住地址" />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="saving" @click="saveProfile">保存修改</el-button>
          <el-button size="large" @click="resetForm">重置</el-button>
        </div>
      </el-form>
    </section>

    <section class="data-card">
      <h3>我的数据</h3>
      <div class="stat-grid">
        <div class="stat-item">
          <strong>{{ stats.applications }}</strong>
          <span>申请总数</span>
        </div>
        <div class="stat-item">
          <strong>{{ stats.bookings }}</strong>
          <span>预约总数</span>
        </div>
        <div class="stat-item">
          <strong>{{ stats.proxyRelations }}</strong>
          <span>家属绑定</span>
        </div>
        <div class="stat-item">
          <strong>{{ stats.completed }}</strong>
          <span>已完成</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, UserFilled } from '@element-plus/icons-vue'
import { getMe, updateMe, type UserInfoVO } from '@/api/auth'
import { getApplicationList } from '@/api/application'
import { getBookingList } from '@/api/booking'
import { getProxyRelations } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const original = ref<UserInfoVO | null>(null)

const form = reactive({
  realName: '',
  phone: '',
  email: '',
  gender: 'private',
  birthday: '',
  address: ''
})

const stats = reactive({
  applications: 0,
  bookings: 0,
  proxyRelations: 0,
  completed: 0
})

const role = computed(() => normalizeRole(authStore.userInfo?.role || 'resident'))
const roleText = computed(() => {
  const map: Record<string, string> = {
    resident: '居民用户',
    family: '家属代办',
    staff: '工作人员',
    admin: '管理员'
  }
  return map[role.value] || '居民用户'
})

onMounted(async () => {
  loading.value = true
  try {
    const me = await getMe()
    original.value = me
    fillForm(me)
    await loadStats()
  } finally {
    loading.value = false
  }
})

function fillForm(me: UserInfoVO) {
  form.realName = me.realName || ''
  form.phone = me.phone || ''
  form.email = me.email || ''
  form.gender = me.gender || 'private'
  form.birthday = me.birthday || ''
  form.address = me.address || ''
}

async function loadStats() {
  const [appPage, bookingPage, proxies] = await Promise.all([
    getApplicationList({ pageNum: 1, pageSize: 100 }).catch(() => null),
    getBookingList(1, 100).catch(() => null),
    getProxyRelations().catch(() => [])
  ])
  const applications = pageRows<any>(appPage)
  const bookings = pageRows<any>(bookingPage)
  stats.applications = pageTotal(appPage, applications.length)
  stats.bookings = pageTotal(bookingPage, bookings.length)
  stats.proxyRelations = Array.isArray(proxies) ? proxies.filter(item => item.status === 'active').length : 0
  stats.completed = applications.filter(item => item.status === 'completed').length +
    bookings.filter(item => item.status === 'completed').length
}

async function saveProfile() {
  saving.value = true
  try {
    await updateMe({
      realName: form.realName,
      email: form.email,
      gender: form.gender,
      birthday: form.birthday,
      address: form.address
    })
    authStore.setUserInfo(authStore.userInfo ? { ...authStore.userInfo, realName: form.realName } : authStore.userInfo)
    original.value = await getMe()
    fillForm(original.value)
    ElMessage.success('个人信息已保存')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  if (original.value) {
    fillForm(original.value)
  }
}

function pageRows<T>(page: any): T[] {
  if (Array.isArray(page)) return page
  return page?.records || page?.list || page?.rows || []
}

function pageTotal(page: any, fallback: number) {
  return Number(page?.total ?? fallback)
}

function normalizeRole(value: string) {
  return value.replace(/^ROLE_/, '').toLowerCase()
}
</script>

<style scoped>
.profile-page {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  color: var(--text-muted);
}

.profile-card,
.data-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 2.25rem 2rem;
  margin-bottom: 2rem;
}

.avatar-block {
  text-align: center;
  margin-bottom: 2rem;
}

.avatar-wrap {
  position: relative;
  display: inline-flex;
  margin-bottom: 1rem;
}

.avatar {
  background: #bfc3cb;
  color: #fff;
}

.camera-btn {
  position: absolute;
  right: -0.25rem;
  bottom: 0;
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  background: var(--gold);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.avatar-block h3 {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.profile-form {
  max-width: 58rem;
  margin: 0 auto;
}

.form-actions {
  display: flex;
  gap: 1rem;
  padding-left: 6rem;
  margin-top: 1.5rem;
}

.data-card h3 {
  font-size: 1.125rem;
  color: var(--text-primary);
  margin-bottom: 1.5rem;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(12rem, 100%), 1fr));
  gap: 1.25rem;
}

.stat-item {
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  text-align: center;
}

.stat-item strong {
  display: block;
  font-size: 1.875rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.stat-item span {
  color: var(--text-muted);
  font-size: 0.875rem;
}

:deep(.el-input__wrapper) {
  min-height: 3rem;
  background: var(--bg-tertiary);
}

@media (max-width: 48rem) {
  .profile-card,
  .data-card {
    padding: 1.5rem 1rem;
  }

  .form-actions {
    padding-left: 0;
    flex-wrap: wrap;
  }
}
</style>
