<template>
  <div class="booking-container">
    <div class="page-header">
      <h2>社区服务预约</h2>
      <p>预约助餐、陪诊、上门等社区便民服务</p>
    </div>

    <div class="service-types">
      <div
        v-for="type in serviceTypes"
        :key="type.value"
        class="type-card"
        :class="{ active: selectedType === type.value }"
        @click="selectedType = type.value"
      >
        <div class="type-name">{{ type.label }}</div>
        <div class="type-desc">{{ type.desc }}</div>
      </div>
    </div>

    <div class="booking-form">
      <h3>填写预约信息</h3>
      <el-form :model="bookingForm" :rules="formRules" ref="formRef" label-width="6rem">
        <el-form-item label="服务类型">
          <div class="form-control">{{ getServiceTypeName }}</div>
        </el-form-item>
        <el-form-item label="预约时间" prop="expectTime">
          <el-date-picker
            v-model="bookingForm.expectTime"
            type="datetime"
            placeholder="选择期望服务时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="服务地址" prop="address">
          <el-input v-model="bookingForm.address" placeholder="请输入详细服务地址" />
        </el-form-item>
        <el-form-item label="特殊需求">
          <el-input v-model="bookingForm.remark" type="textarea" :rows="3" placeholder="如有特殊需求，请在此说明" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" class="submit-btn" @click="submitBooking">
            提交预约
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBooking } from '@/api/booking'

type ServiceType = 'dining' | 'accompany' | 'home_visit'
const route = useRoute()

const serviceTypes = [
  { value: 'dining' as const, label: '助餐服务', desc: '社区食堂配送到家' },
  { value: 'accompany' as const, label: '陪诊服务', desc: '陪同就医、取药' },
  { value: 'home_visit' as const, label: '上门服务', desc: '家政清洁、维修探访' }
]

const selectedType = ref<ServiceType>('dining')
const submitting = ref(false)
const formRef = ref()
const bookingForm = ref({
  expectTime: '',
  address: '',
  remark: ''
})

const getServiceTypeName = computed(() => serviceTypes.find(t => t.value === selectedType.value)?.label || '')

const formRules = {
  expectTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  address: [{ required: true, message: '请输入服务地址', trigger: 'blur' }]
}

async function submitBooking() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      await createBooking({
        serviceType: selectedType.value,
        expectTime: bookingForm.value.expectTime,
        address: bookingForm.value.address,
        remark: bookingForm.value.remark
      })
      bookingForm.value = { expectTime: '', address: '', remark: '' }
      ElMessage.success('预约提交成功')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  const queryType = String(route.query.serviceType || '')
  if (serviceTypes.some(type => type.value === queryType)) {
    selectedType.value = queryType as ServiceType
  }
})
</script>

<style scoped>
.booking-container {
  width: 100%;
  max-width: 56.25rem;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  color: var(--text-muted);
}

.service-types {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(8.75rem, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.type-card {
  background: var(--card-bg);
  border-radius: 0.75rem;
  padding: 1rem 0.75rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 0.125rem solid transparent;
  box-shadow: var(--shadow-sm);
}

.type-card.active {
  border-color: var(--gold);
  background: rgba(212, 168, 67, 0.08);
}

.type-name {
  font-size: 0.9375rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
  color: var(--text-primary);
}

.type-desc {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.booking-form {
  background: var(--card-bg);
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: var(--shadow-sm);
}

.booking-form h3 {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
  color: var(--text-primary);
  padding-bottom: 0.5rem;
  border-bottom: 0.0625rem solid var(--border-color);
}

.full-width {
  width: 100%;
}

.form-control {
  width: 100%;
  padding: 0.5rem 0.75rem;
  background: var(--bg-tertiary);
  border-radius: 0.375rem;
  color: var(--text-primary);
  border: 0.0625rem solid var(--border-color);
}

.submit-btn {
  width: 100%;
  min-height: 2.75rem;
}
</style>
