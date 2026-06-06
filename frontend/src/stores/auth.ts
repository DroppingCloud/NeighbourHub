import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  type AuthUserInfo = {
    userId: string
    username: string
    phone: string
    role: string
    realName?: string
    avatar?: string
    staffType?: string
  }

  const savedUserInfo = localStorage.getItem('userInfo')
  const userInfo = ref<AuthUserInfo | null>(savedUserInfo ? JSON.parse(savedUserInfo) : null)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserInfo(info: typeof userInfo.value) {
    userInfo.value = info
    if (info) {
      localStorage.setItem('userInfo', JSON.stringify(info))
    } else {
      localStorage.removeItem('userInfo')
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
