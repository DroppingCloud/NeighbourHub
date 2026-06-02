<template>
  <div class="material-upload-container">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <p>{{ pageDescription }}</p>
    </div>

    <div class="service-summary-card">
      <div>
        <span class="summary-label">当前办理事项</span>
        <strong>{{ serviceName || '事项办理' }}</strong>
        <div v-if="contextTags.length" class="summary-context">
          <span v-for="tag in contextTags" :key="tag.label" class="context-chip">
            <span>{{ tag.label }}</span>
            <strong>{{ tag.value }}</strong>
          </span>
        </div>
      </div>
      <el-tag effect="plain">{{ serviceCategory || '事项' }}</el-tag>
    </div>

    <div class="steps-wrapper">
      <el-steps :active="2" align-center finish-status="success">
        <el-step title="填写信息" />
        <el-step title="上传材料" />
        <el-step :title="isSupplementMode ? '重新提交' : '提交申请'" />
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
                <div class="material-title-row">
                  <span class="material-name">{{ material.name }}</span>
                  <el-tag v-if="material.alternativeGroup" size="small" type="warning" effect="plain">
                    {{ material.preferred ? '推荐上传' : '可替代' }}
                  </el-tag>
                  <el-tag v-else-if="material.required" size="small" type="danger" effect="plain">必需</el-tag>
                  <el-tag v-else size="small" effect="plain">可选</el-tag>
                </div>
                <div v-if="material.alternativeGroupLabel" class="material-group-rule">
                  {{ material.alternativeGroupLabel }}任选一项上传即可。
                </div>
                <div class="material-format">
                  {{ material.description || material.hint }}
                </div>
                <div class="material-rule">
                  支持 {{ material.allowedExtensions.map(ext => `.${ext}`).join(' / ') }}，最大 {{ material.maxSizeMb }}MB
                </div>
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
            <div class="template-actions">
              <el-button size="small" plain :disabled="material.noTemplateRequired" @click="previewTemplate(material)">
                <el-icon><View /></el-icon> 查看模板
              </el-button>
              <el-button size="small" plain :disabled="material.noTemplateRequired" @click="downloadTemplate(material)">
                <el-icon><Download /></el-icon> 下载模板
              </el-button>
              <span v-if="material.noTemplateRequired" class="template-note">证件原件上传，无需模板</span>
              <span v-else-if="material.templateName" class="template-note">{{ material.templateName }}</span>
            </div>

            <div
              v-if="!material.uploaded"
              class="upload-area"
              :class="{ dragging: draggingMaterialIndex === index, uploading: material.uploading }"
              @click="triggerUpload(index)"
              @dragenter.prevent="handleDragEnter(index)"
              @dragover.prevent="handleDragOver(index)"
              @dragleave.prevent="handleDragLeave(index)"
              @drop.prevent.stop="handleFileDrop($event, index)"
            >
              <el-icon :size="32"><UploadFilled /></el-icon>
              <span v-if="material.uploading">正在预审材料...</span>
              <span v-else-if="draggingMaterialIndex === index">松开鼠标上传到此材料</span>
              <span v-else>{{ isSupplementMode ? '点击或拖拽上传修改后的材料' : '点击或拖拽上传' }}</span>
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
                <el-button link type="primary" @click.stop="downloadFile(index)">
                  <el-icon><Download /></el-icon> 下载
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
              <div v-if="material.suggestions.length" class="suggestion-box">
                <strong>修改建议：</strong>
                <span>{{ material.suggestions.join('；') }}</span>
              </div>
              <el-button size="small" type="primary" plain @click="reUpload(index)">
                重新上传
              </el-button>
            </div>
            
            <div v-if="material.uploaded && material.precheckStatus === 'passed'" class="precheck-pass">
              <el-icon><SuccessFilled /></el-icon>
              <span>{{ material.precheckRemark || '材料通过预审，符合当前提交要求' }}</span>
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
        <el-icon><Check /></el-icon> {{ submitButtonText }}
      </el-button>
    </div>

    <input 
      ref="fileInputRef" 
      type="file" 
      :accept="currentAccept"
      style="display: none" 
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document, UploadFilled, View, Delete, CircleCheck, CircleClose,
  Loading, WarningFilled, SuccessFilled, Back, Check, Download
} from '@element-plus/icons-vue'
import {
  getApplicationDetail,
  getApplicationMaterialFileUrl,
  resubmitApplication,
  submitApplication,
  cleanupFailedDraftApplication,
  checkApplicationMaterialCompleteness,
  uploadApplicationMaterialFile,
  runAiPrecheckApplicationMaterial
} from '@/api/application'
import { getPublicMaterialTemplates } from '@/api/serviceItem'
import {
  isIdentityDocumentMaterial,
  resolveBuiltInMaterialTemplate,
  wrapMaterialTemplateHtml
} from '@/utils/materialTemplateLibrary'

const router = useRouter()
const route = useRoute()

// 定义材料项类型
interface MaterialItem {
  id: string
  templateId?: number | null
  materialId?: number
  name: string
  hint: string
  description: string
  required: boolean
  alternativeGroup: string
  alternativeGroupLabel: string
  preferred: boolean
  allowedExtensions: string[]
  maxSizeMb: number
  templateUrl?: string
  templateName: string
  templateHtml: string
  noTemplateRequired: boolean
  uploaded: boolean
  uploading: boolean
  dirty: boolean
  file?: File
  fileName: string
  fileSize: number
  fileUrl: string
  precheckStatus: 'passed' | 'failed' | 'checking' | null
  precheckRemark: string
  issues: string[]
  suggestions: string[]
}

const currentMaterialIndex = ref(-1)
const draggingMaterialIndex = ref<number | null>(null)
const fileInputRef = ref<HTMLInputElement>()
const submitting = ref(false)
const isSupplementMode = ref(false)
const supplementApplicationId = ref<number | null>(null)
const existingApplicationMode = ref<'supplement' | 'resubmit'>('supplement')
const defaultMaxSizeMb = 20

// 表单数据（从上一页获取）
const formData = ref({
  name: '',
  idCard: '',
  phone: '',
  address: '',
  residenceStartDate: '',
  residenceEndDate: '',
  employerName: '',
  workAddress: '',
  schoolName: '',
  enrollmentDate: '',
  applicationCondition: '',
  applicationConditionLabel: '',
  housingType: '',
  housingLabel: '',
  proofType: '',
  proofLabel: '',
  scenario: '',
  scenarioLabel: ''
})

const serviceName = ref('')
const serviceId = ref('')
const serviceCategory = ref('')
const proxyUserId = ref<number | null>(null)

function emptyFormData() {
  return {
    name: '',
    idCard: '',
    phone: '',
    address: '',
    residenceStartDate: '',
    residenceEndDate: '',
    employerName: '',
    workAddress: '',
    schoolName: '',
    enrollmentDate: '',
    applicationCondition: '',
    applicationConditionLabel: '',
    housingType: '',
    housingLabel: '',
    proofType: '',
    proofLabel: '',
    scenario: '',
    scenarioLabel: ''
  }
}

// 材料列表
const materials = ref<MaterialItem[]>([
  {
    id: 'idCard',
    name: '身份证',
    hint: '需上传正反面清晰照片',
    description: '用于核验申请人身份信息',
    required: true,
    alternativeGroup: '',
    alternativeGroupLabel: '',
    preferred: false,
    allowedExtensions: ['pdf', 'jpg', 'jpeg', 'png'],
    maxSizeMb: defaultMaxSizeMb,
    templateName: '',
    templateHtml: '',
    noTemplateRequired: true,
    uploaded: false,
    uploading: false,
    dirty: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    precheckRemark: '',
    issues: [],
    suggestions: []
  },
  {
    id: 'householdRegister',
    name: '户口本',
    hint: '需上传户主页和本人页',
    description: '用于核验户籍信息',
    required: true,
    alternativeGroup: '',
    alternativeGroupLabel: '',
    preferred: false,
    allowedExtensions: ['pdf', 'jpg', 'jpeg', 'png'],
    maxSizeMb: defaultMaxSizeMb,
    templateName: '',
    templateHtml: '',
    noTemplateRequired: true,
    uploaded: false,
    uploading: false,
    dirty: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    precheckRemark: '',
    issues: [],
    suggestions: []
  },
  {
    id: 'residenceProof',
    name: '居住证明',
    hint: '租房合同或房产证',
    description: '用于核验当前居住地址',
    required: true,
    alternativeGroup: '',
    alternativeGroupLabel: '',
    preferred: false,
    allowedExtensions: ['pdf', 'jpg', 'jpeg', 'png'],
    maxSizeMb: defaultMaxSizeMb,
    templateName: '',
    templateHtml: '',
    noTemplateRequired: false,
    uploaded: false,
    uploading: false,
    dirty: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    precheckRemark: '',
    issues: [],
    suggestions: []
  },
  {
    id: 'photo',
    name: '近期免冠照片',
    hint: '白底或蓝底，2寸',
    description: '用于办事材料归档或证件照采集',
    required: false,
    alternativeGroup: '',
    alternativeGroupLabel: '',
    preferred: false,
    allowedExtensions: ['jpg', 'jpeg', 'png'],
    maxSizeMb: defaultMaxSizeMb,
    templateName: '证件照要求说明.doc',
    templateHtml: '',
    noTemplateRequired: false,
    uploaded: false,
    uploading: false,
    dirty: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    precheckRemark: '',
    issues: [],
    suggestions: []
  }
])

const requiredMaterials = computed(() => materials.value.filter(m => m.required))
const alternativeMaterialGroups = computed(() => {
  const groups = new Map<string, { label: string; rows: MaterialItem[] }>()
  for (const material of materials.value) {
    if (!material.alternativeGroup) continue
    if (!groups.has(material.alternativeGroup)) {
      groups.set(material.alternativeGroup, {
        label: material.alternativeGroupLabel || '可替代材料',
        rows: []
      })
    }
    groups.get(material.alternativeGroup)!.rows.push(material)
  }
  return Array.from(groups.values())
})
const missingRequiredNames = computed(() => [
  ...requiredMaterials.value.filter(m => !m.uploaded).map(m => m.name),
  ...alternativeMaterialGroups.value
    .filter(group => !group.rows.some(row => row.uploaded))
    .map(group => `${group.label}（任选一项）`)
])
const allUploaded = computed(() => missingRequiredNames.value.length === 0)
const hasFailedPrecheck = computed(() => materials.value.some(m => m.precheckStatus === 'failed'))
const showOverallResult = computed(() => materials.value.some(m => m.uploaded))
const currentAccept = computed(() => {
  const material = materials.value[currentMaterialIndex.value]
  const extensions = material?.allowedExtensions?.length
    ? material.allowedExtensions
    : ['jpg', 'jpeg', 'png', 'pdf']
  return extensions.map(ext => `.${ext}`).join(',')
})
const pageTitle = computed(() => {
  if (!isSupplementMode.value) return '材料上传与预审'
  return existingApplicationMode.value === 'resubmit' ? '修改并重新提交' : '补交材料'
})
const pageDescription = computed(() => {
  if (!isSupplementMode.value) return '请按要求上传以下材料，系统将自动进行智能预审'
  return existingApplicationMode.value === 'resubmit'
    ? '请检查并修改已撤回申请的材料，确认无误后重新提交'
    : '请根据退回意见补充缺失或预审未通过的材料'
})
const submitButtonText = computed(() => {
  if (!isSupplementMode.value) return '提交申请'
  return existingApplicationMode.value === 'resubmit' ? '重新提交' : '提交补件'
})

const overallResult = computed(() => {
  const uploadedCount = requiredMaterials.value.filter(m => m.uploaded).length
  const passedCount = requiredMaterials.value.filter(m => m.precheckStatus === 'passed').length
  const failedCount = materials.value.filter(m => m.precheckStatus === 'failed').length
  const checkingCount = materials.value.filter(m => m.precheckStatus === 'checking').length
  const missingItems = missingRequiredNames.value
  
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

  if (missingItems.length > 0) {
    return {
      passed: false,
      title: '材料待补充',
      desc: `还缺少 ${missingItems.length} 项必需材料，请补齐后再提交`,
      failedItems: missingItems
    }
  }
  
  if (uploadedCount === requiredMaterials.value.length && passedCount === requiredMaterials.value.length) {
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
    desc: `已上传 ${uploadedCount}/${requiredMaterials.value.length} 项固定必需材料`,
    failedItems: []
  }
})

const contextTags = computed(() => {
  const tags: Array<{ label: string; value: string }> = []
  if (formData.value.applicationConditionLabel) {
    tags.push({ label: '申请条件', value: formData.value.applicationConditionLabel })
  }
  if (formData.value.housingLabel) {
    tags.push({ label: '住房情况', value: formData.value.housingLabel })
  }
  if (formData.value.proofLabel) {
    tags.push({
      label: serviceName.value.includes('便民证明') ? '证明类型' : '证明材料',
      value: formData.value.proofLabel
    })
  }
  if (formData.value.scenarioLabel) {
    tags.push({ label: '具体情形', value: formData.value.scenarioLabel })
  }
  if (formData.value.residenceStartDate || formData.value.residenceEndDate) {
    tags.push({
      label: '居住时间',
      value: `${formData.value.residenceStartDate || '未填'} 至 ${formData.value.residenceEndDate || '未填'}`
    })
  }
  return tags
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

async function triggerUpload(index: number) {
  if (materials.value[index]?.uploading) return
  currentMaterialIndex.value = index
  await nextTick()
  fileInputRef.value?.click()
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || currentMaterialIndex.value === -1) return
  await processSelectedFile(file, currentMaterialIndex.value)
  input.value = ''
}

function handleDragEnter(index: number) {
  if (materials.value[index]?.uploading) return
  draggingMaterialIndex.value = index
}

function handleDragOver(index: number) {
  if (materials.value[index]?.uploading) return
  draggingMaterialIndex.value = index
}

function handleDragLeave(index: number) {
  if (draggingMaterialIndex.value === index) {
    draggingMaterialIndex.value = null
  }
}

async function handleFileDrop(event: DragEvent, index: number) {
  draggingMaterialIndex.value = null
  const file = event.dataTransfer?.files?.[0]
  if (!file) return
  await processSelectedFile(file, index)
}

async function processSelectedFile(file: File, index: number) {
  const material = materials.value[index]
  if (!material) return
  if (material.uploading) return

  material.uploading = true
  material.precheckStatus = 'checking'
  material.issues = []
  material.suggestions = []

  try {
    await handleLocalFilePrecheck(file, material)
  } finally {
    material.uploading = false
  }
}

async function handleLocalFilePrecheck(file: File, material: MaterialItem) {
  const result = await runPrecheck(file, material)
  material.uploaded = true
  material.dirty = true
  material.fileName = file.name
  material.fileSize = file.size
  material.fileUrl = URL.createObjectURL(file)
  material.file = file
  material.precheckStatus = result.passed ? 'passed' : 'failed'
  material.precheckRemark = result.remark
  material.issues = result.issues
  material.suggestions = result.suggestions

  if (result.passed) {
    ElMessage.success(`${material.name} 预审通过`)
  } else {
    ElMessage.warning(`${material.name} 预审未通过，请根据提示修改`)
  }
}

async function runPrecheck(file: File, material: MaterialItem) {
  const issues: string[] = []
  const suggestions: string[] = []
  const extension = getFileExtension(file.name)
  const allowedExtensions = material.allowedExtensions.map(ext => ext.toLowerCase())
  const maxBytes = material.maxSizeMb * 1024 * 1024
  const lowerName = material.name.toLowerCase()

  if (!allowedExtensions.includes(extension)) {
    issues.push(`文件格式不符合要求，当前为 .${extension || '未知'}，要求 ${allowedExtensions.map(ext => `.${ext}`).join(' / ')}`)
    suggestions.push('请按材料模板要求转换为规定格式后重新上传')
  }

  const minUsefulBytes = getMinUsefulFileSize(extension)
  if (file.size === 0) {
    issues.push('文件为空，无法用于审核')
    suggestions.push('请重新导出或重新扫描后上传')
  } else if (file.size < minUsefulBytes) {
    issues.push(getSmallFileIssue(extension))
    suggestions.push('请确认文件内容完整后重新上传')
  }

  if (file.size > maxBytes) {
    issues.push(`文件超过 ${material.maxSizeMb}MB 限制`)
    suggestions.push('请压缩文件或降低图片分辨率后重新上传')
  }

  if (['jpg', 'jpeg', 'png'].includes(extension)) {
    const imageIssues = await checkImageQuality(file)
    const fatalImageIssues = imageIssues.filter(issue => issue.includes('无法正常读取'))
    issues.push(...fatalImageIssues)
    if (imageIssues.length) {
      suggestions.push('请使用清晰、无遮挡、光线充足的原件照片')
      suggestions.push(...imageIssues)
    }
  }

  if (extension === 'pdf' && !(await hasPdfHeader(file))) {
    issues.push('PDF 文件头异常，文件可能损坏或格式伪装')
    suggestions.push('请重新导出 PDF 后上传')
  }

  if (extension === 'docx' && !(await hasZipHeader(file))) {
    issues.push('DOCX 文件结构异常，文件可能损坏')
    suggestions.push('请使用 Word 重新保存为 DOCX 后上传')
  }

  if (requiresSignature(material) && !file.name.includes('签') && !file.name.toLowerCase().includes('sign')) {
    suggestions.push('该材料通常需要签字或盖章，请确认文件内容已签字后再提交')
  }

  if (/银行卡|银行账户/.test(material.name) && !/\d/.test(file.name)) {
    suggestions.push('建议文件名包含银行卡或账户关键字，便于工作人员快速识别')
  }

  if (requiresTemplateDocument(material) && !['doc', 'docx', 'pdf'].includes(extension)) {
    issues.push('该材料应使用系统模板填写后上传 DOC/DOCX/PDF 文件')
    suggestions.push('请先点击“下载模板”，填写完成并签字后再上传')
  }

  if (/证件照|照片/.test(material.name) && !['jpg', 'jpeg', 'png'].includes(extension)) {
    issues.push('证件照必须上传 JPG 或 PNG 图片')
    suggestions.push('请按证件照要求重新上传近期免冠照片')
  }

  if (lowerName.includes('doc') && extension === 'pdf') {
    suggestions.push('若由 Word 模板转换为 PDF，请确认表格内容完整且签字页清晰')
  }

  const passed = issues.length === 0
  return {
    passed,
    issues,
    suggestions: Array.from(new Set(suggestions)),
    remark: passed
      ? '格式、大小和基础质量检查通过；提交时将调用后端OCR/AI预审'
      : issues.join('；')
  }
}

async function checkImageQuality(file: File) {
  const issues: string[] = []
  const url = URL.createObjectURL(file)
  try {
    const image = await loadImage(url)
    if (image.width < 600 || image.height < 400) {
      issues.push(`图片分辨率较低，当前 ${image.width}x${image.height}，可能影响人工审核和 OCR 识别`)
    }
    if (file.size < 50 * 1024) {
      issues.push('图片文件过小，可能存在压缩过度或内容模糊')
    }
  } catch {
    issues.push('图片无法正常读取，文件可能损坏')
  } finally {
    URL.revokeObjectURL(url)
  }
  return issues
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = url
  })
}

async function hasPdfHeader(file: File) {
  const text = await readFileHeader(file, 4)
  return text === '%PDF'
}

async function hasZipHeader(file: File) {
  const text = await readFileHeader(file, 2)
  return text === 'PK'
}

async function readFileHeader(file: File, length: number) {
  const buffer = await file.slice(0, length).arrayBuffer()
  return String.fromCharCode(...new Uint8Array(buffer))
}

function getFileExtension(fileName: string) {
  const parts = fileName.toLowerCase().split('.')
  return parts.length > 1 ? parts.pop() || '' : ''
}

function requiresSignature(material: MaterialItem) {
  return /授权|承诺|申请表|签字|签章/.test(`${material.name}${material.description}`)
}

function getMinUsefulFileSize(extension: string) {
  if (['doc', 'docx'].includes(extension)) {
    return 512
  }
  if (extension === 'pdf') {
    return 1024
  }
  if (['jpg', 'jpeg', 'png'].includes(extension)) {
    return 1024
  }
  return 1024
}

function getSmallFileIssue(extension: string) {
  if (['doc', 'docx'].includes(extension)) {
    return 'Word 文件内容过少，可能未填写或保存异常'
  }
  if (extension === 'pdf') {
    return 'PDF 文件过小，可能为空白文件、损坏文件或页数不完整'
  }
  if (['jpg', 'jpeg', 'png'].includes(extension)) {
    return '图片文件过小，可能为空白图片或保存异常'
  }
  return '文件过小，可能为空白文件或损坏文件'
}

function requiresTemplateDocument(material: MaterialItem) {
  const text = `${material.name}${material.description}`
  if (material.noTemplateRequired) return false
  if (/照片|相片|身份证|户口簿|居住证|学生证|残疾证|营业执照|合同|证书|银行卡/.test(text)) {
    return false
  }
  return /申请表|授权书|承诺书|声明书|说明/.test(text)
}

function removeFile(index: number) {
  const material = materials.value[index]
  material.uploaded = false
  material.dirty = true
  material.fileName = ''
  material.fileSize = 0
  material.fileUrl = ''
  material.file = undefined
  material.precheckStatus = null
  material.precheckRemark = ''
  material.issues = []
  material.suggestions = []
  ElMessage.info(`${material.name} 已删除`)
}

function reUpload(index: number) {
  removeFile(index)
  triggerUpload(index)
}

async function previewFile(index: number) {
  const material = materials.value[index]
  if (!material.fileUrl) return

  const previewWindow = window.open('', '_blank')
  if (!previewWindow) {
    ElMessage.error('材料预览窗口被浏览器拦截，请允许弹窗后重试')
    return
  }

  try {
    if (material.fileUrl.startsWith('/api/')) {
      const blob = await fetchMaterialBlob(material)
      if (!blob) {
        previewWindow.close()
        return
      }
      const url = URL.createObjectURL(blob)
      previewWindow.location.href = url
      setTimeout(() => URL.revokeObjectURL(url), 60000)
      return
    }
    previewWindow.location.href = material.fileUrl
  } catch (error) {
    previewWindow.close()
    ElMessage.error('材料预览失败，请尝试下载后查看')
  }
}

async function downloadFile(index: number) {
  const material = materials.value[index]
  if (!material.fileUrl) return

  if (material.fileUrl.startsWith('/api/')) {
    const blob = await fetchMaterialBlob(material)
    if (!blob) return
    saveBlob(blob, material.fileName || `${material.name}.${material.allowedExtensions[0] || 'dat'}`)
    return
  }

  const link = document.createElement('a')
  link.href = material.fileUrl
  link.download = material.fileName || `${material.name}.${material.allowedExtensions[0] || 'dat'}`
  link.click()
}

async function fetchMaterialBlob(material: MaterialItem) {
  const token = localStorage.getItem('token')
  try {
    const response = await fetch(material.fileUrl, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!response.ok) {
      ElMessage.error(response.status === 401 ? '登录已过期，请重新登录后预览材料' : '材料文件读取失败')
      return null
    }
    const blob = await response.blob()
    const contentType = response.headers.get('content-type') || inferMimeType(material.fileName)
    return blob.type === contentType ? blob : new Blob([blob], { type: contentType })
  } catch {
    ElMessage.error('材料文件读取失败，请检查后端服务是否正常')
    return null
  }
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function inferMimeType(fileName: string) {
  const extension = fileName.split('.').pop()?.toLowerCase()
  const mimeTypes: Record<string, string> = {
    pdf: 'application/pdf',
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  }
  return mimeTypes[extension || ''] || 'application/octet-stream'
}

function previewTemplate(material: MaterialItem) {
  if (material.noTemplateRequired) {
    ElMessage.info('该材料为官方证件原件或照片上传，无需下载模板')
    return
  }
  if (material.templateUrl) {
    window.open(material.templateUrl, '_blank')
    return
  }

  const html = material.templateHtml || buildTemplateHtml(material)
  const url = URL.createObjectURL(new Blob([html], { type: 'text/html;charset=utf-8' }))
  window.open(url, '_blank')
  setTimeout(() => URL.revokeObjectURL(url), 5000)
}

function downloadTemplate(material: MaterialItem) {
  if (material.noTemplateRequired) {
    ElMessage.info('该材料为官方证件原件或照片上传，无需下载模板')
    return
  }
  if (material.templateUrl) {
    const link = document.createElement('a')
    link.href = material.templateUrl
    link.download = material.templateName || `${material.name}模板`
    link.click()
    return
  }

  const html = material.templateHtml || buildTemplateHtml(material)
  const templateFileName = normalizeGeneratedTemplateFileName(material)
  const link = document.createElement('a')
  link.href = URL.createObjectURL(new Blob([html], { type: 'application/msword;charset=utf-8' }))
  link.download = templateFileName
  link.click()
  URL.revokeObjectURL(link.href)
}

function normalizeGeneratedTemplateFileName(material: MaterialItem) {
  const rawName = material.templateName || `${material.name}模板.doc`
  return rawName.replace(/\.docx$/i, '.doc').replace(/\.pdf$/i, '.doc')
}

function buildTemplateHtml(material: MaterialItem) {
  return `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${material.name}模板</title>
  <style>
    body { font-family: "Microsoft YaHei", Arial, sans-serif; line-height: 1.8; padding: 32px; color: #1f2937; }
    h1 { font-size: 24px; }
    table { border-collapse: collapse; width: 100%; margin-top: 16px; }
    td, th { border: 1px solid #d1d5db; padding: 10px; text-align: left; }
    .muted { color: #6b7280; }
  </style>
</head>
<body>
  <h1>${material.name}模板</h1>
  <p class="muted">该模板由系统根据事项材料配置自动生成，用于演示在线查看与下载能力。</p>
  <table>
    <tr><th>材料名称</th><td>${material.name}</td></tr>
    <tr><th>是否必需</th><td>${material.required ? '是' : '否'}</td></tr>
    <tr><th>用途说明</th><td>${material.description || material.hint}</td></tr>
    <tr><th>格式要求</th><td>${material.allowedExtensions.map(ext => `.${ext}`).join(' / ')}</td></tr>
    <tr><th>大小限制</th><td>${material.maxSizeMb}MB以内</td></tr>
  </table>
  <h2>填写说明</h2>
  <ol>
    <li>请按真实、完整、清晰的原则准备材料。</li>
    <li>身份证、居住证明等材料需保证文字和证件号码可辨认。</li>
    <li>授权书、承诺书、申请表等材料如需签字，请完成签字后再上传。</li>
    <li>上传后系统会自动进行格式、大小和基础质量预审。</li>
  </ol>
</body>
</html>`
}

async function submitMaterials() {
  if (!allUploaded.value) {
    const missing = missingRequiredNames.value.join('、')
    ElMessage.warning(`请先上传所有必需材料：${missing}`)
    return
  }
  
  if (hasFailedPrecheck.value) {
    ElMessage.warning('存在预审未通过的材料，请修正后重新上传')
    return
  }
  
  submitting.value = true
  let createdApplicationId: number | null = null
  try {
    const applicationId = isSupplementMode.value
      ? Number(supplementApplicationId.value)
        : await submitApplication({
          itemId: Number(serviceId.value),
          proxyUserId: proxyUserId.value || undefined,
          formData: formData.value,
          remark: `${serviceName.value || '事项'}申请`
        })
    if (!isSupplementMode.value) {
      createdApplicationId = applicationId
    }

    for (const material of materials.value) {
      if (!material.uploaded) {
        continue
      }
      if (isSupplementMode.value && !material.dirty) {
        continue
      }
      if (!material.file) {
        continue
      }
      const materialId = await uploadApplicationMaterialFile(applicationId, {
        templateId: material.templateId ?? null,
        materialName: material.name,
        file: material.file
      })
      const aiResult = await runAiPrecheckApplicationMaterial(materialId)
      material.materialId = materialId
      material.fileUrl = getApplicationMaterialFileUrl(materialId)
      material.precheckStatus = aiResult.precheckStatus === 'passed' ? 'passed' : 'failed'
      material.precheckRemark = aiResult.precheckRemark || material.precheckRemark
      material.issues = aiResult.precheckStatus === 'failed'
        ? [aiResult.precheckRemark || 'OCR/AI预审未通过，请重新上传清晰完整的材料']
        : []
      material.suggestions = aiResult.precheckStatus === 'failed'
        ? ['请根据OCR/AI预审提示重新上传材料']
        : []
      if (material.precheckStatus === 'failed') {
        await cleanupCreatedApplication(createdApplicationId)
        ElMessage.warning(`${material.name} OCR/AI预审未通过`)
        return
      }
    }

    const completeness = await checkApplicationMaterialCompleteness(applicationId)
    if (!completeness.complete) {
      await cleanupCreatedApplication(createdApplicationId)
      ElMessage.warning(`材料不完整，缺少：${completeness.missingMaterialNames.join('、')}`)
      return
    }

    if (isSupplementMode.value) {
      await resubmitApplication(applicationId, {
        itemId: Number(serviceId.value),
        formData: formData.value,
        remark: existingApplicationMode.value === 'resubmit'
          ? `${serviceName.value || '事项'}重新提交`
          : `${serviceName.value || '事项'}补件已提交`
      })
    }

    sessionStorage.removeItem('tempApplication')
    ElMessage.success(isSupplementMode.value
      ? (existingApplicationMode.value === 'resubmit' ? '申请已重新提交' : '补件材料已重新提交')
      : '申请和材料已提交')
    router.push('/application/list')
  } catch (error) {
    await cleanupCreatedApplication(createdApplicationId)
    throw error
  } finally {
    submitting.value = false
  }
}

async function cleanupCreatedApplication(applicationId: number | null) {
  if (!applicationId || isSupplementMode.value) {
    return
  }
  try {
    await cleanupFailedDraftApplication(applicationId)
  } catch {
    // 清理失败时不打断用户当前修正材料的操作，后端权限和状态会兜底。
  }
}

function goBack() {
  router.back()
}

async function loadTempData() {
  const applicationId = Number(route.query.applicationId)
  const mode = String(route.query.mode || '')
  if ((mode === 'supplement' || mode === 'resubmit') && applicationId) {
    existingApplicationMode.value = mode
    loadExistingApplicationData(applicationId)
    return
  }

  const tempData = sessionStorage.getItem('tempApplication')
  if (tempData) {
    const data = JSON.parse(tempData)
    serviceId.value = data.serviceId
    serviceName.value = data.serviceName
    serviceCategory.value = data.serviceCategory || ''
    proxyUserId.value = data.proxyUserId || null
    formData.value = data.formData
    if (Array.isArray(data.serviceMaterials) && data.serviceMaterials.length > 0) {
      const templates = await getPublicMaterialTemplates(Number(data.serviceId)).catch(() => [])
      const serviceMaterials = withTemplateIds(data.serviceMaterials, templates)
      materials.value = serviceMaterials.map((material: any, index: number) =>
        createMaterialItem(material, index)
      )
    }
  } else {
    ElMessage.warning('请先填写申请信息')
    router.back()
  }
}

async function loadExistingApplicationData(applicationId: number) {
  isSupplementMode.value = true
  supplementApplicationId.value = applicationId
  const detail = await getApplicationDetail(applicationId)

  serviceId.value = String(detail.itemId)
  serviceName.value = detail.itemName
  serviceCategory.value = detail.category || ''
  formData.value = parseFormData(detail.formData)

  const uploadedByTemplate = new Map(
    (detail.materials || [])
      .filter((material: any) => material.templateId)
      .map((material: any) => [material.templateId, material])
  )
  const uploadedByName = new Map(
    (detail.materials || [])
      .filter((material: any) => material.materialName)
      .map((material: any) => [material.materialName, material])
  )
  const requiredMaterials = String(detail.itemName || serviceName.value || '').includes('居住证办理')
    ? getResidencePermitMaterialsByProof(formData.value.proofType)
    : String(detail.itemName || serviceName.value || '').includes('便民证明')
      ? getConvenienceMaterialsByProof(formData.value.proofType, formData.value.scenario)
    : mergeMaterials(
        detail.requiredMaterials || [],
        getFallbackMaterialsByService(detail.itemName || serviceName.value)
      )

  materials.value = requiredMaterials.map((template: any, index: number) => {
    const uploaded = (template.templateId
      ? uploadedByTemplate.get(template.templateId)
      : uploadedByName.get(template.materialName)) as any
    const usable = uploaded && uploaded.precheckStatus !== 'failed'
    const base = createMaterialItem(template, index)
    return {
      ...base,
      materialId: uploaded?.materialId,
      uploaded: Boolean(usable),
      uploading: false,
      dirty: false,
      fileName: usable ? uploaded.fileName : '',
      fileSize: usable ? Number(uploaded.fileSize || 0) : 0,
      fileUrl: usable ? uploaded.fileUrl || getApplicationMaterialFileUrl(uploaded.materialId) : '',
      precheckStatus: usable ? uploaded.precheckStatus || 'passed' : null,
      precheckRemark: usable ? uploaded.precheckRemark || '' : '',
      issues: uploaded?.precheckStatus === 'failed'
        ? [uploaded.precheckRemark || '材料预审未通过，请重新上传']
        : [],
      suggestions: uploaded?.precheckStatus === 'failed'
        ? ['请根据工作人员意见重新上传清晰完整的材料']
        : []
    }
  })
}

function getFallbackMaterialsByService(itemName: string) {
  const name = String(itemName || '')
  if (name.includes('居住证办理')) {
    return getResidencePermitMaterialsByProof()
  }
  if (name.includes('老年补贴')) {
    return [
      materialTemplate('身份证', 'id_card', '申请人身份证明材料。', true),
      materialTemplate('户口簿', 'household_register', '申请人户籍信息材料。', true),
      materialTemplate('高龄津贴申请表', 'senior_allowance_application', '请下载模板填写申请人、紧急联系人和补贴发放账户信息并签字。', true),
      materialTemplate('近期免冠两寸照片', 'recent_two_inch_photo', '近期6个月内免冠彩色两寸照，白色或淡蓝色背景。', true),
      materialTemplate('社保卡（银行卡）', 'social_security_card', '用于补贴发放账户核验。', true)
    ]
  }
  if (name.includes('居住证明')) {
    return [
      materialTemplate('居民身份证（或户口簿）', 'identity_or_household_register', '用于核验申请人身份和户籍信息。', true),
      materialTemplate('居住情况证明', 'residence_situation_proof', '可上传房产证、租房合同、单位宿舍证明等。', true),
      materialTemplate('亲属关系证明', 'family_relationship_proof', '居住在近亲属家中时提交。', false)
    ]
  }
  if (name.includes('便民证明')) {
    return getConvenienceMaterialsByProof()
  }
  return []
}

function getConvenienceMaterialsByProof(proofType = 'no_criminal', scenario = '') {
  const common = [
    materialTemplate('居民身份证', 'id_card', '申请人当前有效身份证或电子身份证。', true),
    materialTemplate('户口簿', 'household_register', '首页和本人页，用于核验户籍及家庭关系。', true)
  ]
  const configs: Record<string, any> = {
    no_criminal: {
      base: [
        ...common,
        materialTemplate('申请事由证明', 'application_reason_proof', '如单位介绍信、录取通知书、签证要求说明或资格审查说明。', true),
        materialTemplate('无犯罪记录证明申请表', 'no_criminal_record_application', '现场填写或下载模板填写后上传。', true),
        materialTemplate('居住证', 'residence_permit_card', '非本地户籍人员部分城市需提供。', false),
        materialTemplate('委托书', 'proxy_authorization', '委托他人代办时提交，并补充双方身份证。', false)
      ]
    },
    low_income: {
      base: [
        ...common,
        materialTemplate('收入说明', 'income_statement', '包括工资、养老金、子女赡养费等家庭收入。', true),
        materialTemplate('家庭情况说明', 'family_situation_statement', '说明因病、因残、因灾等致困原因。', true),
        materialTemplate('低收入家庭申请表', 'low_income_family_application', '社区或街道统一制式申请表。', true)
      ],
      scenario: {
        illness: [materialTemplate('病历/诊断证明', 'medical_diagnosis', '医院出具的病历、诊断书、出院小结等。', true)],
        disability: [materialTemplate('残疾证', 'disability_certificate', '残联颁发的残疾证。', true)],
        unemployment: [materialTemplate('失业证明', 'unemployment_certificate', '就业创业证或单位解除劳动关系证明。', true)],
        student_support: [materialTemplate('在校学生证明', 'student_study_proof', '学校出具的在读证明或学生证。', true)]
      },
      optional: [
        materialTemplate('婚姻状况证明', 'marital_status_proof', '单亲家庭可补充离婚证、配偶死亡证明等。', false),
        materialTemplate('房产证明', 'housing_asset_proof', '部分地区用于核验住房困难情况。', false),
        materialTemplate('车辆登记证明', 'vehicle_registration_proof', '部分地区用于核对家庭资产。', false)
      ]
    },
    marital_status: {
      base: [
        ...common,
        materialTemplate('婚姻登记档案证明', 'marriage_archive_proof', '向原登记机关申请调取。', true)
      ],
      scenario: {
        marriage_certificate_lost: [materialTemplate('婚姻状况说明承诺书', 'marital_status_commitment', '无法提供完整档案时补充说明并签字承诺。', false)],
        divorce_certificate_lost: [
          materialTemplate('法院判决书/调解书', 'court_judgment_or_mediation', '判决离婚情形需提供法院出具材料。', false),
          materialTemplate('结婚证/离婚证', 'marriage_or_divorce_certificate', '如有剩余证件，需提供。', false)
        ],
        widowed: [materialTemplate('配偶死亡证明', 'spouse_death_certificate', '医院死亡证明或派出所注销户口证明。', true)],
        household_migration: [materialTemplate('结婚证/离婚证', 'marriage_or_divorce_certificate', '用于户口迁移或婚姻状况变更。', true)]
      },
      optional: [
        materialTemplate('单位/社区证明', 'unit_or_community_marriage_proof', '无法获取档案时由单位人事部门或社区出具说明。', false),
        materialTemplate('委托书', 'proxy_authorization', '委托他人代办时提交，并补充双方身份证。', false)
      ]
    },
    same_person: {
      base: [
        ...common,
        materialTemplate('同一人身份声明书', 'same_person_identity_statement', '本人签字承诺两个身份信息为同一人。', true)
      ],
      scenario: {
        id_15_to_18: [materialTemplate('原身份证件', 'old_identity_document', '旧身份证复印件或原身份证号证件。', false)],
        name_changed: [materialTemplate('户口簿曾用名页', 'household_previous_name_page', '户口簿中记载曾用名的页面。', true)],
        archive_error: [materialTemplate('单位/学校证明', 'unit_or_school_proof', '人事档案记录不一致时由单位或学校出具说明。', true)],
        household_migration_change: [materialTemplate('户籍变更证明', 'household_change_proof', '派出所出具的户籍变更证明。', true)]
      },
      optional: [
        materialTemplate('佐证材料', 'supporting_identity_evidence', '驾驶证、护照、毕业证等能证明身份的证件。', false)
      ]
    }
  }
  const config = configs[proofType] || configs.no_criminal
  return mergeMaterials([
    ...config.base,
    ...(config.scenario?.[scenario] || []),
    ...(config.optional || [])
  ], [])
}

function getResidencePermitMaterialsByProof(proofType = 'rental_contract') {
  const base = [
    materialTemplate('居民身份证', 'id_card', '原件及复印件，用于核验申请人身份。', true),
    materialTemplate('本人相片', 'personal_photo', '近期免冠彩色一寸照，部分地区可从人口系统提取。', true)
  ]
  const proofMap: Record<string, any> = {
    rental_contract: materialTemplate('房屋租赁合同', 'rental_contract', '租赁住房申请时提交房屋租赁合同关键页。', true),
    real_estate_certificate: materialTemplate('房屋产权证明文件', 'real_estate_certificate', '自有房屋申请时提交房屋产权证明文件或不动产权证书关键信息页。', true),
    purchase_contract: materialTemplate('购房合同', 'purchase_contract', '购房但暂未取得产权证时提交购房合同关键信息页。', true),
    unit_accommodation_proof: materialTemplate('用人单位出具的住宿证明', 'unit_accommodation_proof', '居住在单位宿舍时提交加盖单位公章的住宿证明。', true),
    school_accommodation_proof: materialTemplate('就读学校出具的住宿证明', 'school_accommodation_proof', '居住在学校宿舍时提交加盖学校公章的住宿证明。', true),
    business_license: materialTemplate('工商营业执照', 'business_license', '个体工商户或企业主申请时提交营业执照副本信息。', true),
    labor_contract: materialTemplate('劳动合同', 'labor_contract', '受雇就业申请时提交劳动合同关键页。', true),
    labor_relation_proof: materialTemplate('用人单位出具的劳动关系证明', 'labor_relation_proof', '不便提供完整劳动合同时，由用人单位出具劳动关系证明。', true),
    student_card: materialTemplate('学生证', 'student_card', '提交学生证关键信息页和有效注册记录。', true),
    continuous_study_proof: materialTemplate('就读学校出具的连续就读证明', 'continuous_study_proof', '由学校教务处或相关部门盖章出具。', true)
  }
  const groupKeys = (() => {
    if (['real_estate_certificate', 'purchase_contract'].includes(proofType)) {
      return ['real_estate_certificate', 'purchase_contract']
    }
    if (['business_license', 'labor_contract', 'labor_relation_proof'].includes(proofType)) {
      return ['business_license', 'labor_contract', 'labor_relation_proof']
    }
    if (['student_card', 'continuous_study_proof'].includes(proofType)) {
      return ['student_card', 'continuous_study_proof']
    }
    return [proofType]
  })()
  const proofRows = groupKeys
    .map(key => proofMap[key])
    .filter(Boolean)
    .map((row: any) => ({
      ...row,
      isRequired: groupKeys.length > 1 ? 0 : row.isRequired,
      alternativeGroup: groupKeys.length > 1 ? 'residence_permit_proof' : '',
      alternativeGroupLabel: groupKeys.length > 1 ? '居住证办理证明材料' : '',
      preferred: row.materialType === proofType
    }))
  return [...base, ...proofRows]
}

function materialTemplate(materialName: string, materialType: string, description: string, isRequired: boolean) {
  return {
    templateId: null,
    materialName,
    materialType,
    description,
    sampleUrl: '',
    isRequired: isRequired ? 1 : 0,
    sortOrder: 0
  }
}

function withTemplateIds(materials: any[], templates: any[]) {
  const templateByType = new Map<string, any>()
  const templateByName = new Map<string, any>()
  for (const template of templates || []) {
    if (template.materialType) {
      templateByType.set(template.materialType, template)
    }
    if (template.materialName) {
      templateByName.set(template.materialName, template)
    }
  }

  return (materials || []).map(row => {
    const matched = templateByType.get(row.materialType) || templateByName.get(row.materialName)
    if (!matched) return row
    return {
      ...row,
      templateId: row.templateId ?? matched.templateId,
      itemId: row.itemId ?? matched.itemId,
      sampleUrl: row.sampleUrl || matched.sampleUrl || '',
      sortOrder: row.sortOrder ?? matched.sortOrder
    }
  })
}

function mergeMaterials(primaryMaterials: any[], fallbackMaterials: any[]) {
  const rows = [...(primaryMaterials || []), ...(fallbackMaterials || [])]
  const map = new Map<string, any>()
  for (const row of rows) {
    const key = row.materialType || row.materialName
    if (!map.has(key)) {
      map.set(key, row)
    }
  }
  return Array.from(map.values())
}

function createMaterialItem(material: any, index: number): MaterialItem {
  const isObject = typeof material === 'object' && material !== null
  const name = isObject ? material.materialName || material.name : String(material)
  const materialType = isObject ? material.materialType || '' : ''
  const description = isObject ? material.description || '' : ''
  const builtInTemplate = resolveBuiltInMaterialTemplate(name, materialType, description)
  const noTemplateRequired = !builtInTemplate && isIdentityDocumentMaterial(name, materialType)
  const allowedExtensions = inferAllowedExtensions(name, materialType, description, Boolean(builtInTemplate), noTemplateRequired)
  const templateKey = isObject ? material.templateId ?? 'no-template' : 'text'
  const typeKey = materialType || name || 'material'
  return {
    id: `material-${index}-${templateKey}-${typeKey}`,
    templateId: isObject ? material.templateId ?? null : null,
    name,
    hint: description || '请上传清晰完整的材料文件',
    description,
    required: isObject && material.alternativeGroup ? false : (isObject ? Number(material.isRequired ?? 1) === 1 : true),
    alternativeGroup: isObject ? material.alternativeGroup || '' : '',
    alternativeGroupLabel: isObject ? material.alternativeGroupLabel || '' : '',
    preferred: isObject ? Boolean(material.preferred) : false,
    allowedExtensions,
    maxSizeMb: defaultMaxSizeMb,
    templateUrl: isObject ? material.sampleUrl || '' : '',
    templateName: builtInTemplate?.fileName || '',
    templateHtml: builtInTemplate ? wrapMaterialTemplateHtml(builtInTemplate) : '',
    noTemplateRequired,
    uploaded: false,
    uploading: false,
    dirty: false,
    fileName: '',
    fileSize: 0,
    fileUrl: '',
    precheckStatus: null,
    precheckRemark: '',
    issues: [],
    suggestions: []
  }
}

function inferAllowedExtensions(
  name: string,
  materialType?: string,
  description?: string,
  hasTemplate = false,
  noTemplateRequired = false
) {
  const source = `${name} ${materialType || ''} ${description || ''}`.toLowerCase()
  const explicitExtensions = resolveAllowedExtensionsByMaterialType(materialType || '', name)
  if (explicitExtensions.length) {
    return explicitExtensions
  }
  if (noTemplateRequired) {
    return ['pdf', 'jpg', 'jpeg', 'png']
  }
  if (/照片|相片|图片|photo|image|jpg|png/.test(source)) {
    return ['jpg', 'jpeg', 'png']
  }
  if (/租赁合同|购房合同|劳动合同|合同/.test(source)) {
    return ['doc', 'docx', 'pdf', 'jpg', 'jpeg', 'png']
  }
  if (/身份证|户口簿|居住证|学生证|残疾证|营业执照|证书|银行卡|产权证明|证明文件/.test(source)) {
    return ['pdf', 'jpg', 'jpeg', 'png']
  }
  if (hasTemplate) {
    return ['doc', 'docx', 'pdf']
  }
  if (/docx|word|申请表|授权书|承诺书|表格|doc/.test(source)) {
    return ['doc', 'docx', 'pdf']
  }
  if (/pdf/.test(source)) {
    return ['pdf']
  }
  return ['pdf', 'jpg', 'jpeg', 'png']
}

function resolveAllowedExtensionsByMaterialType(materialType: string, name: string) {
  const key = materialType.toLowerCase()
  const text = `${name} ${key}`.toLowerCase()
  const imageOnlyTypes = new Set([
    'personal_photo',
    'recent_two_inch_photo'
  ])
  const documentScanTypes = new Set([
    'id_card',
    'household_register',
    'identity_or_household_register',
    'social_security_card',
    'bank_card',
    'residence_permit_card',
    'real_estate_certificate',
    'business_license',
    'student_card',
    'disability_certificate',
    'old_identity_document',
    'household_previous_name_page',
    'housing_asset_proof',
    'vehicle_registration_proof',
    'marriage_or_divorce_certificate',
    'spouse_death_certificate',
    'court_judgment_or_mediation',
    'medical_diagnosis',
    'residence_situation_proof',
    'supporting_identity_evidence'
  ])
  const templateDocumentTypes = new Set([
    'rental_contract',
    'purchase_contract',
    'labor_contract',
    'senior_allowance_application',
    'unit_accommodation_proof',
    'school_accommodation_proof',
    'labor_relation_proof',
    'continuous_study_proof',
    'family_relationship_proof',
    'community_residence_proof',
    'application_reason_proof',
    'application_reason_note',
    'no_criminal_record_application',
    'no_criminal_record_proof',
    'income_statement',
    'family_situation_statement',
    'low_income_family_application',
    'low_income_hardship_proof',
    'marriage_archive_proof',
    'marital_status_proof',
    'marital_status_commitment',
    'unit_or_community_marriage_proof',
    'same_person_identity_statement',
    'same_person_identity_proof',
    'unit_or_school_proof',
    'household_change_proof',
    'proxy_authorization',
    'student_study_proof',
    'unemployment_certificate'
  ])

  if (imageOnlyTypes.has(key) || /照片|相片|证件照|photo/.test(text)) {
    return ['jpg', 'jpeg', 'png']
  }
  if (documentScanTypes.has(key)) {
    return ['pdf', 'jpg', 'jpeg', 'png']
  }
  if (templateDocumentTypes.has(key)) {
    return ['doc', 'docx', 'pdf']
  }
  return []
}

function parseFormData(value?: string) {
  if (!value) return emptyFormData()
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return {
      name: parsed.name || '',
      idCard: parsed.idCard || '',
      phone: parsed.phone || '',
      address: parsed.address || '',
      residenceStartDate: parsed.residenceStartDate || '',
      residenceEndDate: parsed.residenceEndDate || '',
      employerName: parsed.employerName || '',
      workAddress: parsed.workAddress || '',
      schoolName: parsed.schoolName || '',
      enrollmentDate: parsed.enrollmentDate || '',
      applicationCondition: parsed.applicationCondition || '',
      applicationConditionLabel: parsed.applicationConditionLabel || '',
      housingType: parsed.housingType || '',
      housingLabel: parsed.housingLabel || '',
      proofType: parsed.proofType || '',
      proofLabel: parsed.proofLabel || '',
      scenario: parsed.scenario || '',
      scenarioLabel: parsed.scenarioLabel || ''
    }
  } catch {
    return emptyFormData()
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

.service-summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1rem 1.25rem;
  margin-bottom: 1.5rem;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.service-summary-card strong {
  display: block;
  margin-top: 0.25rem;
  color: var(--text-primary);
  font-size: 1.125rem;
  line-height: 1.5;
  word-break: break-word;
}

.summary-context {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.context-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: var(--bg-tertiary);
  color: var(--text-muted);
  font-size: 0.78rem;
}

.context-chip strong {
  display: inline;
  margin: 0;
  color: var(--text-primary);
  font-size: 0.78rem;
}

.summary-label {
  color: var(--text-muted);
  font-size: 0.8125rem;
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

.material-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.material-format {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.material-group-rule {
  font-size: 0.6875rem;
  color: var(--gold);
  margin-top: 0.125rem;
}

.material-rule {
  font-size: 0.6875rem;
  color: var(--text-secondary);
  margin-top: 0.125rem;
}

.material-body {
  padding: 1rem;
}

.template-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.template-note {
  font-size: 0.75rem;
  color: var(--text-muted);
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

.upload-area.dragging {
  border-color: var(--gold);
  background: rgba(212, 168, 67, 0.12);
  box-shadow: inset 0 0 0 0.0625rem rgba(212, 168, 67, 0.35);
}

.upload-area.uploading {
  cursor: wait;
  opacity: 0.82;
}

.upload-area .el-icon {
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.upload-area.dragging .el-icon {
  color: var(--gold);
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

.suggestion-box {
  margin: 0.625rem 0;
  padding: 0.625rem;
  border-radius: var(--radius-sm);
  background: var(--bg-tertiary);
  font-size: 0.75rem;
  line-height: 1.6;
  color: var(--text-secondary);
}

.suggestion-box strong {
  color: var(--text-primary);
  margin-right: 0.25rem;
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
