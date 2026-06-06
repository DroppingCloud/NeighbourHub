import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { useProxyStore } from '@/stores/proxy'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000
})

let _lastToast = { msg: '', ts: 0 }

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 家属代办：如果当前有代办目标，自动添加 _proxyFor 参数
    // 可以通过设置请求头 `X-Skip-Proxy: true` 来跳过该自动添加（用于通知等严格针对登录用户的 API）
    const authStore = useAuthStore()
    const proxyStore = useProxyStore()
    const skipProxy = config.headers && (config.headers as any)['X-Skip-Proxy'] === 'true'
    const role = normalizeRole(authStore.userInfo?.role || '')
    const currentTarget = proxyStore.currentTarget
    const targetStillValid = !!currentTarget && proxyStore.targets.some(target => target.id === currentTarget.id)

    if (currentTarget && (role !== 'family' || !targetStillValid)) {
      proxyStore.setCurrentTarget(null)
    }

    if (role === 'family' && currentTarget && targetStillValid && !skipProxy) {
      // 后端统一通过 @RequestParam("_proxyFor") 读取代办目标居民档案 ID。
      config.params = { ...config.params, _proxyFor: currentTarget.profileId }
    }
    return config
  },
  (error) => Promise.reject(error)
)

function normalizeRole(role: string) {
  return role.replace(/^ROLE_/, '').toLowerCase()
}


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
      const m = message || '权限不足，无法访问该功能'
      if (m !== _lastToast.msg || Date.now() - _lastToast.ts > 3000) {
        ElMessage.error(m)
        _lastToast = { msg: m, ts: Date.now() }
      }
    } else {
      const m = message || '请求失败'
      if (m !== _lastToast.msg || Date.now() - _lastToast.ts > 3000) {
        ElMessage.error(m)
        _lastToast = { msg: m, ts: Date.now() }
      }
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
      const m = message || error.message || '网络异常，请稍后重试'
      if (m !== _lastToast.msg || Date.now() - _lastToast.ts > 3000) {
        ElMessage.error(m)
        _lastToast = { msg: m, ts: Date.now() }
      }
    }
    return Promise.reject(error)
  }
)

export default request
