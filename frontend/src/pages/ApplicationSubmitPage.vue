<template>
  <el-card header="发起事项申请">
    <el-steps :active="step" finish-status="success" style="margin-bottom:24px">
      <el-step title="选择事项" />
      <el-step title="填写信息" />
      <el-step title="上传材料" />
      <el-step title="提交确认" />
    </el-steps>

    <!-- Step 0: 选择事项 -->
    <div v-if="step === 0">
      <el-form-item label="搜索事项">
        <el-input v-model="searchText" placeholder="输入事项名称搜索" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="8" v-for="item in filteredItems" :key="item.itemId">
          <ServiceCard :item="item" @apply="selectItem(item)" />
        </el-col>
      </el-row>
    </div>

    <!-- Step 1: 填写表单 -->
    <div v-if="step === 1">
      <el-alert :title="`正在申请：${selectedItem?.itemName}`" type="info" :closable="false" style="margin-bottom:16px" />
      <el-form ref="formRef" :model="formData" label-width="120px">
        <el-form-item label="备注说明">
          <el-input v-model="formData.remark" type="textarea" placeholder="如有特殊情况请说明" />
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2: 上传材料 -->
    <div v-if="step === 2">
      <MaterialUploadPanel @uploaded="handleUploaded" />
    </div>

    <!-- Step 3: 确认提交 -->
    <div v-if="step === 3">
      <el-descriptions title="申请信息确认" :column="2" border>
        <el-descriptions-item label="申请事项">{{ selectedItem?.itemName }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ formData.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert title="提交后将生成工单，请等待工作人员审核。" type="warning" style="margin-top:16px" />
    </div>

    <div class="step-btns">
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" @click="step++" :disabled="step === 0 && !selectedItem">下一步</el-button>
      <el-button v-if="step === 3" type="primary" :loading="submitting" @click="handleSubmit">确认提交</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { submitApplication } from '@/api/application'
import { getServiceItemList } from '@/api/serviceItem'
import ServiceCard from '@/components/ServiceCard.vue'
import MaterialUploadPanel from '@/components/MaterialUploadPanel.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const step = ref(0)
const searchText = ref('')
const items = ref<any[]>([])
const selectedItem = ref<any>(null)
const formData = ref({ remark: '' })
const submitting = ref(false)

const filteredItems = computed(() =>
  items.value.filter(i => i.itemName.includes(searchText.value))
)

const selectItem = (item: any) => { selectedItem.value = item; step.value++ }
const handleUploaded = (files: any[]) => { console.log('已上传', files) }

const handleSubmit = async () => {
  if (!selectedItem.value) return
  submitting.value = true
  try {
    const id = await submitApplication({ itemId: selectedItem.value.itemId, remark: formData.value.remark })
    ElMessage.success('申请提交成功')
    router.push(`/application/${id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const res = await getServiceItemList({ status: 'online', pageSize: 100 })
  items.value = res.records || []
})
</script>

<style scoped>
.step-btns { margin-top: 24px; display: flex; justify-content: center; gap: 12px; }
</style>
