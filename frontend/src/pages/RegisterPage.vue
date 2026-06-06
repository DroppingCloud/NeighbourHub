<template>
  <AuthLayout>
    <div class="register-box">
      <div class="step-indicator">
        <div v-for="s in 3" :key="s" class="step-node" :class="{ active: step >= s, done: step > s }">
          <div class="node-circle">
            <el-icon v-if="step > s"><Check /></el-icon>
            <span v-else>{{ s }}</span>
          </div>
          <span class="node-label">{{ stepLabels[s - 1] }}</span>
          <div v-if="s < 3" class="node-line" :class="{ active: step > s }"></div>
        </div>
      </div>

      <!-- Step 1 -->
      <div v-if="step === 1" class="step-content">
        <div class="form-header">
          <h2 class="form-title">注册账号</h2>
          <p class="form-hint">请输入手机号完成身份验证</p>
        </div>
        <el-form ref="step1FormRef" :model="form" :rules="step1Rules" label-position="top">
          <el-form-item label="手机号码" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入11位手机号"
              maxlength="11"
              size="large"
              :prefix-icon="Phone"
            />
          </el-form-item>
          <el-form-item label="短信验证码" prop="code">
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
                {{ codeCooldown > 0 ? `${codeCooldown}秒后重发` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <div class="code-tip" v-if="codeSent">
            <el-icon><InfoFilled /></el-icon>
            <span>验证码已发送（模拟：{{ mockCode }}）</span>
          </div>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="nextStep1">
            下一步
          </el-button>
        </el-form>
        <div class="form-footer">
          已有账号？<router-link to="/login" class="link">立即登录</router-link>
        </div>
      </div>

      <!-- Step 2 -->
      <div v-if="step === 2" class="step-content">
        <div class="form-header">
          <h2 class="form-title">完善信息</h2>
          <p class="form-hint">系统已为您分配专属账号</p>
        </div>

        <div class="account-card">
          <div class="account-card-label">您的专属账号</div>
          <div class="account-card-value">{{ assignedAccount }}</div>
          <div class="account-card-tip">账号唯一，请妥善记录</div>
        </div>

        <el-form ref="step2FormRef" :model="form" :rules="step2Rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请设置用户名（1-15个字符）"
              maxlength="15"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input
              v-model="form.realName"
              placeholder="请输入真实姓名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="身份证号" prop="idCard">
            <el-input
              v-model="form.idCard"
              placeholder="请输入身份证号"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              :type="showPwd ? 'text' : 'password'"
              placeholder="请设置密码（6-20位）"
              maxlength="20"
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
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              :type="showConfirmPwd ? 'text' : 'password'"
              placeholder="请再次输入密码"
              maxlength="20"
              size="large"
              :prefix-icon="Lock"
            >
              <template #suffix>
                <el-icon class="pwd-eye" @click="showConfirmPwd = !showConfirmPwd">
                  <View v-if="showConfirmPwd" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="选择身份" prop="role">
            <el-radio-group v-model="form.role" class="role-radio-group">
              <el-radio value="resident" border>居民用户</el-radio>
              <el-radio value="family" border>家属用户</el-radio>
            </el-radio-group>
            <div class="role-tip">{{ roleTip }}</div>
          </el-form-item>

          <div class="pwd-strength" v-if="form.password">
            <span class="strength-label">密码强度：</span>
            <div class="strength-bars">
              <div class="bar" :class="{ active: pwdStrength >= 1, weak: pwdStrength === 1 }"></div>
              <div class="bar" :class="{ active: pwdStrength >= 2, medium: pwdStrength === 2 }"></div>
              <div class="bar" :class="{ active: pwdStrength >= 3, strong: pwdStrength === 3 }"></div>
            </div>
            <span class="strength-text" :class="strengthClass">{{ strengthText }}</span>
          </div>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="nextStep2">
            完成注册
          </el-button>
        </el-form>
      </div>

      <!-- Step 3 -->
      <div v-if="step === 3" class="step-content step-success">
        <div class="success-icon">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
        <h2 class="success-title">注册成功</h2>
        <p class="success-desc">欢迎加入智慧社区服务平台</p>
        <div class="success-info">
          <div class="info-row">
            <span class="info-label">账号</span>
            <span class="info-val">{{ assignedAccount }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-val">{{ form.username }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">手机号</span>
            <span class="info-val">{{ form.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') }}</span>
          </div>
        </div>
        <el-button type="primary" size="large" class="submit-btn" @click="goLogin">
          前往登录
        </el-button>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Phone, Message, User, Lock, View, Hide,
  Check, InfoFilled, CircleCheckFilled
} from '@element-plus/icons-vue'
import AuthLayout from '@/layout/AuthLayout.vue'

const router = useRouter()

const step = ref(1)
const stepLabels = ['验证手机', '完善信息', '注册成功']
const loading = ref(false)
const codeSent = ref(false)
const codeCooldown = ref(0)
const mockCode = ref('')
const assignedAccount = ref('')
const showPwd = ref(false)
const showConfirmPwd = ref(false)

const step1FormRef = ref<FormInstance>()
const step2FormRef = ref<FormInstance>()

type RegisterRole = 'resident' | 'family'

const form = ref({
  phone: '',
  code: '',
  username: '',
  password: '',
  confirmPassword: '',
  role: 'resident' as RegisterRole,
  realName: '',
  idCard: ''
})

const pwdStrength = computed(() => {
  const p = form.value.password
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p) || /[!@#$%^&*]/.test(p)) score++
  if (score === 2 && p.length >= 10) score++
  return Math.min(score + (p.length >= 6 ? 1 : 0), 3)
})

const strengthText = computed(() => ['', '较弱', '一般', '强'][pwdStrength.value])
const strengthClass = computed(() => ['', 'text-weak', 'text-medium', 'text-strong'][pwdStrength.value])
const roleTip = computed(() => {
  return form.value.role === 'family'
    ? '家属用户可在登录后发起家属绑定申请，待居民确认后进行代办。'
    : '居民用户可办理本人事项、预约服务，并确认家属绑定申请。'
})

const step1Rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const step2Rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 15, message: '用户名长度1-15个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择身份', trigger: 'change' }
  ]
}

// import API
import { register } from '@/api/auth'

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
  ElMessage.success(`验证码已发送（模拟：${mockCode.value}）`)
}

async function nextStep1() {
  await step1FormRef.value?.validate(async (valid) => {
    if (!valid) return
    if (form.value.code !== mockCode.value) {
      ElMessage.error('验证码不正确')
      return
    }
    loading.value = true
    await new Promise(r => setTimeout(r, 800))
    assignedAccount.value = 'SQ' + Date.now().toString().slice(-8)
    loading.value = false
    step.value = 2
  })
}

async function nextStep2() {
  await step2FormRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await register({
        username: form.value.username,
        password: form.value.password,
        phone: form.value.phone,
        realName: form.value.realName,
        idCard: form.value.idCard,
        role: form.value.role,
        account: assignedAccount.value
      })
      // registration succeeded — redirect to login (do not auto-login)
      ElMessage.success('注册成功，请使用账号或手机号登录')
      router.push('/login')
    } catch (err: any) {
      // Global interceptor already shows server error messages; avoid duplicate toasts here
      console.warn('注册失败', err)
    } finally {
      loading.value = false
    }
  })
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-box {
  width: 100%;
}

.step-indicator {
  display: flex;
  align-items: flex-start;
  margin-bottom: 2.25rem;
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

.form-header {
  margin-bottom: 1.75rem;
}

.form-title {
  font-family: var(--font-serif);
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.form-hint {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.code-row {
  display: flex;
  gap: 0.625rem;
  width: 100%;
}

.code-btn {
  flex-shrink: 0;
  min-width: 6.875rem;
  white-space: nowrap;
}

.code-tip {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--jade);
  margin-top: -0.5rem;
  margin-bottom: 0.5rem;
}

.account-card {
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%);
  border-radius: var(--radius-md);
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.5rem;
  position: relative;
  overflow: hidden;
}

.account-card-label {
  font-size: 0.6875rem;
  color: rgba(255,255,255,0.5);
  letter-spacing: 0.08em;
  margin-bottom: 0.5rem;
}

.account-card-value {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gold-light);
  letter-spacing: 0.1em;
  margin-bottom: 0.375rem;
}

.account-card-tip {
  font-size: 0.6875rem;
  color: rgba(255,255,255,0.4);
}

.role-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.role-tip {
  margin-top: 0.5rem;
  font-size: 0.8125rem;
  line-height: 1.6;
  color: var(--text-muted);
}

.pwd-strength {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: -0.75rem;
  margin-bottom: 0.75rem;
}

.strength-label {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.strength-bars {
  display: flex;
  gap: 0.25rem;
}

.bar {
  width: 1.75rem;
  height: 0.25rem;
  border-radius: 0.125rem;
  background: var(--border-color);
}

.bar.active.weak { background: #e74c3c; }
.bar.active.medium { background: var(--gold); }
.bar.active.strong { background: var(--jade); }

.strength-text { font-size: 0.75rem; }
.text-weak { color: #e74c3c; }
.text-medium { color: var(--gold); }
.text-strong { color: var(--jade); }

.pwd-eye {
  cursor: pointer;
  color: var(--text-muted);
}

.submit-btn {
  width: 100% !important;
  height: 2.75rem !important;
  font-size: 0.9375rem !important;
  margin-top: 0.5rem;
}

.form-footer {
  text-align: center;
  margin-top: 1.25rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.link {
  color: var(--gold);
  text-decoration: none;
  margin-left: 0.25rem;
}

.link:hover {
  color: var(--vermilion);
}

.step-success {
  text-align: center;
  padding: 1.25rem 0;
}

.success-icon {
  font-size: 4rem;
  color: var(--jade);
  margin-bottom: 1rem;
}

.success-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.success-desc {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-bottom: 1.75rem;
}

.success-info {
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.5rem;
  text-align: left;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.625rem 0;
  border-bottom: 0.0625rem solid var(--border-color);
}

.info-row:last-child { border-bottom: none; }

.info-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.info-val {
  font-size: 0.8125rem;
  color: var(--text-primary);
  font-weight: 500;
}
</style>
