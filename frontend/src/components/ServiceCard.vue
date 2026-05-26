<template>
  <el-card class="service-card" shadow="hover">
    <div class="card-header">
      <el-tag size="small">{{ item.category }}</el-tag>
      <span class="item-name">{{ item.itemName }}</span>
    </div>
    <p class="item-desc">{{ item.description }}</p>
    <div class="card-footer">
      <el-button type="primary" size="small" @click="$emit('apply', item.itemId)">立即申请</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  item: {
    itemId: number
    itemName: string
    category: string
    description?: string
  }
}>()

defineEmits<{ apply: [itemId: number] }>()
</script>

<style scoped>
.service-card { margin-bottom: 12px; cursor: pointer; }
.card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.item-name { font-weight: 500; font-size: 15px; }
.item-desc { color: #909399; font-size: 13px; margin-bottom: 12px; }
.card-footer { text-align: right; }
</style>
