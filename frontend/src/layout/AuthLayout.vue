<template>
  <div class="auth-layout">
    <div class="auth-font-control">
      <FontSizeControl />
    </div>

    <div class="auth-panel-left">
      <!-- Layered background: gradient base + animated decorative shapes -->
      <div class="left-bg"></div>
      <div class="left-decor">
        <!-- Floating ink circles (abstract mountain/cloud motif) -->
        <div class="decor-orb orb-1"></div>
        <div class="decor-orb orb-2"></div>
        <div class="decor-orb orb-3"></div>
        <!-- Gold accent lines (like calligraphy strokes) -->
        <div class="decor-stroke stroke-1"></div>
        <div class="decor-stroke stroke-2"></div>
        <!-- Grid pattern overlay -->
        <div class="decor-grid"></div>
      </div>
      <!-- Particle canvas -->
      <canvas ref="particleCanvas" class="particle-canvas"></canvas>
      <!-- Light beam sweep -->
      <div class="light-beam"></div>

      <div class="left-content">
        <div class="brand-mark">
          <img class="brand-icon" src="@/assets/logo.png" alt="社区服务平台" />
          <span class="brand-ring"></span>
        </div>

        <div class="brand-text">
          <h1 class="brand-title">多事项智能导办</h1>
          <h2 class="brand-subtitle">社区服务协同平台</h2>
        </div>

        <div class="brand-divider"></div>

        <div class="brand-desc">
          <p class="desc-line">智慧连接居民与社区</p>
          <p class="desc-line">让每一次办事简单便捷</p>
        </div>

        <div class="left-features">
          <div
            v-for="(item, i) in features"
            :key="i"
            class="feature-item"
            :style="{ '--idx': i }"
          >
            <span class="feature-icon">{{ item.icon }}</span>
            <div class="feature-body">
              <span class="feature-label">{{ item.label }}</span>
              <span class="feature-desc">{{ item.desc }}</span>
            </div>
          </div>
        </div>

        <div class="left-footer">
          <span class="footer-line"></span>
          <span class="footer-text">安全 · 便捷 · 智能</span>
          <span class="footer-line"></span>
        </div>
      </div>
    </div>

    <div class="auth-panel-right">
      <div class="right-inner">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import FontSizeControl from '@/components/FontSizeControl.vue'

const features = [
  { icon: '导', label: 'AI 智能导办', desc: '自然语言交互，精准推荐' },
  { icon: '办', label: '政务在线办理', desc: '一站式申请，材料预审' },
  { icon: '约', label: '社区服务预约', desc: '助餐陪诊，一键预约' },
  { icon: '代', label: '家属协同代办', desc: '远程授权，进度同步' }
]

// ---- Particle Effect ----
const particleCanvas = ref<HTMLCanvasElement | null>(null)
let animationId = 0

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  radius: number
  opacity: number
  fadeDir: number
}

onMounted(() => {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const PARTICLE_COUNT = 35
  const particles: Particle[] = []

  function resize() {
    if (!canvas) return
    const rect = canvas.parentElement!.getBoundingClientRect()
    canvas.width = rect.width * window.devicePixelRatio
    canvas.height = rect.height * window.devicePixelRatio
    canvas.style.width = rect.width + 'px'
    canvas.style.height = rect.height + 'px'
    ctx!.scale(window.devicePixelRatio, window.devicePixelRatio)
  }

  function createParticle(): Particle {
    const rect = canvas!.parentElement!.getBoundingClientRect()
    return {
      x: Math.random() * rect.width,
      y: Math.random() * rect.height,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3 - 0.15,
      radius: Math.random() * 1.5 + 0.5,
      opacity: Math.random() * 0.4 + 0.1,
      fadeDir: Math.random() > 0.5 ? 1 : -1
    }
  }

  function init() {
    resize()
    particles.length = 0
    for (let i = 0; i < PARTICLE_COUNT; i++) {
      particles.push(createParticle())
    }
  }

  function draw() {
    if (!canvas || !ctx) return
    const rect = canvas.parentElement!.getBoundingClientRect()
    const w = rect.width
    const h = rect.height

    ctx.clearRect(0, 0, w, h)

    for (const p of particles) {
      // Update position
      p.x += p.vx
      p.y += p.vy

      // Fade in/out gently
      p.opacity += p.fadeDir * 0.003
      if (p.opacity >= 0.5) { p.opacity = 0.5; p.fadeDir = -1 }
      if (p.opacity <= 0.05) { p.opacity = 0.05; p.fadeDir = 1 }

      // Wrap around edges
      if (p.x < -10) p.x = w + 10
      if (p.x > w + 10) p.x = -10
      if (p.y < -10) p.y = h + 10
      if (p.y > h + 10) p.y = -10

      // Draw particle
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(212, 168, 67, ${p.opacity})`
      ctx.fill()
    }

    // Draw subtle connecting lines between nearby particles
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 100) {
          const lineOpacity = (1 - dist / 100) * 0.08
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(212, 168, 67, ${lineOpacity})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }

    animationId = requestAnimationFrame(draw)
  }

  init()
  draw()

  window.addEventListener('resize', resize)
  onBeforeUnmount(() => {
    cancelAnimationFrame(animationId)
    window.removeEventListener('resize', resize)
  })
})
</script>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100vh;
  background: var(--paper);
  position: relative;
}

.auth-font-control {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
  background: var(--card-bg); /* 使用主题卡片背景，支持深色模式 */
  border-radius: 2rem;
  box-shadow: var(--shadow-sm);
}

/* ====== Left Panel ====== */
.auth-panel-left {
  position: relative;
  width: 26.25rem;
  flex-shrink: 0;
  overflow: hidden;
}

/* Multi-layer gradient base */
.left-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(45, 53, 97, 0.9) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(26, 26, 46, 0.95) 0%, transparent 55%),
    linear-gradient(160deg, #0d0d1a 0%, #1a1a2e 35%, #2d3561 75%, #1a1a2e 100%);
}

/* Decorative layer */
.left-decor {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

/* Floating orbs — abstract ink-wash circles */
.decor-orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  filter: blur(40px);
}

.orb-1 {
  width: 18rem;
  height: 18rem;
  background: radial-gradient(circle, var(--gold) 0%, transparent 70%);
  top: -4rem;
  right: -6rem;
  animation: orb-drift 12s ease-in-out infinite alternate;
}

.orb-2 {
  width: 14rem;
  height: 14rem;
  background: radial-gradient(circle, #5b8def 0%, transparent 70%);
  bottom: 10%;
  left: -4rem;
  animation: orb-drift 15s ease-in-out infinite alternate-reverse;
}

.orb-3 {
  width: 10rem;
  height: 10rem;
  background: radial-gradient(circle, var(--gold-light) 0%, transparent 70%);
  top: 45%;
  right: 15%;
  animation: orb-drift 10s ease-in-out infinite alternate;
  animation-delay: -3s;
}

@keyframes orb-drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(12px, -18px) scale(1.1); }
}

/* Gold accent strokes — diagonal lines */
.decor-stroke {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(212, 168, 67, 0.2), transparent);
  height: 1px;
}

.stroke-1 {
  width: 60%;
  top: 28%;
  left: 25%;
  transform: rotate(-8deg);
  animation: stroke-fade 6s ease-in-out infinite;
}

.stroke-2 {
  width: 40%;
  bottom: 22%;
  left: 10%;
  transform: rotate(5deg);
  animation: stroke-fade 6s ease-in-out infinite;
  animation-delay: -3s;
}

@keyframes stroke-fade {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.8; }
}

/* Subtle dot grid overlay */
.decor-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 2rem 2rem;
  animation: grid-shift 20s linear infinite;
}

@keyframes grid-shift {
  from { background-position: 0 0; }
  to { background-position: 2rem 2rem; }
}

/* Particle canvas layer */
.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* Diagonal light beam sweep */
.light-beam {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.light-beam::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 40%;
  height: 200%;
  background: linear-gradient(
    105deg,
    transparent 40%,
    rgba(255, 255, 255, 0.03) 45%,
    rgba(212, 168, 67, 0.04) 50%,
    rgba(255, 255, 255, 0.03) 55%,
    transparent 60%
  );
  animation: beam-sweep 8s ease-in-out infinite;
}

@keyframes beam-sweep {
  0%, 100% { transform: translateX(-20%) rotate(15deg); opacity: 0; }
  15% { opacity: 1; }
  85% { opacity: 1; }
  50% { transform: translateX(320%) rotate(15deg); }
}

/* Content */
.left-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
  padding: 3rem 2.5rem;
  justify-content: center;
}

/* Brand mark with ring animation */
.brand-mark {
  position: relative;
  width: 4rem;
  height: 4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
}

.brand-icon {
  position: relative;
  z-index: 1;
  width: 2.5rem;
  height: 2.5rem;
  object-fit: contain;
}

.brand-ring {
  position: absolute;
  inset: 0;
  border-radius: 1rem;
  border: 1.5px solid rgba(212, 168, 67, 0.4);
  background: rgba(212, 168, 67, 0.08);
  animation: ring-breathe 4s ease-in-out infinite;
}

@keyframes ring-breathe {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.06); opacity: 0.7; }
}

/* Typography */
.brand-text {
  margin-bottom: 1rem;
  animation: fadeSlideUp 0.6s ease both;
  animation-delay: 0.1s;
}

.brand-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.12em;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.brand-subtitle {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 400;
  color: rgba(255,255,255,0.55);
  letter-spacing: 0.15em;
  line-height: 1.4;
}

/* Gold divider */
.brand-divider {
  width: 3rem;
  height: 2px;
  background: linear-gradient(90deg, var(--gold), transparent);
  margin-bottom: 1.25rem;
  border-radius: 1px;
  animation: fadeSlideUp 0.6s ease both;
  animation-delay: 0.25s;
}

/* Description lines with stagger */
.brand-desc {
  margin-bottom: 2.5rem;
  animation: fadeSlideUp 0.6s ease both;
  animation-delay: 0.35s;
}

.desc-line {
  font-size: 0.875rem;
  color: rgba(255,255,255,0.4);
  line-height: 2.2;
  letter-spacing: 0.08em;
}

/* Feature items with staggered reveal */
.left-features {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-sm);
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.05);
  transition: all 0.3s ease;
  animation: fadeSlideUp 0.5s ease both;
  animation-delay: calc(0.45s + var(--idx) * 0.1s);
}

.feature-item:hover {
  background: rgba(212, 168, 67, 0.06);
  border-color: rgba(212, 168, 67, 0.15);
  transform: translateX(4px);
}

.feature-icon {
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--gold);
  background: rgba(212, 168, 67, 0.1);
  border-radius: 0.375rem;
  flex-shrink: 0;
}

.feature-body {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.feature-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  letter-spacing: 0.04em;
}

.feature-desc {
  font-size: 0.6875rem;
  color: rgba(255,255,255,0.4);
  letter-spacing: 0.03em;
}

/* Footer tagline */
.left-footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 2.5rem;
  animation: fadeSlideUp 0.5s ease both;
  animation-delay: 0.9s;
}

.footer-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.12), transparent);
}

.footer-text {
  font-size: 0.6875rem;
  color: rgba(255,255,255,0.3);
  letter-spacing: 0.2em;
  white-space: nowrap;
}

/* Entrance animation */
@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ====== Right Panel ====== */
.auth-panel-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2.5rem;
  background: var(--paper);
}

.right-inner {
  width: 100%;
  max-width: 26.25rem;
}

/* ====== Responsive ====== */
@media (max-width: 48rem) {
  .auth-layout {
    flex-direction: column;
  }
  .auth-panel-left {
    width: 100%;
    min-height: auto;
  }
  .left-content {
    min-height: auto;
    padding: 2rem 1.75rem;
  }
  .brand-title { font-size: 1.25rem; }
  .left-features { display: none; }
  .left-footer { display: none; }
  .brand-desc { margin-bottom: 0; }
  .auth-panel-right { padding: 2rem 1.5rem; }
  .auth-font-control {
    top: 0.75rem;
    right: 0.75rem;
  }
}
</style>
