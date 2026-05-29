<template>
  <AuthLayout>
    <div class="login-box">
      <div class="form-header">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-hint">请使用系统账号登录</p>
      </div>

      <div class="login-tabs">
        <button
          v-for="tab in loginTabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: loginMode === tab.key }"
          type="button"
          @click="switchMode(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <el-form
        ref="loginFormRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <template v-if="loginMode === 'username-pwd'">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
            >
              <template #suffix>
                <el-icon class="pwd-eye" @click="showPwd = !showPwd">
                  <View v-if="showPwd" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
        </template>

        <div v-else class="unavailable-panel">
          <el-icon><InfoFilled /></el-icon>
          <div>
            <strong>{{ currentTabLabel }}暂未开放</strong>
            <p>当前后端仅支持用户名和密码登录，请切换到“用户名登录”。</p>
          </div>
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          :disabled="loginMode !== 'username-pwd'"
          @click="handleLogin"
        >
          {{ loginMode === 'username-pwd' ? '登录' : '暂未开放' }}
        </el-button>
      </el-form>

      <div class="quick-users">
        <span>测试账号：</span>
        <button type="button" @click="fillUser('resident01')">居民</button>
        <button type="button" @click="fillUser('family01')">家属</button>
        <button type="button" @click="fillUser('staff01')">工作人员</button>
        <button type="button" @click="fillUser('admin')">管理员</button>
      </div>

      <div class="form-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>

      <div class="notice">
        <el-icon><InfoFilled /></el-icon>
        <span>初始化账号密码均为 123456。登录后会按后端返回角色进入对应界面。</span>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Hide, InfoFilled, Lock, User, View } from '@element-plus/icons-vue'
import AuthLayout from '@/layout/AuthLayout.vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const showPwd = ref(false)
const loginMode = ref<LoginMode>('username-pwd')

type LoginMode = 'username-pwd' | 'account-pwd' | 'phone-pwd' | 'phone-code'

const loginTabs = [
  { key: 'username-pwd' as LoginMode, label: '用户名登录' },
  { key: 'account-pwd' as LoginMode, label: '账号登录' },
  { key: 'phone-pwd' as LoginMode, label: '手机密码' },
  { key: 'phone-code' as LoginMode, label: '验证码登录' }
]

const currentTabLabel = computed(() => {
  return loginTabs.find(tab => tab.key === loginMode.value)?.label || '该登录方式'
})

const form = ref({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '请输入至少 6 位密码', trigger: 'blur' }]
}

function fillUser(username: string) {
  loginMode.value = 'username-pwd'
  form.value.username = username
  form.value.password = '123456'
}

function switchMode(mode: LoginMode) {
  loginMode.value = mode
  loginFormRef.value?.clearValidate()
}

async function handleLogin() {
  if (loginMode.value !== 'username-pwd') {
    ElMessage.info(`${currentTabLabel.value}暂未开放`)
    return
  }

  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await login({
      username: form.value.username,
      password: form.value.password
    })
    const role = normalizeRole(result.roles?.[0])

    authStore.setToken(result.token)
    authStore.setUserInfo({
      userId: String(result.userId),
      username: result.username,
      phone: '',
      role
    })

    ElMessage.success('登录成功')
    if (role === 'admin') {
      router.push('/admin/dashboard')
    } else if (role === 'staff') {
      router.push('/staff/workbench')
    } else {
      router.push('/home')
    }
  } finally {
    loading.value = false
  }
}

function normalizeRole(role?: string) {
  const map: Record<string, string> = {
    ROLE_RESIDENT: 'resident',
    ROLE_FAMILY: 'family',
    ROLE_STAFF: 'staff',
    ROLE_ADMIN: 'admin',
    resident: 'resident',
    family: 'family',
    staff: 'staff',
    admin: 'admin'
  }
  return map[role || ''] || 'resident'
}
</script>

<style scoped>
.login-box {
  width: 100%;
}

.form-header {
  margin-bottom: 1.5rem;
}

.form-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.form-hint,
.form-footer,
.notice,
.quick-users {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.login-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.25rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  padding: 0.25rem;
  margin-bottom: 1.5rem;
}

.tab-btn {
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: calc(var(--radius-sm) - 0.125rem);
  padding: 0.55rem 0.35rem;
  cursor: pointer;
  line-height: 1.4;
  min-width: 0;
  white-space: nowrap;
}

.tab-btn.active {
  background: var(--card-bg);
  color: var(--text-primary);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.unavailable-panel {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 1rem;
  min-height: 7rem;
  border: 0.0625rem dashed var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  line-height: 1.5;
}

.unavailable-panel strong {
  display: block;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.unavailable-panel p {
  margin: 0;
}

.pwd-eye {
  cursor: pointer;
  color: var(--text-muted);
}

.submit-btn {
  width: 100%;
  min-height: 2.75rem;
  margin-top: 0.5rem;
}

.quick-users {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-top: 1rem;
}

.quick-users button {
  border: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  padding: 0.35rem 0.65rem;
  cursor: pointer;
}

.quick-users button:hover {
  color: var(--gold);
  border-color: var(--gold);
}

.form-footer {
  text-align: center;
  margin-top: 1.5rem;
}

.form-footer .link {
  color: var(--gold);
  text-decoration: none;
  margin-left: 0.25rem;
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem 0.875rem;
  background: rgba(212, 168, 67, 0.08);
  border: 0.0625rem solid rgba(212, 168, 67, 0.2);
  border-radius: var(--radius-sm);
  line-height: 1.5;
  margin-top: 1rem;
}
</style>
