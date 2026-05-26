import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface MenuItem {
  path: string
  title: string
  icon?: string
  roles: string[]
  children?: MenuItem[]
}

export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<MenuItem[]>([])
  const permissions = ref<string[]>([])

  // 菜单配置，根据角色过滤
  const allMenus: MenuItem[] = [
    { path: '/home', title: '首页', icon: 'House', roles: ['ROLE_RESIDENT', 'ROLE_FAMILY', 'ROLE_STAFF', 'ROLE_ADMIN'] },
    { path: '/guide', title: '智能导办', icon: 'MagicStick', roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] },
    { path: '/application/list', title: '我的申请', icon: 'Document', roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] },
    { path: '/booking', title: '服务预约', icon: 'Calendar', roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] },
    { path: '/progress', title: '进度查询', icon: 'Search', roles: ['ROLE_RESIDENT', 'ROLE_FAMILY'] },
    { path: '/notice', title: '消息通知', icon: 'Bell', roles: ['ROLE_RESIDENT', 'ROLE_FAMILY', 'ROLE_STAFF', 'ROLE_ADMIN'] },
    { path: '/workorder', title: '工单管理', icon: 'Files', roles: ['ROLE_STAFF', 'ROLE_ADMIN'] },
    { path: '/admin/dashboard', title: '统计看板', icon: 'DataAnalysis', roles: ['ROLE_ADMIN'] },
    { path: '/admin/service-config', title: '事项配置', icon: 'Setting', roles: ['ROLE_ADMIN'] },
    { path: '/admin/user-manage', title: '用户管理', icon: 'User', roles: ['ROLE_ADMIN'] },
  ]

  function loadPermissions(userRoles: string[]) {
    menus.value = allMenus.filter(menu =>
      menu.roles.some(role => userRoles.includes(role))
    )
  }

  return { menus, permissions, loadPermissions }
})
