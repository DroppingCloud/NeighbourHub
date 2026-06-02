<template>
  <header class="app-header">
    <div class="header-left">
      <div class="logo">
        <span class="logo-icon">社</span>
        <span class="logo-text">智慧社区服务平台</span>
      </div>
    </div>

    <div class="header-right">
      <el-dropdown v-if="isFamily && proxyStore.targets.length > 0" @command="switchProxy" class="proxy-switcher">
        <div class="proxy-info">
          <el-icon><User /></el-icon>
          <span>{{ currentProxyLabel }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :command="null">
              <span>本人</span>
            </el-dropdown-item>
            <el-dropdown-item
              v-for="target in proxyStore.targets"
              :key="target.id"
              :command="target"
            >
              <span>{{ target.name }}（{{ target.relation }}）</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <FontSizeControl />
      <NotificationBell />

      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="40" :icon="UserFilled" />
          <span class="username">{{ authStore.userInfo?.username || '用户' }}</span>
          <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="settings">系统设置</el-dropdown-item>
            <el-dropdown-item command="accessibility">无障碍设置</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, ArrowDown, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useProxyStore } from '@/stores/proxy'
import NotificationBell from '@/components/NotificationBell.vue'
import FontSizeControl from '@/components/FontSizeControl.vue'

const router = useRouter()
const authStore = useAuthStore()
const proxyStore = useProxyStore()

const isFamily = computed(() => authStore.userInfo?.role === 'family')
const currentProxyLabel = computed(() => {
  if (!proxyStore.currentTarget) return '本人'
  return `${proxyStore.currentTarget.name}（${proxyStore.currentTarget.relation}）`
})

function switchProxy(target: any) {
  proxyStore.setCurrentTarget(target)
  ElMessage.success(target ? `已切换至为 ${target.name} 代办` : '已切换至本人')
  // 触发页面数据刷新（可以发出全局事件或由各页面自行监听 store 变化）
  window.dispatchEvent(new CustomEvent('proxy-changed'))
}

async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'accessibility':
      router.push('/accessibility')
      break
    case 'logout':
      try {
        // 1. 先清除本地存储的 token（确保路由守卫立即认为未登录）
        localStorage.removeItem('token')
        // 如果有用户信息存储，一并清除
        localStorage.removeItem('userInfo')
        
        // 2. 调用 store 的 logout（如果有后端接口，可 await）
        await authStore.logout()
        proxyStore.clearTarget()
        
        // 3. 使用 replace 跳转到登录页，避免回退
        await router.replace('/login')
        ElMessage.success('已退出登录')
      } catch (error) {
        console.error('退出失败', error)
        // 即使出错也强制跳转
        window.location.href = '/login'
      }
      break
    default:
      break
  }
}

onMounted(() => {
  if (isFamily.value) {
    proxyStore.restoreTarget()
  }
})
</script>

<style scoped>
.app-header {
  min-height: var(--app-header-height, 4rem);
  background: var(--header-bg) !important;
  border-bottom: 0.0625rem solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0 1.75rem;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 0.0625rem 0.25rem var(--shadow-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.logo-icon {
  width: 2.5rem;
  height: 2.5rem;
  background: var(--ink);
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-serif);
  font-size: 1.25rem;
  color: var(--gold-light);
  font-weight: 700;
}

.logo-text {
  font-family: var(--font-serif);
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.04em;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: flex-end;
  min-width: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  cursor: pointer;
  padding: 0.375rem 0.875rem;
  border-radius: 2.5rem;
  transition: background 0.2s;
  min-width: 0;
}

.user-info:hover {
  background: var(--bg-tertiary);
}

.username {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.dropdown-icon {
  font-size: 0.875rem;
  color: var(--text-muted);
  flex-shrink: 0;
}

.proxy-switcher {
  margin-right: 0.5rem;
  cursor: pointer;
}
.proxy-info {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  background: var(--bg-tertiary);
  border-radius: 2rem;
  font-size: 0.875rem;
}

@media (max-width: 48rem) {
  .app-header {
    padding: 0.75rem 1rem;
  }

  .logo-text {
    font-size: 1rem;
  }
}
</style>