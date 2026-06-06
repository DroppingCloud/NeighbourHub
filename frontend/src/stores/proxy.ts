import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProxyRelations, type ProxyRelationVO } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

export interface ProxyTarget {
  id: number          // 绑定关系ID
  profileId: number   // 被代理人档案ID
  targetUserId: number // 被代理人用户ID
  name: string        // 被代理人姓名（从档案获取）
  relation: string    // 关系
  authorizedActions: string  // 授权范围
}

export const useProxyStore = defineStore('proxy', () => {
  // 当前选中的代办目标（null 表示本人）
  const currentTarget = ref<ProxyTarget | null>(null)
  // 可选的代办目标列表（家属绑定且状态为 active）
  const targets = ref<ProxyTarget[]>([])

  // 加载家属绑定的被代理人列表
  async function loadTargets() {
    const authStore = useAuthStore()
    const currentUserId = Number(authStore.userInfo?.userId || 0)
    const currentRole = normalizeRole(authStore.userInfo?.role || '')
    if (currentRole !== 'family' || !currentUserId) {
      setCurrentTarget(null)
      targets.value = []
      return
    }

    const list: ProxyRelationVO[] = await getProxyRelations()
    // 只把“当前家属作为代理人”的 active 关系作为可代办目标。
    // 居民视角也会返回绑定关系，但不能放入代办目标，否则会把居民自己的请求误加 _proxyFor。
    targets.value = list
      .filter(item => item.status === 'active' && Number(item.proxyUserId) === currentUserId)
      .map(item => ({
        id: item.id,
        profileId: item.targetProfileId!,
        targetUserId: item.targetUserId!,
        name: item.targetProfileName || `被代理人${item.targetProfileId}`,
        relation: item.relation || '家属',
        authorizedActions: item.authorizedActions || ''
      }))

    if (currentTarget.value && !targets.value.some(target => target.id === currentTarget.value?.id)) {
      setCurrentTarget(null)
    }
  }

  // 切换代办目标
  function setCurrentTarget(target: ProxyTarget | null) {
    currentTarget.value = target
    // 可选：存入 localStorage，刷新后保持选择
    if (target) {
      localStorage.setItem('currentProxyId', String(target.id))
    } else {
      localStorage.removeItem('currentProxyId')
    }
  }

  function clearTarget() {
    setCurrentTarget(null)
    targets.value = []
  }

  // 恢复上次选择的代办目标
  async function restoreTarget() {
    await loadTargets()
    const savedId = localStorage.getItem('currentProxyId')
    if (savedId) {
      const found = targets.value.find(t => String(t.id) === savedId)
      if (found) {
        currentTarget.value = found
      } else {
        setCurrentTarget(null)
      }
    }
  }

  return { currentTarget, targets, loadTargets, setCurrentTarget, clearTarget, restoreTarget }
})

function normalizeRole(role: string) {
  return role.replace(/^ROLE_/, '').toLowerCase()
}
