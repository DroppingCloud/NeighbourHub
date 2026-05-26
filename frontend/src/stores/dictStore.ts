import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDictStore = defineStore('dict', () => {
  const dicts = ref<Record<string, Array<{ label: string; value: string; color?: string }>>>({
    applicationStatus: [
      { label: '待审核', value: 'pending', color: 'warning' },
      { label: '已通过', value: 'approved', color: 'success' },
      { label: '已驳回', value: 'rejected', color: 'danger' },
      { label: '需补件', value: 'supplement_required', color: 'warning' },
      { label: '补件中', value: 'supplementing', color: 'info' },
      { label: '已办结', value: 'completed', color: 'success' }
    ],
    bookingStatus: [
      { label: '待确认', value: 'pending', color: 'info' },
      { label: '已确认', value: 'confirmed', color: 'primary' },
      { label: '服务中', value: 'in_progress', color: 'warning' },
      { label: '已完成', value: 'completed', color: 'success' },
      { label: '已取消', value: 'cancelled', color: 'info' }
    ],
    serviceType: [
      { label: '助餐服务', value: 'dining' },
      { label: '陪诊服务', value: 'accompany' },
      { label: '上门服务', value: 'home_visit' }
    ]
  })

  function getDictLabel(type: string, value: string): string {
    const items = dicts.value[type] || []
    return items.find(i => i.value === value)?.label || value
  }

  function getDictColor(type: string, value: string): string {
    const items = dicts.value[type] || []
    return items.find(i => i.value === value)?.color || 'info'
  }

  return { dicts, getDictLabel, getDictColor }
})
