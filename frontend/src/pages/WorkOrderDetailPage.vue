<template>
  <el-row :gutter="20" v-loading="loading">
    <el-col :span="14">
      <el-card header="工单信息" v-if="order">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ order.orderId }}</el-descriptions-item>
          <el-descriptions-item label="申请事项">{{ order.itemName }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ order.residentName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <TaskStatusTag :status="order.status" type="workorder" />
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="审核意见">{{ order.auditOpinion || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-col>
    <el-col :span="10">
      <el-card header="审核操作">
        <el-form :model="auditForm" label-width="80px">
          <el-form-item label="审核动作">
            <el-radio-group v-model="auditForm.action">
              <el-radio value="approved">通过</el-radio>
              <el-radio value="supplement_required">退回补件</el-radio>
              <el-radio value="rejected">驳回</el-radio>
              <el-radio value="completed">办结</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核意见">
            <el-input v-model="auditForm.opinion" type="textarea" placeholder="请填写审核意见" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleAudit">提交审核</el-button>
            <el-button @click="$router.back()">返回</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWorkOrderDetail, auditWorkOrder } from '@/api/workOrder'
import TaskStatusTag from '@/components/TaskStatusTag.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const order = ref<any>(null)
const auditForm = ref({ action: 'approved' as any, opinion: '' })

const handleAudit = async () => {
  submitting.value = true
  try {
    await auditWorkOrder({ orderId: Number(route.params.id), ...auditForm.value })
    ElMessage.success('审核操作已提交')
    router.push('/workorder')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try { order.value = await getWorkOrderDetail(Number(route.params.id)) }
  finally { loading.value = false }
})
</script>
