<template>
  <div class="auth-layout">
    <!-- 添加字体控制按钮 - 固定在右上角 -->
    <div class="auth-font-control">
      <FontSizeControl />
    </div>

    <div class="auth-panel-left">
      <div class="left-bg"></div>
      <div class="left-content">
        <div class="brand-mark">
          <span class="brand-icon">社</span>
        </div>
        <div class="brand-text">
          <h1 class="brand-title">多事项智能导办</h1>
          <h2 class="brand-subtitle">社区服务协同平台</h2>
        </div>
        <div class="brand-desc">
          <p>智慧连接居民与社区</p>
          <p>让每一次办事简单便捷</p>
        </div>
        <div class="left-features">
          <div v-for="(item, i) in features" :key="i" class="feature-item">
            <span class="feature-dot"></span>
            <span>{{ item }}</span>
          </div>
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
import FontSizeControl from '@/components/FontSizeControl.vue'

const features = [
  'AI 智能导办，自然语言交互',
  '政务事项在线申请办理',
  '社区服务便捷预约',
  '家属协同代办支持'
]
</script>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100vh;
  background: var(--paper);
  position: relative;
}

/* 字体控制按钮 - 固定在右上角 */
.auth-font-control {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 2rem;
  backdrop-filter: blur(4px);
  box-shadow: var(--shadow-sm);
}

.auth-panel-left {
  position: relative;
  width: 26.25rem;
  flex-shrink: 0;
  overflow: hidden auto;
}

.left-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, var(--ink-deep) 0%, var(--ink) 40%, var(--ink-light) 100%);
}

.left-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100%;
  padding: 3.5rem 3rem;
}

.brand-mark {
  width: 4rem;
  height: 4rem;
  background: rgba(212, 168, 67, 0.15);
  border: 0.09375rem solid rgba(212, 168, 67, 0.4);
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
}

.brand-icon {
  font-family: var(--font-serif);
  font-size: 1.75rem;
  color: var(--gold-light);
  font-weight: 700;
}

.brand-text {
  margin-bottom: 1.25rem;
}

.brand-title {
  font-family: var(--font-serif);
  font-size: 1.375rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.08em;
  margin-bottom: 0.375rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.brand-subtitle {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 400;
  color: rgba(255,255,255,0.65);
  letter-spacing: 0.1em;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.brand-desc {
  margin-bottom: 2.5rem;
}

.brand-desc p {
  font-size: 0.8125rem;
  color: rgba(255,255,255,0.45);
  line-height: 2;
}

.left-features {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8125rem;
  color: rgba(255,255,255,0.75);
}

.feature-dot {
  width: 0.375rem;
  height: 0.375rem;
  border-radius: 50%;
  background: var(--gold);
  flex-shrink: 0;
}

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

@media (max-width: 48rem) {
  .auth-layout {
    flex-direction: column;
  }
  .auth-panel-left {
    width: 100%;
    min-height: 12.5rem;
  }
  .left-content {
    padding: 2rem 1.75rem;
  }
  .brand-title { font-size: 1.125rem; }
  .left-features { display: none; }
  .auth-panel-right { padding: 2rem 1.5rem; }
  
  .auth-font-control {
    top: 0.75rem;
    right: 0.75rem;
  }
}
</style>
