import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProxyRelations, type ProxyRelationVO } from '@/api/user'

export interface ProxyTarget {
  id: number          // 绑定关系ID
  profileId: number   // 被代理人档案ID
  targetUserId: number // 被代理人用户ID（用于 _proxyFor 参数）
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
    const list: ProxyRelationVO[] = await getProxyRelations()
    // 过滤出状态为 active 的绑定关系，并转换为 ProxyTarget
    // 注意：需要从后端获取目标居民的姓名，可能需要额外接口，这里假设 ProxyRelationVO 已包含 targetProfileName
    targets.value = list
      .filter(item => item.status === 'active')
      .map(item => ({
        id: item.id,
        profileId: item.targetProfileId!,
        targetUserId: item.targetUserId!,
        name: item.targetProfileName || `被代理人${item.targetProfileId}`,
        relation: item.relation || '家属',
        authorizedActions: item.authorizedActions || ''
      }))
    // 如果当前没有选中且存在目标，默认选中第一个（可选）
    if (!currentTarget.value && targets.value.length > 0) {
      currentTarget.value = targets.value[0]
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
      if (found) currentTarget.value = found
    }
  }

  return { currentTarget, targets, loadTargets, setCurrentTarget, clearTarget, restoreTarget }
})
