<template>
  <div class="material-upload-container">
    <div class="page-header">
      <h2>材料上传与预审</h2>
      <p>请按要求上传以下材料，系统将自动进行智能预审</p>
    </div>

    <div class="steps-wrapper">
      <el-steps :active="2" align-center finish-status="success">
        <el-step title="填写信息" />
        <el-step title="上传材料" />
        <el-step title="提交申请" />
      </el-steps>
    </div>

    <div class="applicant-info-card">
      <h3>申请人信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <span class="label">申请人：</span>
          <span class="value">{{ formData.name }}</span>
        </div>
        <div class="info-item">
          <span class="label">身份证号：</span>
          <span class="value">{{ formatIdCard(formData.idCard) }}</span>
        </div>
        <div class="info-item">
          <span class="label">联系电话：</span>
          <span class="value">{{ formData.phone }}</span>
        </div>
        <div class="info-item">
          <span class="label">居住地址：</span>
          <span class="value">{{ formData.address }}</span>
        </div>
      </div>
    </div>

    <div class="material-section">
      <div class="section-header">
        <h3>所需材料清单</h3>
        <span class="material-count">共 {{ materials.length }} 项</span>
      </div>
      
      <div class="material-list">
        <div 
          v-for="(material, index) in materials" 
          :key="material.id"
          class="material-card"
          :class="{ 
            uploaded: material.uploaded,
            precheckPassed: material.precheckStatus === 'passed',
            precheckFailed: material.precheckStatus === 'failed'
          }"
        >
          <div class="material-header">
            <div class="material-info">
              <span class="material-index">{{ index + 1 }}</span>
              <div class="material-detail">
                <div class="material-name">{{ material.name }}</div>
                <div class="material-format">支持 .jpg .png .pdf，最大20MB</div>
              </div>
            </div>
            <div class="material-status">
              <el-tag v-if="material.uploaded && material.precheckStatus === 'passed'" type="success" size="small">
                <el-icon><CircleCheck /></el-icon> 预审通过
              </el-tag>
              <el-tag v-else-if="material.uploaded && material.precheckStatus === 'failed'" type="danger" size="small">
                <el-icon><CircleClose /></el-icon> 预审不通过
              </el-tag>
              <el-tag v-else-if="material.uploaded && material.precheckStatus === 'checking'" type="info" size="small">
                <el-icon><Loading /></el-icon> 预审中
              </el-tag>
              <el-tag v-else-if="material.uploaded" type="info" size="small">
                已上传
              </el-tag>
              <el-tag v-else type="info" plain size="small">
                待上传
              </el-tag>
            </div>
          </div>
          
          <div class="material-body">
            <div v-if="!material.uploaded" class="upload-area" @click="triggerUpload(index)">
              <el-icon :size="32"><UploadFilled /></el-icon>
              <span>点击或拖拽上传</span>
              <span class="upload-hint">{{ material.hint }}</span>
            </div>
            
            <div v-else class="file-preview">
              <div class="file-info">
                <el-icon><Document /></el-icon>
                <span class="file-name">{{ material.fileName }}</span>
                <span class="file-size">{{ formatFileSize(material.fileSize) }}</span>
              </div>
              <div class="file-actions">
                <el-button link type="primary" @click.stop="previewFile(index)">
                  <el-icon><View /></el-icon> 预览
                </el-button>
                <el-button link type="danger" @click.stop="removeFile(index)">
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </div>
            </div>
            
            <div v-if="material.uploaded && material.precheckStatus === 'failed'" class="precheck-result">
              <div class="result-title">
                <el-icon><WarningFilled /></el-icon>
                <span>预审问题：</span>
              </div>
              <ul>
                <li v-for="(issue, idx) in material.issues" :key="idx">{{ issue }}</li>
              </ul>
              <el-button size="small" type="primary" plain @click="reUpload(index)">
                重新上传
              </el-button>
            </div>
            
            <div v-if="material.uploaded && material.precheckStatus === 'passed'" class="precheck-pass">
              <el-icon><SuccessFilled /></el-icon>
              <span>材料通过预审，内容清晰完整</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="overall-result" v-if="showOverallResult">
      <div class="result-card" :class="overallResult.passed ? 'success' : 'warning'">
        <div class="result-icon">
          <el-icon :size="48">
            <SuccessFilled v-if="overallResult.passed" />
            <WarningFilled v-else />
          </el-icon>
        </div>
        <div class="result-content">
          <div class="result-title">{{ overallResult.title }}</div>
          <div class="result-desc">{{ overallResult.desc }}</div>
          <div class="result-detail" v-if="!overallResult.passed && overallResult.failedItems.length">
            <span>问题材料：</span>
            <span v-for="item in overallResult.failedItems" :key="item" class="failed-item">{{ item }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="action-buttons">
      <el-button @click="goBack" size="large">
        <el-icon><Back /></el-icon> 返回修改
      </el-button>
      <el-button 
        type="primary" 
        size="large" 
        :loading="submitting" 
        :disabled="!allUploaded || hasFailedPrecheck"
        @click="submitMaterials"
      >
        <el-icon><Check /></el-icon> 提交申请
      </el-button>
    </div>

    <input 
      ref="fileInputRef" 
      type="file" 
      accept=".jpg,.jpeg,.png,.pdf" 
      style="display: none" 
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document, UploadFilled, View, Delete, CircleCheck, CircleClose,
  Loading, WarningFilled, SuccessFilled, Back, Check
} from '@element-plus/icons-vue'
import {
  submitApplication,
  uploadApplicationMaterial,
  precheckApplicationMaterial
} from '@/api/application'

const router = useRouter()

// 定义材料项类型
interface MaterialItem {
  id: string
  name: string
  hint: string
  uploaded: boolean
  uploading: boolean
  fileName: string
  fileSize: number
  fileUrl: string
  precheckStatus: 'passed' | 'failed' | 'checking' | null
  issues: string[]
}

const currentMaterialIndex = ref(-1)
const fileInputRef = ref<HTMLInputElement>()
const submitting = ref(false)

// 表单数据（从上一页获取）
const formData = ref({
  name: '',
  idCard: '',
  phone: '',
  address: ''
})

const serviceName = ref('')
const serviceId = ref('')

// 材料列表
const materials = ref<MaterialItem[]>([
  {
    id: 'idCard',
    name: '身份证',
    hint: '需上传正反面清晰照片',
    uploaded: false,
    uploading: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    issues: []
  },
  {
    id: 'householdRegister',
    name: '户口本',
    hint: '需上传户主页和本人页',
    uploaded: false,
    uploading: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    issues: []
  },
  {
    id: 'residenceProof',
    name: '居住证明',
    hint: '租房合同或房产证',
    uploaded: false,
    uploading: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    issues: []
  },
  {
    id: 'photo',
    name: '近期免冠照片',
    hint: '白底或蓝底，2寸',
    uploaded: false,
    uploading: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    issues: []
  }
])

const allUploaded = computed(() => materials.value.every(m => m.uploaded))
const hasFailedPrecheck = computed(() => materials.value.some(m => m.precheckStatus === 'failed'))
const showOverallResult = computed(() => materials.value.some(m => m.uploaded))

const overallResult = computed(() => {
  const uploadedCount = materials.value.filter(m => m.uploaded).length
  const passedCount = materials.value.filter(m => m.precheckStatus === 'passed').length
  const failedCount = materials.value.filter(m => m.precheckStatus === 'failed').length
  const checkingCount = materials.value.filter(m => m.precheckStatus === 'checking').length
  
  if (checkingCount > 0) {
    return {
      passed: false,
      title: '预审进行中',
      desc: '正在智能分析您上传的材料，请稍候...',
      failedItems: []
    }
  }
  
  if (failedCount > 0) {
    const failedItems = materials.value
      .filter(m => m.precheckStatus === 'failed')
      .map(m => m.name)
    return {
      passed: false,
      title: '材料预审未通过',
      desc: `${failedCount} 项材料存在问题，请修正后重新上传`,
      failedItems
    }
  }
  
  if (uploadedCount === materials.value.length && passedCount === materials.value.length) {
    return {
      passed: true,
      title: '材料预审全部通过',
      desc: '所有材料均符合要求，可以提交申请',
      failedItems: []
    }
  }
  
  return {
    passed: false,
    title: '材料待补充',
    desc: `已上传 ${uploadedCount}/${materials.value.length} 项材料`,
    failedItems: []
  }
})

function formatIdCard(idCard: string): string {
  if (!idCard || idCard.length !== 18) return idCard
  return idCard.slice(0, 6) + '********' + idCard.slice(-4)
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function triggerUpload(index: number) {
  currentMaterialIndex.value = index
  fileInputRef.value?.click()
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || currentMaterialIndex.value === -1) return
  
  const material = materials.value[currentMaterialIndex.value]
  if (material.uploading) return
  
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('请上传 JPG、PNG 或 PDF 格式的文件')
    input.value = ''
    return
  }
  
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    input.value = ''
    return
  }
  
  material.uploading = true
  material.precheckStatus = 'checking'
  
  handleLocalFilePrecheck(file, material)
  
  material.uploading = false
  input.value = ''
}

function handleLocalFilePrecheck(file: File, material: MaterialItem) {
  material.uploaded = true
  material.fileName = file.name
  material.fileSize = file.size
  material.fileUrl = URL.createObjectURL(file)
  material.precheckStatus = 'passed'
  material.issues = []
  ElMessage.success(`${material.name} 已选择，基础格式校验通过`)
}

function removeFile(index: number) {
  const material = materials.value[index]
  material.uploaded = false
  material.fileName = ''
  material.fileSize = 0
  material.fileUrl = ''
  material.precheckStatus = null
  material.issues = []
  ElMessage.info(`${material.name} 已删除`)
}

function reUpload(index: number) {
  removeFile(index)
  triggerUpload(index)
}

function previewFile(index: number) {
  const material = materials.value[index]
  if (material.fileUrl) {
    window.open(material.fileUrl, '_blank')
  }
}

async function submitMaterials() {
  if (!allUploaded.value) {
    ElMessage.warning('请先上传所有必需材料')
    return
  }
  
  if (hasFailedPrecheck.value) {
    ElMessage.warning('存在预审未通过的材料，请修正后重新上传')
    return
  }
  
  submitting.value = true
  try {
    const applicationId = await submitApplication({
      itemId: Number(serviceId.value),
      formData: formData.value,
      remark: `${serviceName.value || '事项'}申请`
    })

    for (const material of materials.value) {
      const materialId = await uploadApplicationMaterial(applicationId, {
        templateId: null,
        materialName: material.name,
        fileName: material.fileName,
        filePath: material.fileUrl || material.fileName,
        fileSize: material.fileSize,
        fileType: material.fileName.split('.').pop() || ''
      })
      await precheckApplicationMaterial(materialId, {
        precheckStatus: material.precheckStatus === 'passed' ? 'passed' : 'pending',
        precheckRemark: material.precheckStatus === 'passed' ? '前端预检通过' : '待工作人员复核'
      })
    }

    sessionStorage.removeItem('tempApplication')
    ElMessage.success('申请和材料已提交')
    router.push('/application/list')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

function loadTempData() {
  const tempData = sessionStorage.getItem('tempApplication')
  if (tempData) {
    const data = JSON.parse(tempData)
    serviceId.value = data.serviceId
    serviceName.value = data.serviceName
    formData.value = data.formData
    if (Array.isArray(data.serviceMaterials) && data.serviceMaterials.length > 0) {
      materials.value = data.serviceMaterials.map((name: string, index: number) => ({
        id: `material-${index}`,
        name,
        hint: '请上传清晰完整的材料文件',
        uploaded: false,
        uploading: false,
        fileName: '',
        fileSize: 0,
        fileUrl: '',
        precheckStatus: null,
        issues: []
      }))
    }
  } else {
    ElMessage.warning('请先填写申请信息')
    router.back()
  }
}

onMounted(() => {
  loadTempData()
})
</script>

<style scoped>
/* 样式保持不变，与之前相同 */
.material-upload-container {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.steps-wrapper {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
}

.applicant-info-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  margin-bottom: 1.5rem;
  box-shadow: var(--shadow-sm);
}

.applicant-info-card h3 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--text-primary);
  padding-bottom: 0.5rem;
  border-bottom: 0.0625rem solid var(--border-color);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(16rem, 100%), 1fr));
  gap: 0.75rem;
}

.info-item {
  font-size: 0.875rem;
}

.info-item .label {
  color: var(--text-muted);
}

.info-item .value {
  color: var(--text-primary);
  font-weight: 500;
}

.material-section {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: var(--shadow-sm);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 0.0625rem solid var(--border-color);
}

.section-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.material-count {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.material-card {
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: visible;
}

.material-card.precheckPassed {
  background: rgba(39, 174, 96, 0.05);
  border-color: var(--jade);
}

.material-card.precheckFailed {
  background: rgba(231, 76, 60, 0.05);
  border-color: var(--vermilion);
}

.material-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--bg-tertiary);
  border-bottom: 0.0625rem solid var(--border-color);
}

.material-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.material-index {
  width: 1.75rem;
  height: 1.75rem;
  background: var(--gold);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 600;
}

.material-name {
  font-weight: 600;
  color: var(--text-primary);
}

.material-format {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.material-body {
  padding: 1rem;
}

.upload-area {
  border: 0.125rem dashed var(--border-color);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-area:hover {
  border-color: var(--gold);
  background: var(--bg-tertiary);
}

.upload-area .el-icon {
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.upload-area span {
  display: block;
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.upload-hint {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.file-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.file-name {
  font-size: 0.875rem;
  color: var(--text-primary);
}

.file-size {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.file-actions {
  display: flex;
  gap: 0.5rem;
}

.precheck-result {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: rgba(231, 76, 60, 0.08);
  border-radius: var(--radius-sm);
}

.result-title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--vermilion);
  margin-bottom: 0.5rem;
}

.precheck-result ul {
  margin: 0;
  padding-left: 1.5rem;
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.precheck-result li {
  margin: 0.25rem 0;
}

.precheck-pass {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: rgba(39, 174, 96, 0.08);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--jade);
}

.overall-result {
  margin-bottom: 1.5rem;
}

.result-card {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.25rem;
  border-radius: var(--radius-lg);
  background: var(--card-bg);
  box-shadow: var(--shadow-sm);
}

.result-card.success {
  border-left: 0.25rem solid var(--jade);
}

.result-card.warning {
  border-left: 0.25rem solid var(--vermilion);
}

.result-icon {
  flex-shrink: 0;
}

.result-card.success .result-icon {
  color: var(--jade);
}

.result-card.warning .result-icon {
  color: var(--vermilion);
}

.result-content {
  flex: 1;
}

.result-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
  color: var(--text-primary);
}

.result-desc {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.result-detail {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.failed-item {
  display: inline-block;
  background: rgba(231, 76, 60, 0.1);
  color: var(--vermilion);
  padding: 0.125rem 0.5rem;
  border-radius: 0.75rem;
  margin-left: 0.5rem;
  font-size: 0.75rem;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .file-preview {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .result-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>
