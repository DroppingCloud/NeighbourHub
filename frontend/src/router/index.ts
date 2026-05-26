import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/DefaultLayout.vue'),
    children: [
      {
        path: 'home',
        component: () => import('@/pages/HomePage.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'guide',
        component: () => import('@/pages/GuidePage.vue'),
        meta: { title: '智能导办', requiresAuth: true, roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] }
      },
      {
        path: 'application/submit',
        component: () => import('@/pages/ApplicationSubmitPage.vue'),
        meta: { title: '提交申请', requiresAuth: true, roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] }
      },
      {
        path: 'application/list',
        component: () => import('@/pages/ApplicationListPage.vue'),
        meta: { title: '我的申请', requiresAuth: true, roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] }
      },
      {
        path: 'application/:id',
        component: () => import('@/pages/ApplicationDetailPage.vue'),
        meta: { title: '申请详情', requiresAuth: true, roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] }
      },
      {
        path: 'booking',
        component: () => import('@/pages/BookingPage.vue'),
        meta: { title: '服务预约', requiresAuth: true, roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] }
      },
      {
        path: 'progress',
        component: () => import('@/pages/ProgressPage.vue'),
        meta: { title: '进度查询', requiresAuth: true, roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] }
      },
      {
        path: 'notice',
        component: () => import('@/pages/NoticePage.vue'),
        meta: { title: '消息通知', requiresAuth: true }
      },
      {
        path: 'workorder',
        component: () => import('@/pages/WorkOrderManagePage.vue'),
        meta: { title: '工单管理', requiresAuth: true, roles: ['ROLE_STAFF', 'ROLE_ADMIN'] }
      },
      {
        path: 'workorder/:id',
        component: () => import('@/pages/WorkOrderDetailPage.vue'),
        meta: { title: '工单详情', requiresAuth: true, roles: ['ROLE_STAFF', 'ROLE_ADMIN'] }
      },
      {
        path: 'admin',
        redirect: '/admin/dashboard'
      },
      {
        path: 'admin/dashboard',
        component: () => import('@/pages/StatisticsDashboardPage.vue'),
        meta: { title: '统计看板', requiresAuth: true, roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'admin/service-config',
        component: () => import('@/pages/AdminServiceConfigPage.vue'),
        meta: { title: '事项配置', requiresAuth: true, roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'admin/user-manage',
        component: () => import('@/pages/AdminUserManagePage.vue'),
        meta: { title: '用户管理', requiresAuth: true, roles: ['ROLE_ADMIN'] }
      }
    ]
  },
  {
    path: '/403',
    component: () => import('@/pages/ForbiddenPage.vue'),
    meta: { title: '无权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/pages/NotFoundPage.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  document.title = `${to.meta.title || '社区服务平台'} - 社区服务协同平台`

  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !userStore.isLoggedIn) {
    return next('/login')
  }

  if (userStore.isLoggedIn && to.path === '/login') {
    return next('/home')
  }

  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const hasPermission = requiredRoles.some(role => userStore.hasRole(role))
    if (!hasPermission) {
      return next('/403')
    }
  }

  next()
})

export default router
