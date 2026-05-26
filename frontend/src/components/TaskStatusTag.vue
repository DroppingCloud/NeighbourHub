<template>
  <el-tag :type="tagType" size="small">{{ tagLabel }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  type?: 'application' | 'workorder' | 'booking'
}>()

const statusMap: Record<string, { label: string; color: string }> = {
  // 申请/工单状态
  pending: { label: '待审核', color: 'warning' },
  approved: { label: '已通过', color: 'success' },
  rejected: { label: '已驳回', color: 'danger' },
  supplement_required: { label: '需补件', color: 'warning' },
  supplementing: { label: '补件中', color: '' },
  completed: { label: '已办结', color: 'success' },
  // 预约状态
  confirmed: { label: '已确认', color: '' },
  in_progress: { label: '服务中', color: 'warning' },
  cancelled: { label: '已取消', color: 'info' }
}

const tagLabel = computed(() => statusMap[props.status]?.label || props.status)
const tagType = computed(() => (statusMap[props.status]?.color || '') as any)
</script>
