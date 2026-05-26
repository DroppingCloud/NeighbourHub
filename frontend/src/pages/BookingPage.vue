<template>
  <el-row :gutter="20">
    <el-col :span="10">
      <el-card header="发起服务预约">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="服务类型" prop="serviceType">
            <el-select v-model="form.serviceType" placeholder="请选择服务类型" style="width:100%">
              <el-option v-for="s in dictStore.dicts.serviceType" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="期望时间" prop="expectTime">
            <el-date-picker v-model="form.expectTime" type="datetime" placeholder="请选择服务时间" style="width:100%" />
          </el-form-item>
          <el-form-item label="服务地址">
            <el-input v-model="form.address" placeholder="请输入服务地址" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" placeholder="其他特殊需求" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交预约</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="14">
      <el-card header="我的预约">
        <el-table :data="bookingList" v-loading="listLoading" stripe>
          <el-table-column prop="serviceTypeLabel" label="服务类型" />
          <el-table-column prop="expectTime" label="期望时间" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <TaskStatusTag :status="row.status" type="booking" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button v-if="row.status === 'pending'" type="danger" link size="small" @click="cancelBooking(row.bookingId)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createBooking, getBookingList, cancelBooking as cancelApi } from '@/api/booking'
import { useDictStore } from '@/stores/dictStore'
import TaskStatusTag from '@/components/TaskStatusTag.vue'
import { ElMessage } from 'element-plus'

const dictStore = useDictStore()
const submitting = ref(false)
const listLoading = ref(false)
const bookingList = ref<any[]>([])
const formRef = ref()
const form = ref({ serviceType: '', expectTime: '', address: '', remark: '' })
const rules = {
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  expectTime: [{ required: true, message: '请选择期望时间', trigger: 'change' }]
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await createBooking(form.value)
    ElMessage.success('预约提交成功')
    loadBookings()
  } finally {
    submitting.value = false
  }
}

const cancelBooking = async (id: number) => {
  await cancelApi(id)
  ElMessage.success('预约已取消')
  loadBookings()
}

const loadBookings = async () => {
  listLoading.value = true
  try {
    const res = await getBookingList()
    bookingList.value = res.records || []
  } finally {
    listLoading.value = false
  }
}

onMounted(loadBookings)
</script>
