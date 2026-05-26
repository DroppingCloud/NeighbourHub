<template>
  <el-card v-loading="loading">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>申请详情</span>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </template>
    <template v-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请事项">{{ detail.itemName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <TaskStatusTag :status="detail.status" type="application" />
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.submitTime }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>审核进度</el-divider>
      <el-timeline>
        <el-timeline-item timestamp="提交申请" placement="top" type="primary">申请已提交，等待审核</el-timeline-item>
        <el-timeline-item v-if="detail.status !== 'pending'" :type="getTimelineType(detail.status)" timestamp="审核结果" placement="top">
          {{ getStatusText(detail.status) }}
        </el-timeline-item>
      </el-timeline>
      <div v-if="detail.status === 'supplement_required'" style="margin-top:16px">
        <el-alert title="需要补件，请重新上传材料并提交。" type="warning" />
        <el-button type="primary" style="margin-top:12px" @click="$router.push(`/application/submit?resubmit=${detail.applicationId}`)">
          去补件
        </el-button>
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getApplicationDetail } from '@/api/application'
import TaskStatusTag from '@/components/TaskStatusTag.vue'

const route = useRoute()
const loading = ref(false)
const detail = ref<any>(null)

const getTimelineType = (s: string) => s === 'approved' || s === 'completed' ? 'success' : s === 'rejected' ? 'danger' : 'warning'
const getStatusText = (s: string) => ({ approved: '审核通过', rejected: '已驳回', supplement_required: '需补件', completed: '已办结' })[s] || s

onMounted(async () => {
  loading.value = true
  try { detail.value = await getApplicationDetail(Number(route.params.id)) }
  finally { loading.value = false }
})
</script>
