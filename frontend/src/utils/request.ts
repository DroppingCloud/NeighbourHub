import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { useProxyStore } from '@/stores/proxy'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 家属代办：如果当前有代办目标，自动添加 _proxyFor 参数
    const proxyStore = useProxyStore()
    if (proxyStore.currentTarget) {
      // 添加到 URL 参数中（GET 请求）或请求体（POST/PUT）
      if (config.method === 'get') {
        config.params = { ...config.params, _proxyFor: proxyStore.currentTarget.profileId }
      } else {
        // 对于 POST/PUT 等，添加到 data 中（后端需要从请求体解析）
        if (config.data && typeof config.data === 'object') {
          config.data._proxyFor = proxyStore.currentTarget.profileId
        } else {
          config.data = { _proxyFor: proxyStore.currentTarget.profileId }
        }
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)


function clearLoginAndRedirect(message: string) {
  const authStore = useAuthStore()
  authStore.logout()
  ElMessage.error(message)
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
}

request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    if (code === 401) {
      clearLoginAndRedirect(message || '登录已过期，请重新登录')
    } else if (code === 403) {
      ElMessage.error(message || '权限不足，无法访问该功能')
    } else {
      ElMessage.error(message || '请求失败')
    }
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 401) {
      clearLoginAndRedirect(message || '登录已过期，请重新登录')
    } else if (status === 403) {
      clearLoginAndRedirect(message || '登录状态无效，请重新登录')
    } else {
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
