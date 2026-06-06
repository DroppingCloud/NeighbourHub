// src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMe } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const userInfo = ref<any>(null)

  // 修改：支持记住我参数
  function setToken(newToken: string, remember: boolean = false) {
    token.value = newToken
    
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('token', newToken)
    
    if (remember) {
      // 设置7天过期时间
      const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000
      localStorage.setItem('expiresAt', expiresAt.toString())
    } else {
      // 清除可能残留的 localStorage 数据
      localStorage.removeItem('token')
      localStorage.removeItem('expiresAt')
    }
  }

  // 新增：初始化认证状态
  function initAuth(): boolean {
    // 优先从 localStorage 读取
    let storedToken = localStorage.getItem('token')
    let expiresAt = localStorage.getItem('expiresAt')
    
    // 检查 localStorage token 是否过期
    if (expiresAt && Date.now() > parseInt(expiresAt)) {
      // token 已过期，清除
      localStorage.removeItem('token')
      localStorage.removeItem('expiresAt')
      storedToken = null
    }
    
    // 如果 localStorage 没有，再从 sessionStorage 读取
    if (!storedToken) {
      storedToken = sessionStorage.getItem('token')
    }
    
    if (storedToken) {
      token.value = storedToken
      return true
    }
    return false
  }

  // 新增：恢复用户信息（从 storage 中读取）
  function restoreUserInfo() {
    const storage = localStorage.getItem('token') ? localStorage : sessionStorage
    const savedUserInfo = storage.getItem('userInfo')
    if (savedUserInfo) {
      try {
        userInfo.value = JSON.parse(savedUserInfo)
      } catch (e) {
        console.error('Failed to parse userInfo', e)
      }
    }
  }

  function setUserInfo(info: any) {
    userInfo.value = info
    // 将用户信息也存储到对应的 storage 中
    if (token.value) {
      const storage = localStorage.getItem('token') ? localStorage : sessionStorage
      storage.setItem('userInfo', JSON.stringify(info))
    }
  }

  function logout() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('expiresAt')
    localStorage.removeItem('userInfo')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  // 新增：刷新 token（可选，如果后端支持）
  async function refreshToken() {
    // 如果后端有刷新 token 的接口，在这里实现
    // 否则可以重新调用 getMe 来验证 token 有效性
    try {
      const userInfoData = await getMe()
      setUserInfo(userInfoData)
      return true
    } catch (error) {
      logout()
      return false
    }
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    initAuth,
    restoreUserInfo,
    logout,
    refreshToken
  }
})