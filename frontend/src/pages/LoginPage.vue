<template>
  <div class="login-page" @mousemove="onMouseMove">

    <!-- Canvas 流动神经网络背景 -->
    <canvas ref="canvasRef" class="bg-canvas" />

    <!-- 背景光晕层 -->
    <div class="bg-glows">
      <div class="halo h1" />
      <div class="halo h2" />
      <div class="halo h3" />
    </div>

    <!-- 主卡片 -->
    <div class="login-card" :class="{ 'card-shake': shaking }">

      <!-- 卡片左侧光边 -->
      <div class="card-accent" />

      <!-- Logo 区 -->
      <div class="card-header">
        <div class="logo-mark">
          <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
            <circle cx="18" cy="18" r="17" stroke="url(#lg)" stroke-width="1.5"/>
            <circle cx="18" cy="13" r="4.5" fill="url(#lg)" opacity="0.9"/>
            <path d="M9 27c0-5 4-8 9-8s9 3 9 8" stroke="url(#lg)" stroke-width="1.5" stroke-linecap="round"/>
            <circle cx="10" cy="22" r="2.5" fill="url(#lg2)" opacity="0.7"/>
            <circle cx="26" cy="22" r="2.5" fill="url(#lg2)" opacity="0.7"/>
            <line x1="12.5" y1="22" x2="15.5" y2="22" stroke="url(#lg2)" stroke-width="1" opacity="0.6"/>
            <line x1="20.5" y1="22" x2="23.5" y2="22" stroke="url(#lg2)" stroke-width="1" opacity="0.6"/>
            <defs>
              <linearGradient id="lg" x1="1" y1="1" x2="35" y2="35" gradientUnits="userSpaceOnUse">
                <stop stop-color="#5B7FA6"/>
                <stop offset="1" stop-color="#8BAECF"/>
              </linearGradient>
              <linearGradient id="lg2" x1="8" y1="20" x2="28" y2="26" gradientUnits="userSpaceOnUse">
                <stop stop-color="#4A6E96"/>
                <stop offset="1" stop-color="#7A9EC0"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="header-text">
          <h1 class="platform-name">社区服务协同平台</h1>
          <p class="platform-slogan">智能导办 · 多事项协同 · 全流程跟踪</p>
        </div>
      </div>

      <!-- Tab -->
      <div class="tab-bar">
        <button class="tab-btn" :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
        <button class="tab-btn" :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
        <div class="tab-indicator" :style="{ transform: mode === 'login' ? 'translateX(0)' : 'translateX(100%)' }" />
      </div>

      <!-- 表单区 -->
      <div class="card-body">
        <transition name="alert-fade">
          <div v-if="alertMsg.text" class="form-alert" :class="alertMsg.type">
            <span class="alert-dot" />
            <span>{{ alertMsg.text }}</span>
            <button class="alert-close" @click="alertMsg.text = ''">✕</button>
          </div>
        </transition>

        <transition name="panel" mode="out-in">

          <!-- 登录 -->
          <el-form v-if="mode === 'login'" key="login"
            ref="loginFormRef" :model="loginForm" :rules="loginRules"
            @keyup.enter="handleLogin">

            <el-form-item prop="username">
              <div class="field" :class="{ focused: focusedField === 'lu' }">
                <span class="field-label">用户名</span>
                <el-input v-model="loginForm.username" placeholder="请输入用户名"
                  @focus="focusedField='lu'" @blur="focusedField=''" class="clean-input" />
              </div>
            </el-form-item>

            <el-form-item prop="password">
              <div class="field" :class="{ focused: focusedField === 'lp' }">
                <span class="field-label">密码</span>
                <el-input v-model="loginForm.password"
                  :type="showLoginPwd ? 'text' : 'password'"
                  placeholder="请输入密码"
                  @focus="focusedField='lp'" @blur="focusedField=''" class="clean-input" />
                <button type="button" class="eye-btn" @click="showLoginPwd = !showLoginPwd">
                  <svg v-if="!showLoginPwd" viewBox="0 0 16 16" fill="currentColor" width="14" height="14">
                    <path d="M8 3C4.5 3 1.5 5.5 0 8c1.5 2.5 4.5 5 8 5s6.5-2.5 8-5c-1.5-2.5-4.5-5-8-5zm0 8a3 3 0 110-6 3 3 0 010 6z"/>
                  </svg>
                  <svg v-else viewBox="0 0 16 16" fill="currentColor" width="14" height="14">
                    <path d="M2 2l12 12M8 3C4.5 3 1.5 5.5 0 8a10.6 10.6 0 002.34 3.07M5.5 5.5A3 3 0 0111 9.5m.5.5A3 3 0 015.5 5.5M13.66 11.07A10.6 10.6 0 0016 8c-1.5-2.5-4.5-5-8-5a9.4 9.4 0 00-2.34.32" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round"/>
                  </svg>
                </button>
              </div>
            </el-form-item>

            <button class="submit-btn" :class="{ loading }" :disabled="loading" @click.prevent="handleLogin">
              <span v-if="!loading" class="btn-content">
                <span>登录</span>
                <svg viewBox="0 0 16 16" fill="none" width="14" height="14">
                  <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <span v-else class="btn-dots"><i /><i /><i /></span>
            </button>
          </el-form>

          <!-- 注册 -->
          <el-form v-else key="register"
            ref="regFormRef" :model="regForm" :rules="regRules"
            @keyup.enter="handleRegister">

            <el-form-item prop="username">
              <div class="field" :class="{ focused: focusedField === 'ru' }">
                <span class="field-label">用户名</span>
                <el-input v-model="regForm.username" placeholder="3-20 位字母或数字"
                  @focus="focusedField='ru'" @blur="focusedField=''" class="clean-input" />
              </div>
            </el-form-item>

            <el-form-item prop="realName">
              <div class="field" :class="{ focused: focusedField === 'rn' }">
                <span class="field-label">真实姓名</span>
                <el-input v-model="regForm.realName" placeholder="请输入真实姓名"
                  @focus="focusedField='rn'" @blur="focusedField=''" class="clean-input" />
              </div>
            </el-form-item>

            <el-form-item prop="idCard">
              <div class="field" :class="{ focused: focusedField === 'ri' }">
                <span class="field-label">身份证号</span>
                <el-input v-model="regForm.idCard" placeholder="18 位身份证号"
                  @focus="focusedField='ri'" @blur="focusedField=''" class="clean-input" />
              </div>
            </el-form-item>

            <el-form-item prop="phone">
              <div class="field" :class="{ focused: focusedField === 'rph' }">
                <span class="field-label">手机号</span>
                <el-input v-model="regForm.phone" placeholder="选填"
                  @focus="focusedField='rph'" @blur="focusedField=''" class="clean-input" />
              </div>
            </el-form-item>

            <el-form-item prop="password">
              <div class="field" :class="{ focused: focusedField === 'rp' }">
                <span class="field-label">密码</span>
                <el-input v-model="regForm.password"
                  :type="showRegPwd ? 'text' : 'password'"
                  placeholder="6-20 位"
                  @focus="focusedField='rp'" @blur="focusedField=''" class="clean-input" />
                <button type="button" class="eye-btn" @click="showRegPwd = !showRegPwd">
                  <svg v-if="!showRegPwd" viewBox="0 0 16 16" fill="currentColor" width="14" height="14">
                    <path d="M8 3C4.5 3 1.5 5.5 0 8c1.5 2.5 4.5 5 8 5s6.5-2.5 8-5c-1.5-2.5-4.5-5-8-5zm0 8a3 3 0 110-6 3 3 0 010 6z"/>
                  </svg>
                  <svg v-else viewBox="0 0 16 16" fill="currentColor" width="14" height="14">
                    <path d="M8 3C4.5 3 1.5 5.5 0 8a10.6 10.6 0 002.34 3.07M5.5 5.5A3 3 0 0111 9.5m.5.5A3 3 0 015.5 5.5M13.66 11.07A10.6 10.6 0 0016 8c-1.5-2.5-4.5-5-8-5a9.4 9.4 0 00-2.34.32" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round"/>
                  </svg>
                </button>
              </div>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <div class="field" :class="{ focused: focusedField === 'rc' }">
                <span class="field-label">确认密码</span>
                <el-input v-model="regForm.confirmPassword"
                  :type="showRegPwd ? 'text' : 'password'"
                  placeholder="再次输入密码"
                  @focus="focusedField='rc'" @blur="focusedField=''" class="clean-input" />
              </div>
            </el-form-item>

            <button class="submit-btn" :class="{ loading }" :disabled="loading" @click.prevent="handleRegister">
              <span v-if="!loading" class="btn-content">
                <span>创建账号</span>
                <svg viewBox="0 0 16 16" fill="none" width="14" height="14">
                  <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <span v-else class="btn-dots"><i /><i /><i /></span>
            </button>
          </el-form>

        </transition>
      </div>

      <!-- 底部 -->
      <div class="card-footer">
        <transition name="footer-fade" mode="out-in">
          <div v-if="mode === 'login'" key="lf" class="roles-row">
            <span v-for="r in roleHints" :key="r.label" class="role-pip">
              <span class="pip-dot" :style="`background:${r.color}`" />{{ r.label }}
            </span>
          </div>
        </transition>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { usePermissionStore } from '@/stores/permissionStore'
import { register as registerApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// ─── 神经网络背景 ───────────────────────────────────────
const canvasRef = ref<HTMLCanvasElement>()
let animId = 0
const mouse = { x: -9999, y: -9999 }

function onMouseMove(e: MouseEvent) {
  mouse.x = e.clientX
  mouse.y = e.clientY
}

interface Node {
  x: number; y: number
  vx: number; vy: number
  r: number; pulse: number; pulseSpeed: number
}

function initCanvas() {
  const canvas = canvasRef.value!
  const ctx = canvas.getContext('2d')!

  function resize() {
    canvas.width  = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const COUNT = 58
  const nodes: Node[] = Array.from({ length: COUNT }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    vx: (Math.random() - 0.5) * 0.9,
    vy: (Math.random() - 0.5) * 0.9,
    r:  1.2 + Math.random() * 1.6,
    pulse: Math.random() * Math.PI * 2,
    pulseSpeed: 0.018 + Math.random() * 0.022,
  }))

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // 轻盈运动：低阻尼，鼠标微弱吸引 + 粒子间斥力
    nodes.forEach((n, idx) => {
      const dx = mouse.x - n.x
      const dy = mouse.y - n.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 140 && dist > 0) {
        n.vx += dx / dist * 0.008
        n.vy += dy / dist * 0.008
      }
      // 粒子间斥力：距离小于 40px 时互相推开
      for (let j = idx + 1; j < COUNT; j++) {
        const ex = n.x - nodes[j].x
        const ey = n.y - nodes[j].y
        const ed = Math.sqrt(ex * ex + ey * ey)
        if (ed < 40 && ed > 0) {
          const f = (40 - ed) / 40 * 0.06
          n.vx       += ex / ed * f
          n.vy       += ey / ed * f
          nodes[j].vx -= ex / ed * f
          nodes[j].vy -= ey / ed * f
        }
      }
      n.vx *= 0.972
      n.vy *= 0.972
      // 保持最低速度，防止静止
      const speed = Math.sqrt(n.vx * n.vx + n.vy * n.vy)
      if (speed < 0.12) {
        n.vx += (Math.random() - 0.5) * 0.06
        n.vy += (Math.random() - 0.5) * 0.06
      }

      n.x += n.vx
      n.y += n.vy
      n.pulse += n.pulseSpeed

      if (n.x < 0 || n.x > canvas.width)  n.vx *= -1
      if (n.y < 0 || n.y > canvas.height) n.vy *= -1
      n.x = Math.max(0, Math.min(canvas.width,  n.x))
      n.y = Math.max(0, Math.min(canvas.height, n.y))
    })

    // 细连线，缩短连接距离
    for (let i = 0; i < COUNT; i++) {
      for (let j = i + 1; j < COUNT; j++) {
        const dx = nodes[i].x - nodes[j].x
        const dy = nodes[i].y - nodes[j].y
        const d  = Math.sqrt(dx * dx + dy * dy)
        if (d < 120) {
          const alpha = (1 - d / 120) * 0.55
          const t = (Date.now() * 0.0008 + i * 0.25) % 1
          const grad = ctx.createLinearGradient(nodes[i].x, nodes[i].y, nodes[j].x, nodes[j].y)
          grad.addColorStop(0,                       `rgba(140,175,230,${alpha * 0.5})`)
          grad.addColorStop(Math.max(0,   t - 0.12), `rgba(140,175,230,${alpha * 0.5})`)
          grad.addColorStop(t,                       `rgba(200,225,255,${alpha * 2.0})`)
          grad.addColorStop(Math.min(1,   t + 0.12), `rgba(140,175,230,${alpha * 0.5})`)
          grad.addColorStop(1,                       `rgba(140,175,230,${alpha * 0.5})`)
          ctx.beginPath()
          ctx.moveTo(nodes[i].x, nodes[i].y)
          ctx.lineTo(nodes[j].x, nodes[j].y)
          ctx.strokeStyle = grad
          ctx.lineWidth = 0.9
          ctx.stroke()
        }
      }
    }

    // 圆形节点 + 小光晕
    nodes.forEach(n => {
      const glow = Math.sin(n.pulse) * 0.5 + 0.5
      const radius = n.r + glow * 0.8
      // 小光晕（radius * 1.8，原来是 radius * 3）
      const grad = ctx.createRadialGradient(n.x, n.y, 0, n.x, n.y, radius * 1.8)
      grad.addColorStop(0, `rgba(110,150,200,${0.55 + glow * 0.25})`)
      grad.addColorStop(1, `rgba(110,150,200,0)`)
      ctx.beginPath()
      ctx.arc(n.x, n.y, radius * 1.8, 0, Math.PI * 2)
      ctx.fillStyle = grad
      ctx.fill()
      // 实心核
      ctx.beginPath()
      ctx.arc(n.x, n.y, radius, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(150,185,230,${0.7 + glow * 0.3})`
      ctx.fill()
    })

    animId = requestAnimationFrame(draw)
  }
  draw()

  return () => {
    window.removeEventListener('resize', resize)
    cancelAnimationFrame(animId)
  }
}

onMounted(() => {
  const cleanup = initCanvas()
  onUnmounted(cleanup)
})

// ─── 表单逻辑 ──────────────────────────────────────────
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const shaking = ref(false)
const focusedField = ref('')
const showLoginPwd = ref(false)
const showRegPwd = ref(false)
const alertMsg = reactive({ text: '', type: 'error' as 'error' | 'success' })

const loginFormRef = ref<FormInstance>()
const regFormRef   = ref<FormInstance>()
const loginForm = reactive({ username: '', password: '' })
const regForm   = reactive({ username: '', password: '', confirmPassword: '', realName: '', idCard: '', phone: '' })

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码',   trigger: 'blur' }],
}
const regRules: FormRules = {
  username:        [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度 3-20 位', trigger: 'blur' }],
  realName:        [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idCard:          [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /^\d{17}[\dXx]$/, message: '格式不正确', trigger: 'blur' }],
  phone:           [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password:        [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_r: any, v: string, cb: any) => v !== regForm.password ? cb(new Error('两次密码不一致')) : cb(), trigger: 'blur' }],
}

const roleHints = [
  { label: '居民',    color: '#5B7FA6' },
  { label: '家属',    color: '#7AB0C8' },
  { label: '工作人员', color: '#6B9EC4' },
  { label: '管理员',  color: '#7A9EC0' },
]

function switchMode(m: 'login' | 'register') {
  alertMsg.text = ''
  mode.value = m
}
function triggerShake(msg: string) {
  alertMsg.text = msg; alertMsg.type = 'error'
  shaking.value = true
  setTimeout(() => (shaking.value = false), 600)
}

async function handleLogin() {
  try { await loginFormRef.value?.validate() } catch { return }
  alertMsg.text = ''; loading.value = true
  try {
    await userStore.login(loginForm)
    permissionStore.loadPermissions(userStore.roles)
    ElMessage.success(`欢迎回来，${userStore.userInfo?.username}`)
    router.push('/home')
  } catch (e: any) {
    triggerShake(e?.message || '用户名或密码错误')
  } finally { loading.value = false }
}

async function handleRegister() {
  try { await regFormRef.value?.validate() } catch { return }
  alertMsg.text = ''; loading.value = true
  try {
    await registerApi({ username: regForm.username, password: regForm.password, realName: regForm.realName, idCard: regForm.idCard, phone: regForm.phone || undefined })
    alertMsg.text = '注册成功，请登录'; alertMsg.type = 'success'
    loginForm.username = regForm.username; loginForm.password = ''
    setTimeout(() => switchMode('login'), 1400)
  } catch (e: any) {
    triggerShake(e?.message || '注册失败，请稍后重试')
  } finally { loading.value = false }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600&family=DM+Sans:wght@300;400;500;600&display=swap');

/* ─── 变量 ─── */
:root {
  --bg:        #EDF1F7;
  --blue:      #5B7FA6;
  --blue-lt:   #8BAECF;
  --blue-dk:   #3D6490;
  --text:      #1E2E3E;
  --text-2:    #6A7F96;
  --border:    rgba(91,127,166,0.2);
  --card-bg:   rgba(246,249,253,0.92);
  --field-bg:  rgba(234,240,250,0.55);
}

/* ─── 页面 ─── */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(150deg, #DDE6F2 0%, #E4EDF8 40%, #D8E6F5 100%);
  overflow: hidden;
  position: relative;
  font-family: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ─── Canvas ─── */
.bg-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

/* ─── 光晕 ─── */
.bg-glows { position: absolute; inset: 0; pointer-events: none; }
.halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  animation: halo-breathe ease-in-out infinite;
}
.h1 { width: 280px; height: 280px; top: -80px;  left: -80px;  background: rgba(120,160,210,0.16); animation-duration: 11s; animation-delay: 0s; }
.h2 { width: 220px; height: 220px; bottom: -60px; right: -60px; background: rgba(80,130,190,0.12);  animation-duration: 14s; animation-delay: 4s; }
.h3 { width: 180px; height: 180px; top: 38%; left: 58%;       background: rgba(110,160,220,0.10); animation-duration: 9s;  animation-delay: 7s; }
@keyframes halo-breathe {
  0%,100% { transform: scale(1);    opacity: .7; }
  50%      { transform: scale(1.12); opacity: 1;  }
}

/* ─── 卡片 ─── */
.login-card {
  position: relative;
  z-index: 10;
  width: 420px;
  background: var(--card-bg);
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
  border-radius: 20px;
  border: 1px solid rgba(150,185,225,0.45);
  box-shadow:
    0 1px 0 rgba(255,255,255,0.8) inset,
    0 2px 6px rgba(40,70,120,0.05),
    0 8px 24px rgba(40,70,120,0.09),
    0 24px 60px rgba(40,70,120,0.13),
    0 48px 80px rgba(30,55,100,0.08);
  animation: card-in .7s cubic-bezier(0.16,1,0.3,1) both;
  overflow: hidden;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(28px) scale(0.97); }
  to   { opacity: 1; transform: none; }
}
.card-shake { animation: shake .5s cubic-bezier(0.36,0.07,0.19,0.97) both !important; }
@keyframes shake {
  10%,90% { transform: translateX(-5px); }
  20%,80% { transform: translateX(7px); }
  30%,70% { transform: translateX(-7px); }
  40%,60% { transform: translateX(5px); }
  50%      { transform: translateX(-3px); }
}

/* 顶部流动光带 */
.card-accent {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(91,127,166,0.6) 20%,
    rgba(139,174,207,0.9) 50%,
    rgba(91,127,166,0.6) 80%,
    transparent 100%);
  background-size: 200% 100%;
  animation: accent-flow 3.5s ease-in-out infinite;
}
@keyframes accent-flow {
  0%   { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

/* ─── 头部 ─── */
.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 32px 36px 22px;
}
.logo-mark {
  flex-shrink: 0;
  animation: logo-bob 4s ease-in-out infinite;
}
@keyframes logo-bob {
  0%,100% { transform: translateY(0); }
  50%      { transform: translateY(-4px); }
}
.header-text { display: flex; flex-direction: column; gap: 3px; }
.platform-name {
  font-family: 'Cormorant Garamond', 'STSong', 'SimSun', serif;
  font-size: 19px;
  font-weight: 600;
  color: var(--text);
  letter-spacing: 1.5px;
  margin: 0;
  line-height: 1.2;
}
.platform-slogan {
  font-size: 11px;
  color: var(--text-2);
  letter-spacing: 1.8px;
  margin: 0;
  font-weight: 300;
}

/* ─── Tab ─── */
.tab-bar {
  position: relative;
  display: flex;
  margin: 0 36px 20px;
  background: rgba(91,127,166,0.08);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 3px;
}
.tab-btn {
  flex: 1;
  height: 34px;
  border: none;
  background: transparent;
  color: var(--text-2);
  font-family: 'DM Sans', sans-serif;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  position: relative;
  z-index: 2;
  border-radius: 7px;
  transition: color .25s;
  letter-spacing: .5px;
}
.tab-btn.active { color: var(--text); font-weight: 600; }
.tab-indicator {
  position: absolute;
  top: 3px; left: 3px;
  width: calc(50% - 3px);
  height: calc(100% - 6px);
  background: rgba(246,249,253,0.95);
  border-radius: 7px;
  box-shadow: 0 1px 4px rgba(40,70,120,0.1);
  transition: transform .32s cubic-bezier(0.34,1.56,0.64,1);
}

/* ─── 表单体 ─── */
.card-body { padding: 0 36px; }

/* Alert */
.form-alert {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 14px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 14px;
  letter-spacing: .2px;
}
.form-alert.error   { background: rgba(180,60,60,0.07);  color: #8B3030; border: 1px solid rgba(180,60,60,0.18); }
.form-alert.success { background: rgba(80,150,100,0.08); color: #2D6B45; border: 1px solid rgba(80,150,100,0.22); }
.alert-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
.alert-close { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 13px; color: inherit; opacity: .55; padding: 0; line-height: 1; }
.alert-close:hover { opacity: 1; }

.alert-fade-enter-active, .alert-fade-leave-active { transition: all .3s ease; }
.alert-fade-enter-from, .alert-fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* Panel 切换 */
.panel-enter-active { transition: all .3s cubic-bezier(0.16,1,0.3,1); }
.panel-leave-active { transition: all .22s ease; }
.panel-enter-from   { opacity: 0; transform: translateX(16px); }
.panel-leave-to     { opacity: 0; transform: translateX(-16px); }

:deep(.el-form-item) { margin-bottom: 10px; width: 100%; }
:deep(.el-form-item__content) { width: 100%; }
:deep(.el-form-item__error) {
  color: #9B4040;
  font-size: 11.5px;
  padding-top: 3px;
  font-family: 'DM Sans', sans-serif;
}

/* ─── 输入框 ─── */
.field {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  height: 46px;
  background: var(--field-bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0 12px 0 14px;
  gap: 0;
  transition: border-color .22s, box-shadow .22s, background .22s;
}
.field.focused {
  border-color: rgba(91,127,166,0.65);
  box-shadow: 0 0 0 3px rgba(91,127,166,0.12);
  background: rgba(244,248,254,0.95);
}
.field-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-2);
  white-space: nowrap;
  min-width: 60px;
  letter-spacing: .3px;
  flex-shrink: 0;
  border-right: 1px solid rgba(91,127,166,0.2);
  padding-right: 10px;
  margin-right: 10px;
}
.field.focused .field-label { color: var(--blue-dk); border-right-color: rgba(91,127,166,0.4); }

:deep(.clean-input) { flex: 1; min-width: 0; }
:deep(.clean-input .el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  padding: 0;
}
:deep(.clean-input .el-input__inner) {
  color: var(--text);
  font-family: 'DM Sans', 'PingFang SC', sans-serif;
  font-size: 13.5px;
  font-weight: 400;
  height: 100%;
}
:deep(.clean-input .el-input__inner::placeholder) {
  color: rgba(100,120,150,0.45);
  font-weight: 300;
}

.eye-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-2);
  opacity: .55;
  display: flex;
  align-items: center;
  padding: 0 0 0 8px;
  flex-shrink: 0;
  transition: opacity .2s;
}
.eye-btn:hover { opacity: .9; }

/* ─── 提交按钮 ─── */
.submit-btn {
  width: 100%;
  box-sizing: border-box;
  height: 46px;
  margin-top: 8px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #5B7FA6 0%, #3D6490 100%);
  color: #F0F5FB;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1.5px;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s, opacity .2s;
  box-shadow: 0 3px 14px rgba(60,100,160,0.32), inset 0 1px 0 rgba(255,255,255,0.15);
  position: relative;
  overflow: hidden;
}
/* 光泽扫过动效 */
.submit-btn::before {
  content: '';
  position: absolute;
  top: 0; left: -80%;
  width: 60%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.18), transparent);
  transform: skewX(-15deg);
  animation: btn-sheen 3.5s ease-in-out infinite;
}
@keyframes btn-sheen {
  0%,60%,100% { left: -80%; }
  30%          { left: 130%; }
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(60,100,160,0.42);
}
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: .6; cursor: not-allowed; }
.submit-btn.loading { pointer-events: none; }

.btn-content { display: inline-flex; align-items: center; gap: 7px; }
.btn-dots { display: inline-flex; align-items: center; gap: 5px; }
.btn-dots i {
  display: block; width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(240,245,251,0.85);
  animation: dot-bounce 1.2s infinite;
}
.btn-dots i:nth-child(2) { animation-delay: .18s; }
.btn-dots i:nth-child(3) { animation-delay: .36s; }
@keyframes dot-bounce {
  0%,80%,100% { transform: scale(.65); opacity: .45; }
  40%          { transform: scale(1.05); opacity: 1; }
}

/* ─── 底部 ─── */
.card-footer {
  padding: 14px 36px 26px;
  min-height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.footer-fade-enter-active, .footer-fade-leave-active { transition: opacity .25s; }
.footer-fade-enter-from,  .footer-fade-leave-to      { opacity: 0; }
.roles-row { display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; }
.role-pip {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-2);
  font-weight: 500;
  letter-spacing: .3px;
}
.pip-dot { width: 5px; height: 5px; border-radius: 50%; flex-shrink: 0; opacity: .8; }
.reg-note {
  font-size: 11.5px;
  color: var(--text-2);
  text-align: center;
  line-height: 1.6;
  margin: 0;
  font-weight: 400;
}
</style>
