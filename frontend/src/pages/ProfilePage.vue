<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
      <p>查看和编辑您的个人信息</p>
    </div>

    <section class="profile-card" v-loading="loading">
      <div class="avatar-block">
        <div class="avatar-wrap">
          <el-avatar :size="88" :src="avatarSrc" :icon="!avatarSrc ? UserFilled : undefined" class="avatar" />
          <button class="camera-btn" type="button" aria-label="更换头像" @click.prevent="pickAvatar">
            <el-icon><Camera /></el-icon>
          </button>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
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
          <el-button size="large" plain type="warning" @click="clearForm">清空</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, UserFilled } from '@element-plus/icons-vue'
import { getMe, updateMe, type UserInfoVO } from '@/api/auth'
import request from '@/utils/request'
import { getApplicationList } from '@/api/application'
import { getBookingList } from '@/api/booking'
import { getProxyRelations } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
// 保存最新成功保存的数据（作为重置的基准）
const savedData = ref<UserInfoVO | null>(null)

// 默认空表单值
const getEmptyForm = () => ({
  realName: '',
  phone: '',
  email: '',
  gender: 'private',
  birthday: '',
  address: ''
})

const form = reactive(getEmptyForm())

const avatarSrc = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
// 保存当前预览的 objectURL，用于清理
let currentObjectUrl: string | null = null

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
    // 保存最新成功加载的数据
    savedData.value = { ...me }
    fillForm(me)
    updateAvatarDisplay(me)
    await loadStats()
  } finally {
    loading.value = false
  }
})

// 更新头像显示
function updateAvatarDisplay(me: UserInfoVO) {
  if (me.avatar) {
    const base = import.meta.env.VITE_API_BASE_URL ?? ''
    avatarSrc.value = (base || '') + `/api/auth/avatar/${me.userId}?t=${Date.now()}`
  } else {
    avatarSrc.value = ''
  }
}

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
  stats.proxyRelations = Array.isArray(proxies) ? proxies.filter((item: any) => item.status === 'active').length : 0
  stats.completed = applications.filter((item: any) => item.status === 'completed').length +
    bookings.filter((item: any) => item.status === 'completed').length
}

async function saveProfile() {
  // 校验出生日期
  if (form.birthday) {
    const selected = new Date(form.birthday)
    const today = new Date()
    selected.setHours(0, 0, 0, 0)
    today.setHours(0, 0, 0, 0)
    if (selected > today) {
      ElMessage.error('出生日期不能晚于今天')
      return
    }
  }

  saving.value = true
  try {
    await updateMe({
      realName: form.realName,
      email: form.email,
      gender: form.gender,
      birthday: form.birthday,
      address: form.address
    })
    
    // 刷新用户信息
    const refreshed = await getMe()
    savedData.value = { ...refreshed }
    fillForm(refreshed)
    updateAvatarDisplay(refreshed)
    
    // 清除临时 objectURL
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl)
      currentObjectUrl = null
    }
    // 清空文件选择
    if (fileInput.value) fileInput.value.value = ''
    
    // 更新 store 中的用户信息
    authStore.setUserInfo(authStore.userInfo ? { ...authStore.userInfo, realName: form.realName, avatar: avatarSrc.value } : authStore.userInfo)
    
    ElMessage.success('个人信息已保存')
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 重置：恢复到上次保存的状态
function resetForm() {
  if (savedData.value) {
    fillForm(savedData.value)
    updateAvatarDisplay(savedData.value)
    
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl)
      currentObjectUrl = null
    }
    
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    
    ElMessage.info('已重置为上次保存的信息')
  } else {
    ElMessage.warning('暂无已保存的数据')
  }
}

// 清空：清空所有表单字段（不清除已保存的数据）
function clearForm() {
  ElMessageBox.confirm(
    '清空后表单内容将被清空，但已保存的数据不会丢失。确定要清空吗？',
    '确认清空',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 清空表单所有字段
    form.realName = ''
    form.email = ''
    form.gender = 'private'
    form.birthday = ''
    form.address = ''
    // 注意：手机号不可修改，不清空
    
    // 清空头像预览（但不删除已上传的头像）
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl)
      currentObjectUrl = null
    }
    avatarSrc.value = ''
    
    // 清空文件输入框
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    
    ElMessage.success('表单已清空')
  }).catch(() => {})
}

function pickAvatar() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const el = e.target as HTMLInputElement
  const file = el.files && el.files[0]
  if (!file) return
  
  // 清理之前的 objectURL
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl)
    currentObjectUrl = null
  }
  
  // 创建新的预览
  currentObjectUrl = URL.createObjectURL(file)
  avatarSrc.value = currentObjectUrl
  
  // 上传
  const formData = new FormData()
  formData.append('file', file)
  try {
    await request.post<any, string>('/api/auth/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    
    // 上传成功后刷新用户信息，更新 savedData
    const refreshed = await getMe()
    savedData.value = { ...refreshed }
    updateAvatarDisplay(refreshed)
    
    // 清理临时 objectURL
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl)
      currentObjectUrl = null
    }
    
    authStore.setUserInfo({ ...(authStore.userInfo as any), avatar: avatarSrc.value })
    ElMessage.success('头像上传成功')
  } catch (err) {
    console.error(err)
    ElMessage.error('头像上传失败')
    // 上传失败，恢复到之前保存的头像
    if (savedData.value) {
      updateAvatarDisplay(savedData.value)
    }
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
  margin-bottom: 1.75rem;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.page-header p {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.profile-card,
.data-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 1.75rem 2rem;
  margin-bottom: 1.75rem;
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