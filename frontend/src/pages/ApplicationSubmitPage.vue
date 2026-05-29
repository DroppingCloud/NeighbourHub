<template>
  <div class="service-apply-container">
    <div class="page-header">
      <h2>事项办理</h2>
      <p>选择您需要办理的政务服务事项</p>
    </div>

    <div class="service-grid" v-loading="loading">
      <div
        v-for="service in govServices"
        :key="service.id"
        class="service-card"
        :class="{ active: selectedService?.id === service.id }"
        @click="selectService(service)"
      >
        <div class="service-icon">
          <el-icon :size="28"><Document /></el-icon>
        </div>
        <div class="service-name">{{ service.name }}</div>
        <div class="service-category">{{ service.category }}</div>
      </div>
    </div>

    <el-empty v-if="!loading && govServices.length === 0" description="暂无可办理事项" />

    <el-drawer
      v-model="showFormDrawer"
      :title="selectedService?.name"
      direction="rtl"
      size="1000px"
      :modal="true"
    >
      <div v-if="selectedService" class="apply-form">
        <div class="form-section">
          <h4>办理条件</h4>
          <div class="condition-box">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ selectedService.conditions }}</span>
          </div>
        </div>

        <div class="form-section">
          <h4>所需材料</h4>
          <div class="material-list">
            <div v-for="material in selectedService.materials" :key="material" class="material-item">
              <el-icon><Check /></el-icon>
              <span>{{ material }}</span>
            </div>
          </div>
        </div>

        <div class="form-section">
          <h4>办理流程</h4>
          <div class="custom-steps">
            <div
              v-for="(step, index) in selectedService.process"
              :key="index"
              class="step-item"
            >
              <div class="step-number">{{ index + 1 }}</div>
              <div class="step-title">{{ step }}</div>
              <div v-if="index < selectedService.process.length - 1" class="step-line"></div>
            </div>
          </div>
        </div>

        <div class="form-section">
          <h4>填写申请信息</h4>
          <el-form :model="applyForm" :rules="formRules" ref="formRef" label-width="150px">
            <el-form-item label="申请人" prop="name">
              <el-input v-model="applyForm.name" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="applyForm.idCard" placeholder="请输入18位身份证号" />
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="applyForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="居住地址" prop="address">
              <el-input v-model="applyForm.address" placeholder="请输入详细居住地址" />
            </el-form-item>
          </el-form>
        </div>

        <div class="form-actions">
          <el-button @click="showFormDrawer = false">取消</el-button>
          <el-button type="primary" :loading="nextLoading" @click="nextStep">
            下一步：上传材料
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Document, InfoFilled, Check } from '@element-plus/icons-vue'
import { getPublicMaterialTemplates, getPublicServiceItemList } from '@/api/serviceItem'

const router = useRouter()
const govServices = ref<any[]>([])
const selectedService = ref<any>(null)
const showFormDrawer = ref(false)
const nextLoading = ref(false)
const loading = ref(false)
const formRef = ref()

const applyForm = ref({
  name: '',
  idCard: '',
  phone: '',
  address: ''
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [
    {
      required: true,
      pattern: /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/,
      message: '请输入正确的身份证号',
      trigger: 'blur'
    }
  ],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

function selectService(service: any) {
  selectedService.value = service
  showFormDrawer.value = true
  applyForm.value = { name: '', idCard: '', phone: '', address: '' }
}

async function loadServices() {
  loading.value = true
  try {
    const page = await getPublicServiceItemList({ status: 'online', pageNum: 1, pageSize: 50 })
    const records = Array.isArray(page) ? page : page?.records || page?.list || page?.rows || []
    govServices.value = records.map((item: any) => ({
      id: item.itemId,
      name: item.itemName,
      category: item.category,
      conditions: item.conditions || item.description || '请按事项要求准备材料',
      materials: ['身份证明材料', '申请表'],
      process: parseSteps(item.processSteps)
    }))
  } finally {
    loading.value = false
  }
}

function parseSteps(value?: string) {
  const fallback = ['填写信息', '上传材料', '提交申请', '等待审核']
  if (!value) return fallback
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : fallback
  } catch {
    return String(value).split(/[,，\n]/).map(item => item.trim()).filter(Boolean)
  }
}

async function nextStep() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return

    nextLoading.value = true
    try {
      const templates = await getPublicMaterialTemplates(Number(selectedService.value.id)).catch(() => [])
      const materialNames = templates.length
        ? templates.map(item => item.materialName)
        : selectedService.value.materials

      const tempData = {
        serviceId: selectedService.value.id,
        serviceName: selectedService.value.name,
        serviceConditions: selectedService.value.conditions,
        serviceMaterials: materialNames,
        serviceProcess: selectedService.value.process,
        formData: {
          name: applyForm.value.name,
          idCard: applyForm.value.idCard,
          phone: applyForm.value.phone,
          address: applyForm.value.address
        }
      }
      sessionStorage.setItem('tempApplication', JSON.stringify(tempData))
      showFormDrawer.value = false
      router.push({
        path: '/material-upload',
        query: { serviceId: selectedService.value.id }
      })
    } finally {
      nextLoading.value = false
    }
  })
}

onMounted(loadServices)
</script>

<style scoped>
.service-apply-container {
  max-width: 75rem;
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
  font-size: 0.875rem;
  color: var(--text-muted);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(11.25rem, 1fr));
  gap: 1rem;
}

.service-card {
  background: var(--card-bg);
  border-radius: 0.75rem;
  padding: 1.25rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 0.125rem solid transparent;
  box-shadow: var(--shadow-sm);
}

.service-card:hover {
  transform: translateY(-0.125rem);
  box-shadow: var(--shadow-md);
}

.service-card.active {
  border-color: var(--gold);
  background: rgba(212, 168, 67, 0.08);
}

.service-icon {
  width: 3.5rem;
  height: 3.5rem;
  background: var(--bg-tertiary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 0.75rem;
  color: var(--text-primary);
}

.service-name {
  font-size: 0.9375rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
  color: var(--text-primary);
  line-height: 1.5;
  word-break: break-word;
}

.service-category {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.apply-form {
  padding: 1rem;
}

.form-section {
  margin-bottom: 1.5rem;
}

.form-section h4 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--text-primary);
}

.condition-box {
  background: var(--bg-tertiary);
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--text-secondary);
  word-break: break-word;
}

.material-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.material-item {
  background: var(--bg-tertiary);
  padding: 0.5rem 1rem;
  border-radius: 2rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 0.0625rem solid var(--border-color);
}

:deep(.el-form-item__label) {
  font-size: 0.875rem !important;
  color: var(--text-secondary) !important;
}

:deep(.el-input__wrapper) {
  padding: 0.5rem 0.75rem;
  background: var(--bg-tertiary);
  border-color: var(--border-color);
}

.custom-steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 1rem 0;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.step-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-width: 5rem;
}

.step-number {
  width: 2rem;
  height: 2rem;
  background: var(--gold);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 600;
  z-index: 2;
  transition: all 0.2s;
}

.step-title {
  font-size: 0.75rem;
  color: var(--text-secondary);
  margin-top: 0.5rem;
  text-align: center;
  word-break: break-word;
}

.step-line {
  position: absolute;
  top: 1rem;
  left: calc(50% + 1rem);
  right: calc(-50% + 1rem);
  height: 0.125rem;
  background: var(--border-color);
  z-index: 1;
}

@media (max-width: 48rem) {
  .material-item {
    flex: 1 0 auto;
    white-space: normal;
    text-align: center;
    justify-content: center;
  }

  .step-number {
    width: 1.75rem;
    height: 1.75rem;
    font-size: 0.875rem;
  }

  .step-title {
    font-size: 0.6875rem;
  }

  .step-line {
    top: 0.875rem;
    left: calc(50% + 0.875rem);
    right: calc(-50% + 0.875rem);
  }
}
</style>
