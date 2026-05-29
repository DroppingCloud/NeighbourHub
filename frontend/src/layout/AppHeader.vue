<template>
  <header class="app-header">
    <div class="header-left">
      <div class="logo">
        <span class="logo-icon">社</span>
        <span class="logo-text">智慧社区服务平台</span>
      </div>
    </div>

    <div class="header-right">
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import NotificationBell from '@/components/NotificationBell.vue'
import FontSizeControl from '@/components/FontSizeControl.vue'

const router = useRouter()
const authStore = useAuthStore()

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'accessibility') {
    ElMessage.info('可通过顶部字号按钮调整字体大小')
  }
}
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

@media (max-width: 48rem) {
  .app-header {
    padding: 0.75rem 1rem;
  }

  .logo-text {
    font-size: 1rem;
  }
}
</style>
