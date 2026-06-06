<!-- src/App.vue -->
<template>
  <div id="app" class="app-container">
    <div v-if="isAuthPage" class="auth-app">
      <router-view />
    </div>
    <div v-else class="main-app">
      <AppHeader />
      <div class="main-layout">
        <AppSidebar />
        <main class="main-content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/layout/AppHeader.vue'
import AppSidebar from '@/layout/AppSidebar.vue'

const route = useRoute()
const authStore = useAuthStore()

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

// 应用启动时恢复登录状态
onMounted(async () => {
  // 恢复 token
  const hasToken = authStore.initAuth()
  
  if (hasToken) {
    // 恢复用户信息（从 storage 中快速恢复）
    authStore.restoreUserInfo()
    
    // 可选：验证 token 是否仍然有效（建议在需要时才调用，避免每次刷新都请求）
    // 如果需要验证，取消下面的注释
    // const isValid = await authStore.verifyToken()
    // if (!isValid && !isAuthPage.value) {
    //   // token 无效，跳转到登录页
    //   window.location.href = '/login'
    // }
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-sans);
  background: var(--bg-primary);
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
}

.main-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-layout {
  display: flex;
  flex: 1;
  align-items: stretch;
  min-width: 0;
}

.main-content {
  flex: 1;
  min-width: 0;
  padding: 1.5rem;
  overflow-y: auto;
  min-height: calc(100vh - var(--app-header-height, 4rem));
  background: var(--bg-primary);
}

@media (max-width: 48rem) {
  .main-layout {
    flex-direction: column;
  }

  .main-content {
    padding: 1rem;
    min-height: auto;
  }
}
</style>