<template>
  <AuthLayout>
    <div class="login-box">
      <div class="form-header">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-hint">请选择登录方式</p>
      </div>

      <div class="login-tabs">
        <button
          v-for="tab in loginTabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: loginMode === tab.key }"
          @click="switchMode(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <el-form
        ref="loginFormRef"
        :key="loginMode"
        :model="form"
        :rules="currentRules"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item v-if="showPhone" label="手机号码" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入注册手机号"
            maxlength="11"
            size="large"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <el-form-item v-if="loginMode === 'account-pwd'" label="账号" prop="account">
          <el-input
            v-model="form.account"
            placeholder="请输入系统账号"
            size="large"
            :prefix-icon="Tickets"
            clearable
          />
        </el-form-item>

        <el-form-item v-if="loginMode === 'username-pwd'" label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item v-if="loginMode === 'phone-code'" label="短信验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="请输入6位验证码"
              maxlength="6"
              size="large"
              :prefix-icon="Message"
              style="flex:1"
            />
            <el-button
              class="code-btn"
              :disabled="codeCooldown > 0 || !form.phone || form.phone.length < 11"
              @click="sendCode"
              size="large"
            >
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
            </el-button>
          </div>
          <div class="code-tip" v-if="codeSent">
            <el-icon><InfoFilled /></el-icon>
            <span>模拟验证码：{{ mockCode }}</span>
          </div>
        </el-form-item>

        <el-form-item v-if="showPassword" label="密码" prop="password">
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

        <div v-if="showPassword" class="login-options">
          <el-checkbox v-model="rememberMe" size="small">
            <span style="font-size:0.8125rem">记住我</span>
          </el-checkbox>
          <a class="forgot-link" href="javascript:void(0)" @click="handleForgot">忘记密码？</a>
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>

      <div class="form-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>

      <div class="divider">
        <span>温馨提示</span>
      </div>
      <div class="notice">
        <el-icon><WarnTriangleFilled /></el-icon>
        <span>如遇办理问题，可拨打社区服务热线或前往社区服务站窗口咨询</span>
      </div>
    </div>

    <!-- 忘记密码弹窗：加宽+替换注册同款自定义步骤条 -->
    <el-dialog
      v-model="forgotVisible"
      title="重置密码"
      width="600"
      :close-on-click-modal="false"
      @closed="forgotStep = 1"
      class="forgot-dialog"
    >
      <!-- 自定义步骤条【完全照搬Register页面样式】 -->
      <div class="step-indicator">
        <div v-for="s in 3" :key="s" class="step-node" :class="{ active: forgotStep >= s, done: forgotStep > s }">
          <div class="node-circle">
            <el-icon v-if="forgotStep > s"><Check /></el-icon>
            <span v-else>{{ s }}</span>
          </div>
          <span class="node-label">{{ forgotStepLabels[s - 1] }}</span>
          <div v-if="s < 3" class="node-line" :class="{ active: forgotStep > s }"></div>
        </div>
      </div>

      <!-- Step 1：手机号 + 验证码 -->
      <el-form
        v-if="forgotStep === 1"
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotStep1Rules"
        label-position="top"
        class="forgot-form"
      >
        <el-form-item label="手机号码" prop="phone">
          <el-input
            v-model="forgotForm.phone"
            placeholder="请输入注册手机号"
            maxlength="11"
            size="large"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <el-form-item label="短信验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="forgotForm.code"
              placeholder="请输入6位验证码"
              maxlength="6"
              size="large"
              :prefix-icon="Message"
              style="flex:1"
            />
            <el-button
              class="code-btn"
              :disabled="forgotCodeCooldown > 0 || !forgotForm.phone || forgotForm.phone.length < 11"
              @click="sendForgotCode"
              size="large"
            >
              {{ forgotCodeCooldown > 0 ? `${forgotCodeCooldown}s` : '获取验证码' }}
            </el-button>
          </div>
          <div class="code-tip" v-if="forgotCodeSent">
            <el-icon><InfoFilled /></el-icon>
            <span>模拟验证码：{{ forgotMockCode }}</span>
          </div>
        </el-form-item>

        <el-button type="primary" size="large" style="width:100%" @click="submitForgotStep1">
          下一步
        </el-button>
      </el-form>

      <!-- Step 2：新密码 -->
      <el-form
        v-if="forgotStep === 2"
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotStep2Rules"
        label-position="top"
        class="forgot-form"
      >
        <el-form-item label="新密码" prop="newPwd">
          <el-input
            v-model="forgotForm.newPwd"
            :type="forgotShowPwd ? 'text' : 'password'"
            placeholder="请输入新密码（不少于6位）"
            size="large"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon class="pwd-eye" @click="forgotShowPwd = !forgotShowPwd">
                <View v-if="forgotShowPwd" /><Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input
            v-model="forgotForm.confirmPwd"
            :type="forgotShowConfirm ? 'text' : 'password'"
            placeholder="请再次输入新密码"
            size="large"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon class="pwd-eye" @click="forgotShowConfirm = !forgotShowConfirm">
                <View v-if="forgotShowConfirm" /><Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <div style="display:flex;gap:8px">
          <el-button size="large" style="flex:1" @click="forgotStep = 1">上一步</el-button>
          <el-button type="primary" size="large" style="flex:2" @click="submitForgotStep2">
            确认重置
          </el-button>
        </div>
      </el-form>

      <!-- Step 3：成功 -->
      <div v-if="forgotStep === 3" style="text-align:center;padding:32px 0 16px" class="forgot-success">
        <el-icon style="font-size:56px;color:#67c23a"><CircleCheckFilled /></el-icon>
        <p style="font-size:16px;font-weight:600;margin:16px 0 8px">密码重置成功</p>
        <p style="color:var(--text-muted);font-size:14px;margin-bottom:24px">
          请使用新密码重新登录
        </p>
        <el-button type="primary" size="large" style="width:100%" @click="closeForgot">
          返回登录
        </el-button>
      </div>
    </el-dialog>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { resetPassword } from '@/api/auth' 
import {
  Phone, Message, User, Lock, View, Hide,
  Tickets, InfoFilled, WarnTriangleFilled, CircleCheckFilled, Check
} from '@element-plus/icons-vue'
import AuthLayout from '@/layout/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { login, loginBySms } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

type LoginMode = 'phone-code' | 'phone-pwd' | 'account-pwd' | 'username-pwd'

const loginTabs = [
  { key: 'phone-code' as LoginMode, label: '验证码登录' },
  { key: 'phone-pwd' as LoginMode, label: '手机密码' },
  { key: 'account-pwd' as LoginMode, label: '账号登录' },
  { key: 'username-pwd' as LoginMode, label: '用户名登录' },
]

const loginMode = ref<LoginMode>('phone-code')
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const showPwd = ref(false)
const rememberMe = ref(false)
const codeSent = ref(false)
const codeCooldown = ref(0)
const mockCode = ref('')

const form = ref({
  phone: '',
  code: '',
  password: '',
  account: '',
  username: ''
})

const showPhone = computed(() => ['phone-code', 'phone-pwd'].includes(loginMode.value))
const showPassword = computed(() => ['phone-pwd', 'account-pwd', 'username-pwd'].includes(loginMode.value))

const phoneCodeRules: FormRules = {
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  code: [{ required: true, len: 6, message: '请输入6位验证码', trigger: 'blur' }]
}

const phonePwdRules: FormRules = {
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '请输入密码', trigger: 'blur' }]
}

const accountPwdRules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '请输入密码', trigger: 'blur' }]
}

const usernamePwdRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '请输入密码', trigger: 'blur' }]
}

const currentRules = computed(() => {
  const map: Record<LoginMode, FormRules> = {
    'phone-code': phoneCodeRules,
    'phone-pwd': phonePwdRules,
    'account-pwd': accountPwdRules,
    'username-pwd': usernamePwdRules
  }
  return map[loginMode.value]
})

function switchMode(mode: LoginMode) {
  loginMode.value = mode
  loginFormRef.value?.clearValidate()
  showPwd.value = false
  codeSent.value = false
}

function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  mockCode.value = String(Math.floor(100000 + Math.random() * 900000))
  codeSent.value = true
  codeCooldown.value = 60
  const timer = setInterval(() => {
    codeCooldown.value--
    if (codeCooldown.value <= 0) clearInterval(timer)
  }, 1000)
  ElMessage.success(`验证码已发送（模拟验证码：${mockCode.value}）`)
}

async function handleLogin() {
  await loginFormRef.value?.validate(async (valid) => {
    if (!valid) return

    if (loginMode.value === 'phone-code') {
      if (form.value.code !== mockCode.value) {
        ElMessage.error('验证码不正确')
        return
      }
    }

    loading.value = true
    try {
      // determine username to send
      // handle SMS-code login (simulated): call backend SMS login endpoint
      if (loginMode.value === 'phone-code') {
        try {
          const res = await loginBySms({ phone: form.value.phone, code: form.value.code })
          authStore.setToken(res.token)
          authStore.setUserInfo({
            userId: String(res.userId),
            username: res.username,
            phone: form.value.phone,
            role: (res.roles && res.roles[0]) ? res.roles[0].replace(/^ROLE_/, '').toLowerCase() : 'resident',
            staffType: res.staffType
          })
          ElMessage.success('登录成功，欢迎回来！')
          const role = authStore.userInfo?.role
          if (role === 'staff') {
            router.push('/staff/workbench')
          } else if (role === 'admin') {
            router.push('/admin/dashboard')
          } else {
            router.push('/home')
          }
        } catch (err: any) {
          // rely on global interceptor to show auth-related messages (401/403)
          console.warn('SMS 登录失败', err)
        } finally {
          loading.value = false
        }
        return
      }

      let username = ''
      if (loginMode.value === 'username-pwd') username = form.value.username
      else if (loginMode.value === 'account-pwd') username = form.value.account
      else if (loginMode.value === 'phone-pwd') username = form.value.phone

      const res = await login({ username, password: form.value.password })
      authStore.setToken(res.token)
      authStore.setUserInfo({
        userId: String(res.userId),
        username: res.username,
        phone: form.value.phone,
        role: (res.roles && res.roles[0]) ? res.roles[0].replace(/^ROLE_/, '').toLowerCase() : 'resident',
        staffType: res.staffType
      })
      ElMessage.success('登录成功，欢迎回来！')
      const role = authStore.userInfo?.role
      if (role === 'staff') {
        router.push('/staff/workbench')
      } else if (role === 'admin') {
        router.push('/admin/dashboard')
      } else {
        router.push('/home')
      }
    } catch (err: any) {
      ElMessage.error(err?.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}

// ===== 忘记密码 =====
const forgotVisible = ref(false)
const forgotStep = ref(1)
// 步骤文字，和注册页保持一致
const forgotStepLabels = ['验证手机', '设置新密码', '完成']
const forgotCodeSent = ref(false)
const forgotCodeCooldown = ref(0)
const forgotMockCode = ref('')
const forgotShowPwd = ref(false)
const forgotShowConfirm = ref(false)
const forgotFormRef = ref<FormInstance>()

const forgotForm = ref({
  phone: '',
  code: '',
  newPwd: '',
  confirmPwd: ''
})

const forgotStep1Rules: FormRules = {
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  code:  [{ required: true, len: 6, message: '请输入6位验证码', trigger: 'blur' }]
}

const forgotStep2Rules: FormRules = {
  newPwd:     [{ required: true, min: 6, message: '密码不能少于6位', trigger: 'blur' }],
  confirmPwd: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== forgotForm.value.newPwd) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function sendForgotCode() {
  if (!/^1[3-9]\d{9}$/.test(forgotForm.value.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  forgotMockCode.value = String(Math.floor(100000 + Math.random() * 900000))
  forgotCodeSent.value = true
  forgotCodeCooldown.value = 60
  const timer = setInterval(() => {
    forgotCodeCooldown.value--
    if (forgotCodeCooldown.value <= 0) clearInterval(timer)
  }, 1000)
  ElMessage.success(`验证码已发送（模拟验证码：${forgotMockCode.value}）`)
}

async function submitForgotStep1() {
  await forgotFormRef.value?.validate(valid => {
    if (!valid) return
    if (forgotForm.value.code !== forgotMockCode.value) {
      ElMessage.error('验证码不正确')
      return
    }
    forgotStep.value = 2
  })
}

async function submitForgotStep2() {
  await forgotFormRef.value?.validate(async valid => {
    if (!valid) return
    try {
      await resetPassword({
        phone: forgotForm.value.phone,
        code: forgotForm.value.code,
        newPassword: forgotForm.value.newPwd,
        confirmPassword: forgotForm.value.confirmPwd
      })
      forgotStep.value = 3
    } catch (err: any) {
      ElMessage.error(err?.message || '重置失败，请重试')
    }
  })
}

function closeForgot() {
  forgotVisible.value = false
}

function handleForgot() {
  forgotStep.value = 1
  forgotForm.value = { phone: '', code: '', newPwd: '', confirmPwd: '' }
  forgotCodeSent.value = false
  forgotMockCode.value = ''
  forgotVisible.value = true
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
  background: rgba(212, 67, 67, 0.08);
  border: 0.0625rem solid rgba(212, 168, 67, 0.2);
  border-radius: var(--radius-sm);
  line-height: 1.5;
  margin-top: 1rem;
}

/* Code input row and button */
.code-row {
  display: flex;
  gap: 0.625rem;
  width: 100%;
}

.code-btn {
  flex-shrink: 0;
  min-width: 6.25rem;
}

.code-tip {
  display: flex;
  align-items: center;
  gap: 0.3125rem;
  font-size: 0.75rem;
  color: var(--jade);
  margin-top: 0.375rem;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0.5rem 0 1.25rem 0;
  min-height: 2rem;
}

:deep(.el-checkbox) {
  height: auto !important;
  line-height: 1.5 !important;
}

:deep(.el-checkbox__label) {
  font-size: 0.875rem !important;
  line-height: 1.5 !important;
  color: var(--text-secondary);
}

.forgot-link {
  font-size: 0.875rem !important;
  color: var(--text-muted);
  text-decoration: none;
  line-height: 1.5;
}

.forgot-link:hover {
  color: var(--vermilion);
}

:deep(.el-form-item) {
  margin-bottom: 1.25rem !important;
}

:deep(.el-form-item__label) {
  font-size: 0.875rem !important;
  color: var(--text-secondary) !important;
  padding-bottom: 0.25rem !important;
}

/* ===== 弹窗自定义步骤（和注册页面完全一致样式） ===== */
.step-indicator {
  display: flex;
  align-items: flex-start;
  margin-bottom: 2rem;
  padding: 0 0.25rem;
}
.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
}
.node-circle {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 0.09375rem solid var(--border-color);
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8125rem;
  color: var(--text-muted);
  font-weight: 600;
  z-index: 1;
}
.step-node.active .node-circle {
  border-color: var(--ink);
  background: var(--ink);
  color: #fff;
}
.step-node.done .node-circle {
  border-color: var(--jade);
  background: var(--jade);
  color: #fff;
}
.node-label {
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 0.375rem;
  white-space: nowrap;
}
.node-line {
  position: absolute;
  top: 1rem;
  left: calc(50% + 1rem);
  width: calc(100% - 2rem);
  height: 0.0625rem;
  background: var(--border-color);
}
.node-line.active {
  background: var(--jade);
}
.forgot-form {
  padding: 0 0.25rem;
}
.forgot-success {
  padding: 10px 0;
}
/* 弹窗全局内边距加宽 */
:deep(.forgot-dialog .el-dialog__body) {
  padding: 24px 28px !important;
}
</style>
