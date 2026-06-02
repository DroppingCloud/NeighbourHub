import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/RegisterPage.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/pages/HomePage.vue'),
    meta: { title: '首页', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/pages/SettingsPage.vue'),
    meta: { title: '系统设置', requiresAuth: true }
  },
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('@/pages/GuidePage.vue'),
    meta: { title: '智能导办', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/application/submit',
    name: 'ApplicationSubmit',
    component: () => import('@/pages/ApplicationSubmitPage.vue'),
    meta: { title: '提交申请', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/application/list',
    name: 'ApplicationList',
    component: () => import('@/pages/ApplicationListPage.vue'),
    meta: { title: '我的申请', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/booking',
    name: 'Booking',
    component: () => import('@/pages/BookingPage.vue'),
    meta: { title: '服务预约', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/booking/list',
    name: 'BookingList',
    component: () => import('@/pages/BookingListPage.vue'),
    meta: { title: '我的预约', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/family-binding',
    name: 'FamilyBinding',
    component: () => import('@/pages/FamilyBindingPage.vue'),
    meta: { title: '家属代办', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/progress',
    name: 'Progress',
    component: () => import('@/pages/ProgressPage.vue'),
    meta: { title: '进度查询', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/notice',
    name: 'Notice',
    component: () => import('@/pages/NoticePage.vue'),
    meta: { title: '消息通知', requiresAuth: true }
  },
  {
    path: '/material-upload',
    name: 'MaterialUpload',
    component: () => import('@/pages/MaterialUploadPage.vue'),
    meta: { title: '材料上传', requiresAuth: true, roles: ['resident', 'family', 'ROLE_RESIDENT', 'ROLE_FAMILY'] }
  },
  {
    path: '/workorder',
    name: 'WorkOrderManage',
    component: () => import('@/pages/WorkOrderManagePage.vue'),
    meta: { title: '工单管理', requiresAuth: true, roles: ['staff', 'admin', 'ROLE_STAFF', 'ROLE_ADMIN'] }
  },
  {
    path: '/staff/workbench',
    name: 'StaffWorkbench',
    component: () => import('@/pages/StaffWorkbenchPage.vue'),
    meta: { title: '工作人员工作台', requiresAuth: true, roles: ['staff', 'admin', 'ROLE_STAFF', 'ROLE_ADMIN'] }
  },
  {
    path: '/staff/booking',
    name: 'ServiceBookingsStaff',
    component: () => import('@/pages/ServiceBookingsStaffPage.vue'),
    meta: { title: '服务调度', requiresAuth: true, roles: ['staff', 'admin', 'ROLE_STAFF', 'ROLE_ADMIN'] }
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/pages/AdminDashboardPage.vue'),
    meta: { title: '后台管理', requiresAuth: true, roles: ['admin', 'ROLE_ADMIN'] }
  },
  {
    path: '/admin/service-config',
    name: 'AdminServiceConfig',
    component: () => import('@/pages/AdminServiceConfigPage.vue'),
    meta: { title: '事项配置', requiresAuth: true, roles: ['admin', 'ROLE_ADMIN'] }
  },
  {
    path: '/admin/service-resource',
    name: 'ServiceResource',
    component: () => import('@/pages/ServiceResourcePage.vue'),
    meta: { title: '服务资源', requiresAuth: true, roles: ['admin', 'ROLE_ADMIN'] }
  },
  {
    path: '/admin/user-manage',
    name: 'AdminUserManage',
    component: () => import('@/pages/AdminUserManagePage.vue'),
    meta: { title: '用户管理', requiresAuth: true, roles: ['admin', 'ROLE_ADMIN'] }
  },
  {
    path: '/admin/statistics',
    name: 'StatisticsDashboard',
    component: () => import('@/pages/StatisticsDashboardPage.vue'),
    meta: { title: '统计分析', requiresAuth: true, roles: ['admin', 'ROLE_ADMIN'] }
  },
  {
    path: '/service-feedback',
    name: 'ServiceFeedback',
    component: () => import('@/pages/ServiceFeedbackPage.vue'),
    meta: { title: '服务评价', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.onError((error, to) => {
  const message = error instanceof Error ? error.message : String(error)
  if (message.includes('Failed to fetch dynamically imported module') || message.includes('ERR_CACHE_READ_FAILURE')) {
    window.location.href = to.fullPath
  }
})

function normalizeRole(role?: string) {
  if (!role) return ''
  return role.startsWith('ROLE_') ? role : `ROLE_${role.toUpperCase()}`
}

router.beforeEach((to, _from, next) => {
  document.title = `${String(to.meta.title || '社区服务平台')} - 社区服务协同平台`

  const authStore = useAuthStore()
  const isLoggedIn = !!authStore.token

  if (to.meta.requiresAuth !== false && !isLoggedIn) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    next('/home')
    return
  }

  if (to.meta.roles && isLoggedIn) {
    const allowedRoles = to.meta.roles as string[]
    const normalizedUserRole = normalizeRole(authStore.userInfo?.role)
    const hasPermission = allowedRoles.some(role => normalizeRole(role) === normalizedUserRole)

    if (!hasPermission) {
      next('/home')
      return
    }
  }

  next()
})

export default router

