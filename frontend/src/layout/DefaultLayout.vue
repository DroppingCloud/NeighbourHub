<template>
  <div class="layout">

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-icon-wrap">
          <svg width="22" height="22" viewBox="0 0 36 36" fill="none">
            <circle cx="18" cy="18" r="17" stroke="url(#sl)" stroke-width="1.5"/>
            <circle cx="18" cy="13" r="4" fill="url(#sl)" opacity="0.9"/>
            <path d="M9 27c0-5 4-8 9-8s9 3 9 8" stroke="url(#sl)" stroke-width="1.5" stroke-linecap="round"/>
            <defs>
              <linearGradient id="sl" x1="1" y1="1" x2="35" y2="35" gradientUnits="userSpaceOnUse">
                <stop stop-color="#7BAFD4"/>
                <stop offset="1" stop-color="#4A7FA8"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <transition name="logo-text">
          <span v-if="!isCollapsed" class="logo-text">社区服务平台</span>
        </transition>
      </div>

      <!-- 菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="menu in permissionStore.menus"
          :key="menu.path"
          :to="menu.path"
          class="nav-item"
          :class="{ active: activeMenu === menu.path }"
        >
          <span class="nav-icon">
            <component :is="menu.icon" />
          </span>
          <transition name="nav-label">
            <span v-if="!isCollapsed" class="nav-label">{{ menu.title }}</span>
          </transition>
          <span v-if="!isCollapsed && menu.path === '/notice' && noticeStore.unreadCount" class="nav-badge">
            {{ noticeStore.unreadCount > 99 ? '99+' : noticeStore.unreadCount }}
          </span>
        </router-link>
      </nav>

      <!-- 折叠按钮 -->
      <button class="collapse-btn" @click="toggleCollapse">
        <svg viewBox="0 0 16 16" fill="currentColor" width="14" height="14">
          <path v-if="!isCollapsed" d="M11 2L5 8l6 6" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <path v-else d="M5 2l6 6-6 6" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-wrap">

      <!-- 顶栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <!-- 面包屑 -->
          <div class="breadcrumb">
            <span class="bc-home">首页</span>
            <span v-if="currentTitle && currentTitle !== '首页'" class="bc-sep">/</span>
            <span v-if="currentTitle && currentTitle !== '首页'" class="bc-current">{{ currentTitle }}</span>
          </div>
        </div>

        <div class="topbar-right">
          <!-- 通知铃 -->
          <button class="top-btn" @click="$router.push('/notice')">
            <svg viewBox="0 0 20 20" fill="currentColor" width="17" height="17">
              <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zm0 16a2 2 0 01-2-2h4a2 2 0 01-2 2z"/>
            </svg>
            <span v-if="noticeStore.unreadCount" class="top-badge">{{ noticeStore.unreadCount }}</span>
          </button>

          <!-- 用户 -->
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-pill">
              <div class="user-avatar">{{ avatarChar }}</div>
              <span class="user-name">{{ userStore.userInfo?.username }}</span>
              <svg viewBox="0 0 12 12" fill="currentColor" width="10" height="10" style="opacity:.5">
                <path d="M2 4l4 4 4-4" stroke="currentColor" stroke-width="1.2" fill="none" stroke-linecap="round"/>
              </svg>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <svg viewBox="0 0 16 16" fill="none" width="13" height="13" style="margin-right:6px">
                    <path d="M10 2H13a1 1 0 011 1v10a1 1 0 01-1 1H10M7 11l3-3-3-3M2 8h8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <RouterView />
      </main>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { usePermissionStore } from '@/stores/permissionStore'
import { useNoticeStore } from '@/stores/noticeStore'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const noticeStore = useNoticeStore()

const isCollapsed = ref(false)
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title as string || '')
const avatarChar = computed(() => (userStore.userInfo?.username || '?')[0].toUpperCase())

const toggleCollapse = () => { isCollapsed.value = !isCollapsed.value }

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  permissionStore.loadPermissions(userStore.roles)
  noticeStore.fetchUnreadCount()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500;600&display=swap');

/* ─── 整体布局 ─── */
.layout {
  display: flex;
  height: 100vh;
  background: #EDF2F7;
  font-family: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ─── 侧边栏 ─── */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #F7F9FC;
  border-right: 1px solid rgba(91,127,166,0.12);
  display: flex;
  flex-direction: column;
  transition: width 0.28s cubic-bezier(0.4,0,0.2,1);
  position: relative;
  z-index: 20;
  box-shadow: 2px 0 16px rgba(60,90,140,0.06);
}
.sidebar.collapsed { width: 64px; }

/* Logo */
.sidebar-logo {
  height: 62px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(91,127,166,0.1);
  overflow: hidden;
  white-space: nowrap;
}
.logo-icon-wrap {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  font-size: 14px;
  font-weight: 600;
  color: #2C4A6E;
  letter-spacing: 0.5px;
}
.logo-text-enter-active, .logo-text-leave-active { transition: opacity 0.2s, transform 0.2s; }
.logo-text-enter-from, .logo-text-leave-to { opacity: 0; transform: translateX(-6px); }

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 10px;
  text-decoration: none;
  color: #6A7F96;
  font-size: 13.5px;
  font-weight: 500;
  transition: background 0.18s, color 0.18s;
  white-space: nowrap;
  position: relative;
  cursor: pointer;
}
.nav-item:hover {
  background: rgba(91,127,166,0.08);
  color: #3D6490;
}
.nav-item.active {
  background: rgba(91,127,166,0.12);
  color: #2C4A6E;
  font-weight: 600;
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0; top: 6px; bottom: 6px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: #5B7FA6;
}
.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}
.nav-label {
  flex: 1;
}
.nav-label-enter-active, .nav-label-leave-active { transition: opacity 0.18s; }
.nav-label-enter-from, .nav-label-leave-to { opacity: 0; }

.nav-badge {
  background: #5B7FA6;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  line-height: 1.6;
}

/* 折叠按钮 */
.collapse-btn {
  margin: 10px;
  height: 36px;
  border: 1px solid rgba(91,127,166,0.15);
  background: rgba(91,127,166,0.05);
  border-radius: 9px;
  cursor: pointer;
  color: #6A7F96;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.18s, color 0.18s;
}
.collapse-btn:hover { background: rgba(91,127,166,0.12); color: #3D6490; }

/* ─── 主区域 ─── */
.main-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 顶栏 */
.topbar {
  height: 62px;
  background: #F7F9FC;
  border-bottom: 1px solid rgba(91,127,166,0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  flex-shrink: 0;
  box-shadow: 0 1px 8px rgba(60,90,140,0.05);
}
.topbar-left, .topbar-right { display: flex; align-items: center; gap: 14px; }

.breadcrumb { display: flex; align-items: center; gap: 7px; font-size: 13px; }
.bc-home { color: #8A9BB0; }
.bc-sep  { color: #C0CCDA; }
.bc-current { color: #2C4A6E; font-weight: 500; }

/* 顶栏按钮 */
.top-btn {
  position: relative;
  width: 34px; height: 34px;
  border: 1px solid rgba(91,127,166,0.18);
  background: transparent;
  border-radius: 9px;
  color: #6A7F96;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.18s, color 0.18s;
}
.top-btn:hover { background: rgba(91,127,166,0.09); color: #3D6490; }
.top-badge {
  position: absolute;
  top: -4px; right: -4px;
  background: #E06060;
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 8px;
  line-height: 1.5;
  border: 1.5px solid #F7F9FC;
}

/* 用户胶囊 */
.user-pill {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 12px 5px 5px;
  border: 1px solid rgba(91,127,166,0.18);
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.18s;
}
.user-pill:hover { background: rgba(91,127,166,0.07); }
.user-avatar {
  width: 26px; height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7BAFD4, #4A7FA8);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.user-name { font-size: 13px; font-weight: 500; color: #3A526A; }

/* 页面内容 */
.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}

/* Element Plus 覆写 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #4A6A8A;
}
:deep(.el-dropdown-menu__item:hover) { background: rgba(91,127,166,0.08); color: #2C4A6E; }
</style>
