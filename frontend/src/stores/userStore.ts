import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'
import type { LoginRequest } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const roles = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  const hasRole = (role: string) => roles.value.includes(role)

  const isAdmin = computed(() => hasRole('ROLE_ADMIN'))
  const isStaff = computed(() => hasRole('ROLE_STAFF'))
  const isResident = computed(() => hasRole('ROLE_RESIDENT'))

  async function login(loginData: LoginRequest) {
    const data = await loginApi(loginData)
    token.value = data.token
    roles.value = data.roles
    userInfo.value = { userId: data.userId, username: data.username }
    localStorage.setItem('token', data.token)
  }

  async function fetchUserInfo() {
    const data = await getMe()
    userInfo.value = data
    roles.value = data.roles || []
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      roles.value = []
      localStorage.removeItem('token')
    }
  }

  return { token, userInfo, roles, isLoggedIn, isAdmin, isStaff, isResident, hasRole, login, logout, fetchUserInfo }
})
