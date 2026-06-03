<template>
  <AuthLayout>
    <div class="login-box" :class="{ 'is-ready': isReady }">
      <div class="form-header">
        <h2 class="form-title">欢迎回家</h2>
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
        <span class="tab-indicator" :style="tabIndicatorStyle"></span>
      </div>

      <el-form
        ref="loginFormRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <Transition name="form-field" mode="out-in">
          <div v-if="loginMode === 'username-pwd'" key="username-form">
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
          </div>

          <div v-else key="unavailable-form" class="unavailable-panel">
            <el-icon><InfoFilled /></el-icon>
            <div>
              <strong>{{ currentTabLabel }}暂未开放</strong>
              <p>当前后端仅支持用户名和密码登录，请切换到"用户名登录"。</p>
            </div>
          </div>
        </Transition>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :class="{ 'btn-pulse': loginReady }"
          :loading="loading"
          :disabled="loginMode !== 'username-pwd'"
          @click="handleLogin"
        >
          {{ loginMode === 'username-pwd' ? '登录' : '暂未开放' }}
        </el-button>
      </el-form>

      <div class="quick-users">
        <span class="quick-label">快捷登录：</span>
        <TransitionGroup name="quick-btn" appear>
          <button
            v-for="(user, idx) in quickUsers"
            :key="user.username"
            type="button"
            class="quick-user-btn"
            :style="{ '--delay': `${idx * 80}ms` }"
            @click="fillUser(user.username)"
          >
            <span class="quick-dot" :style="{ background: user.color }"></span>
            {{ user.label }}
          </button>
        </TransitionGroup>
      </div>

      <div class="form-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
const isReady = ref(false)
const loginMode = ref<LoginMode>('username-pwd')

type LoginMode = 'username-pwd' | 'account-pwd' | 'phone-pwd' | 'phone-code'

const loginTabs = [
  { key: 'username-pwd' as LoginMode, label: '用户名登录' },
  { key: 'account-pwd' as LoginMode, label: '账号登录' },
  { key: 'phone-pwd' as LoginMode, label: '手机密码' },
  { key: 'phone-code' as LoginMode, label: '验证码登录' }
]

const quickUsers = [
  { username: 'resident01', label: '居民', color: 'var(--jade)' },
  { username: 'family01', label: '家属', color: 'var(--gold)' },
  { username: 'staff01', label: '工作人员', color: '#5b8def' },
  { username: 'admin', label: '管理员', color: 'var(--vermilion)' }
]

const currentTabLabel = computed(() => {
  return loginTabs.find(tab => tab.key === loginMode.value)?.label || '该登录方式'
})

const tabIndicatorStyle = computed(() => {
  const idx = loginTabs.findIndex(t => t.key === loginMode.value)
  return { transform: `translateX(${idx * 100}%)` }
})

const loginReady = computed(() => {
  return loginMode.value === 'username-pwd' &&
    form.value.username.length > 0 &&
    form.value.password.length >= 6
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

onMounted(() => {
  requestAnimationFrame(() => { isReady.value = true })
})
</script>

<style scoped>
.login-box {
  width: 100%;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.login-box.is-ready {
  opacity: 1;
  transform: translateY(0);
}

/* Header */
.form-header {
  margin-bottom: 1.75rem;
}

.form-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.form-hint {
  font-size: 0.875rem;
  color: var(--text-muted);
}

/* Tabs with sliding indicator */
.login-tabs {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  padding: 0.25rem;
  margin-bottom: 1.5rem;
}

.tab-btn {
  position: relative;
  z-index: 1;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: calc(var(--radius-sm) - 0.125rem);
  padding: 0.55rem 0.35rem;
  cursor: pointer;
  line-height: 1.4;
  min-width: 0;
  white-space: nowrap;
  transition: color 0.25s ease;
  font-size: inherit;
}

.tab-btn.active {
  color: var(--text-primary);
  font-weight: 600;
}

.tab-indicator {
  position: absolute;
  top: 0.25rem;
  left: 0.25rem;
  width: calc(25% - 0.125rem);
  height: calc(100% - 0.5rem);
  background: var(--card-bg);
  border-radius: calc(var(--radius-sm) - 0.125rem);
  box-shadow: var(--shadow-sm);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}

/* Form transition */
.form-field-enter-active,
.form-field-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.form-field-enter-from {
  opacity: 0;
  transform: translateX(8px);
}

.form-field-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* Unavailable panel */
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

/* Password eye */
.pwd-eye {
  cursor: pointer;
  color: var(--text-muted);
  transition: color 0.2s;
}

.pwd-eye:hover {
  color: var(--gold);
}

/* Submit button */
.submit-btn {
  width: 100%;
  min-height: 2.75rem;
  margin-top: 0.5rem;
  transition: all 0.3s ease !important;
}

.submit-btn.btn-pulse {
  box-shadow: 0 0 0 0 rgba(26, 26, 46, 0.3) !important;
  animation: pulse-shadow 2s infinite;
}

@keyframes pulse-shadow {
  0% { box-shadow: 0 0 0 0 rgba(26, 26, 46, 0.25); }
  50% { box-shadow: 0 0 0 6px rgba(26, 26, 46, 0); }
  100% { box-shadow: 0 0 0 0 rgba(26, 26, 46, 0); }
}

/* Quick users */
.quick-users {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-top: 1.25rem;
  font-size: 0.875rem;
  color: var(--text-muted);
}

.quick-label {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.quick-user-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  border-radius: 2rem;
  padding: 0.35rem 0.75rem;
  cursor: pointer;
  font-size: 0.8125rem;
  transition: all 0.25s ease;
}

.quick-user-btn:hover {
  border-color: var(--gold);
  color: var(--gold);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(212, 168, 67, 0.15);
}

.quick-user-btn:active {
  transform: translateY(0) scale(0.97);
}

.quick-dot {
  width: 0.4375rem;
  height: 0.4375rem;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Quick button staggered entrance */
.quick-btn-enter-active {
  transition: opacity 0.3s ease var(--delay, 0ms), transform 0.3s ease var(--delay, 0ms);
}

.quick-btn-enter-from {
  opacity: 0;
  transform: translateY(6px) scale(0.9);
}

/* Footer */
.form-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.875rem;
  color: var(--text-muted);
}

.form-footer .link {
  color: var(--gold);
  text-decoration: none;
  margin-left: 0.25rem;
  transition: color 0.2s;
}

.form-footer .link:hover {
  color: var(--gold-light);
}
</style>
