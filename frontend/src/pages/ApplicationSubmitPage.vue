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
        <div class="service-summary">
          <div>
            <span class="summary-label">当前办理事项</span>
            <strong>{{ selectedService.name }}</strong>
          </div>
          <el-tag effect="plain">{{ selectedService.category || '事项' }}</el-tag>
        </div>

        <div class="form-section">
          <h4>办理条件</h4>
          <div class="condition-box">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ selectedService.conditions }}</span>
          </div>
        </div>

        <div v-if="isResidencePermitService" class="form-section">
          <h4>申请条件</h4>
          <el-radio-group v-model="residencePermitCondition" class="option-grid">
            <el-radio-button
              v-for="option in residencePermitConditionOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
          <p class="option-hint">{{ currentConditionOption?.description }}</p>

          <template v-if="residencePermitCondition === 'stable_residence'">
            <h4 class="sub-title">请选择您的居住情况</h4>
            <div class="housing-grid">
              <button
                v-for="option in residencePermitHousingOptions"
                :key="option.value"
                type="button"
                class="housing-card"
                :class="{ active: residencePermitHousingType === option.value }"
                @click="selectResidencePermitHousing(option.value)"
              >
                <el-icon><component :is="option.icon" /></el-icon>
                <strong>{{ option.label }}</strong>
                <span>{{ option.description }}</span>
              </button>
            </div>
            <p class="option-hint">
              您选择了：【{{ currentHousingOption?.fullLabel }}】，请按下方当前居住情况提交对应证明材料。
            </p>
          </template>
        </div>

        <div v-if="isConvenienceProofService" class="form-section">
          <h4>证明类型</h4>
          <el-radio-group v-model="convenienceProofType" class="option-grid">
            <el-radio-button
              v-for="option in convenienceProofTypeOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
          <p class="option-hint">{{ currentConvenienceProofOption?.description }}</p>

          <h4 class="sub-title">具体情形</h4>
          <el-radio-group v-model="convenienceScenario" class="option-grid">
            <el-radio-button
              v-for="scenario in currentConvenienceScenarios"
              :key="scenario.value"
              :label="scenario.value"
            >
              {{ scenario.label }}
            </el-radio-button>
          </el-radio-group>
          <p class="option-hint">{{ currentConvenienceScenario?.description }}</p>
        </div>

        <div class="form-section">
          <h4>所需材料</h4>
          <div class="material-list">
            <button
              v-for="material in displayMaterials"
              :key="material.materialType || material.materialName"
              type="button"
              class="material-item"
              :class="{
                selectable: true,
                active: isResidencePermitProofMaterial(material) && material.materialType === residencePermitProofType
              }"
              @click="handleMaterialClick(material)"
            >
              <el-icon><Check /></el-icon>
              <span>{{ material.materialName }}</span>
              <small v-if="material.groupHint">{{ material.groupHint }}</small>
              <small v-else-if="material.isRequired">必需</small>
              <small v-else>可选</small>
            </button>
          </div>
          <p v-if="isResidencePermitService" class="option-hint">
            居住证办理需提交“居民身份证、本人相片”以及当前申请条件下的一项证明材料；深色标记为本次提交的互斥证明材料。
            <span v-if="residencePermitCondition === 'stable_residence' && currentProofOptions.length > 1">
              {{ currentProofGroupHint }}
            </span>
          </p>
          <p v-if="isConvenienceProofService" class="option-hint">
            便民证明材料会根据证明类型和具体情形动态生成；必需材料需全部上传，可选材料按实际情况补充。
          </p>
        </div>

        <div class="form-section">
          <h4>办理流程</h4>
          <div class="custom-steps">
            <div
              v-for="(step, index) in selectedService.process"
              :key="index"
              class="step-item"
            >
              <div class="step-number">{{ Number(index) + 1 }}</div>
              <div class="step-title">{{ step }}</div>
              <div v-if="Number(index) < selectedService.process.length - 1" class="step-line"></div>
            </div>
          </div>
        </div>

        <div class="form-section">
          <h4>填写申请信息</h4>
          <el-form ref="formRef" :model="applyForm" :rules="formRules" label-width="150px">
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
            <template v-if="isResidencePermitService">
              <template v-if="residencePermitCondition === 'stable_residence'">
                <el-form-item label="居住起始日期" prop="residenceStartDate">
                  <el-date-picker
                    v-model="applyForm.residenceStartDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择居住起始日期"
                    :teleported="true"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="居住结束日期" prop="residenceEndDate">
                  <el-date-picker
                    v-model="applyForm.residenceEndDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择居住结束日期"
                    :disabled-date="disableResidenceEndDate"
                    :teleported="true"
                    style="width: 100%"
                  />
                </el-form-item>
              </template>
              <el-form-item
                v-if="residencePermitCondition === 'stable_employment'"
                label="工作单位"
                prop="employerName"
              >
                <el-input v-model="applyForm.employerName" placeholder="请输入单位或经营主体名称" />
              </el-form-item>
              <el-form-item
                v-if="residencePermitCondition === 'stable_employment'"
                label="工作地址"
                prop="workAddress"
              >
                <el-input v-model="applyForm.workAddress" placeholder="请输入工作或经营地址" />
              </el-form-item>
              <el-form-item
                v-if="residencePermitCondition === 'continuous_study'"
                label="学校名称"
                prop="schoolName"
              >
                <el-input v-model="applyForm.schoolName" placeholder="请输入学校名称" />
              </el-form-item>
              <el-form-item
                v-if="residencePermitCondition === 'continuous_study'"
                label="入学时间"
                prop="enrollmentDate"
              >
                <el-date-picker
                  v-model="applyForm.enrollmentDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择入学时间"
                  style="width: 100%"
                />
              </el-form-item>
            </template>
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

    <el-dialog v-model="materialDialogVisible" :title="activeMaterial?.materialName || '材料说明'" width="760px">
      <div v-if="activeMaterial" class="material-dialog">
        <div class="dialog-summary">
          <el-tag :type="activeMaterial.isRequired ? 'danger' : 'info'" effect="plain">
            {{ activeMaterial.isRequired ? '必需材料' : '可选材料' }}
          </el-tag>
          <span>{{ activeMaterial.description || '请按要求准备清晰完整的材料。' }}</span>
        </div>
        <div class="dialog-block">
          <h4>填写与上传说明</h4>
          <p>{{ materialInstruction }}</p>
        </div>
        <div class="dialog-block">
          <h4>模板预览</h4>
          <iframe
            v-if="activeTemplateHtml"
            class="template-preview-frame"
            :srcdoc="activeTemplateHtml"
          ></iframe>
          <div v-else class="empty-template">
            该材料为证件、照片或官方原件材料，无固定填写模板，请上传清晰照片或扫描件。
          </div>
        </div>
        <div class="dialog-actions">
          <el-button v-if="activeTemplate" type="primary" @click="downloadActiveTemplate">
            下载模板
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormRules } from 'element-plus'
import { Document, InfoFilled, Check, House, OfficeBuilding, School } from '@element-plus/icons-vue'
import { getPublicMaterialTemplates, getPublicServiceItemList } from '@/api/serviceItem'
import { useProxyStore } from '@/stores/proxy'
import {
  isIdentityDocumentMaterial,
  resolveBuiltInMaterialTemplate,
  wrapMaterialTemplateHtml
} from '@/utils/materialTemplateLibrary'

const router = useRouter()
const route = useRoute()
const govServices = ref<any[]>([])
const selectedService = ref<any>(null)
const showFormDrawer = ref(false)
const nextLoading = ref(false)
const loading = ref(false)
const formRef = ref()
const materialDialogVisible = ref(false)
const activeMaterial = ref<any>(null)
const proxyStore = useProxyStore()

const applyForm = ref({
  name: '',
  idCard: '',
  phone: '',
  address: '',
  residenceStartDate: '',
  residenceEndDate: '',
  employerName: '',
  workAddress: '',
  schoolName: '',
  enrollmentDate: ''
})

function material(
  materialName: string,
  materialType: string,
  description: string,
  isRequired: boolean,
  extra: Record<string, any> = {}
) {
  return {
    templateId: null,
    materialName,
    materialType,
    description,
    sampleUrl: '',
    isRequired: isRequired ? 1 : 0,
    ...extra
  }
}

const residencePermitCondition = ref('stable_residence')
const residencePermitHousingType = ref('rental')
const residencePermitProofType = ref('rental_contract')
const convenienceProofType = ref('no_criminal')
const convenienceScenario = ref('employment')

const residencePermitHousingOptions = [
  {
    value: 'rental',
    label: '租房',
    fullLabel: '租赁房屋居住',
    description: '租赁房屋',
    icon: House,
    proofOptions: [
      material('房屋租赁合同', 'rental_contract', '租赁住房申请时提交房屋租赁合同关键页。', true)
    ],
    hint: '请上传租赁合同关键页，需包含承租人、房屋地址、租赁期限和双方签字盖章信息。'
  },
  {
    value: 'self_owned',
    label: '自有房',
    fullLabel: '自有房屋居住',
    description: '已购住房',
    icon: House,
    proofOptions: [
      material('房屋产权证明文件', 'real_estate_certificate', '已取得不动产权证时提交房屋产权证明文件或不动产权证书关键信息页。', true),
      material('购房合同', 'purchase_contract', '尚未取得不动产权证时，可上传购房合同关键页作为替代材料。', true)
    ],
    hint: '房屋产权证明文件与购房合同为互斥材料，请上传其中一项；尚未取得不动产权证时可上传购房合同。'
  },
  {
    value: 'company_dorm',
    label: '单位宿舍',
    fullLabel: '单位宿舍居住',
    description: '单位提供',
    icon: OfficeBuilding,
    proofOptions: [
      material('用人单位出具的住宿证明', 'unit_accommodation_proof', '居住在单位宿舍时提交加盖单位公章的住宿证明。', true)
    ],
    hint: '请上传用人单位出具并盖章的住宿证明，需写明申请人姓名、住宿地址和住宿期限。'
  },
  {
    value: 'school_dorm',
    label: '学校',
    fullLabel: '学校宿舍居住',
    description: '学生宿舍',
    icon: School,
    proofOptions: [
      material('就读学校出具的住宿证明', 'school_accommodation_proof', '居住在学校宿舍时提交加盖学校公章的住宿证明。', true)
    ],
    hint: '请上传就读学校出具并盖章的住宿证明，需写明申请人姓名、宿舍地址和在读住宿情况。'
  }
]

const residencePermitConditionOptions = [
  {
    value: 'stable_residence',
    label: '合法稳定住所',
    description: '适用于已在本社区稳定居住，可提供住址证明材料的申请人。',
    proofOptions: residencePermitHousingOptions.flatMap(option => option.proofOptions)
  },
  {
    value: 'stable_employment',
    label: '合法稳定就业',
    description: '适用于在本社区稳定就业、经营，可提供就业或经营证明材料的申请人。',
    proofOptions: [
      material('工商营业执照', 'business_license', '个体工商户或企业主申请时提交营业执照副本信息。', true),
      material('劳动合同', 'labor_contract', '受雇就业申请时提交劳动合同关键页。', true),
      material('用人单位出具的劳动关系证明', 'labor_relation_proof', '不便提供完整劳动合同时，由用人单位出具劳动关系证明。', true)
    ]
  },
  {
    value: 'continuous_study',
    label: '连续就读',
    description: '适用于在本地学校连续就读，可提供学籍或就读证明材料的申请人。',
    proofOptions: [
      material('学生证', 'student_card', '提交学生证个人信息页和有效注册记录。', true),
      material('就读学校出具的连续就读证明', 'continuous_study_proof', '由学校教务处或相关部门盖章出具。', true)
    ]
  }
]

const residencePermitBaseMaterials = [
  material('居民身份证', 'id_card', '原件及复印件，用于核验申请人身份。', true),
  material('本人相片', 'personal_photo', '近期免冠彩色照片，部分地区可从人口系统提取。', true)
]

const convenienceCommonMaterials = [
  material('居民身份证', 'id_card', '申请人当前有效身份证或电子身份证。', true),
  material('户口簿', 'household_register', '首页和本人页，用于核验户籍及家庭关系。', true)
]

const convenienceProofTypeOptions = [
  {
    value: 'no_criminal',
    label: '无犯罪记录证明',
    description: '用于就业入职、出国签证、志愿者资格审核、出租汽车驾驶员资格等。',
    scenarios: [
      { value: 'employment', label: '就业入职', description: '单位背景调查、入职审核等场景。' },
      { value: 'visa', label: '出国签证', description: '签证中心或目的国要求提供无犯罪记录证明。' },
      { value: 'volunteer', label: '志愿者资格', description: '志愿者协会或服务机构资格审查。' }
    ],
    materials: [
      ...convenienceCommonMaterials,
      material('申请事由证明', 'application_reason_proof', '如单位介绍信、录取通知书、签证要求说明或资格审查说明。', true),
      material('无犯罪记录证明申请表', 'no_criminal_record_application', '现场填写或下载模板填写后上传。', true),
      material('居住证', 'residence_permit_card', '非本地户籍人员部分城市需提供。', false),
      material('委托书', 'proxy_authorization', '委托他人代办时提交，并补充双方身份证。', false)
    ]
  },
  {
    value: 'low_income',
    label: '低收入/困难证明',
    description: '用于低保申请、临时救助、医疗救助、教育资助、廉租房申请等。',
    scenarios: [
      { value: 'basic_assistance', label: '申请救助', description: '一般低收入、临时救助或住房困难申请。' },
      { value: 'illness', label: '因病致困', description: '因病产生较高医疗支出。' },
      { value: 'disability', label: '因残致困', description: '家庭成员持有残疾证或因残影响收入。' },
      { value: 'unemployment', label: '失业情形', description: '就业状态变化导致收入下降。' },
      { value: 'student_support', label: '教育资助', description: '家庭有在校学生，需要申请教育资助。' }
    ],
    materials: [
      ...convenienceCommonMaterials,
      material('收入说明', 'income_statement', '包括工资、养老金、子女赡养费等家庭收入。', true),
      material('家庭情况说明', 'family_situation_statement', '说明因病、因残、因灾等致困原因。', true),
      material('低收入家庭申请表', 'low_income_family_application', '社区或街道统一制式申请表。', true)
    ],
    scenarioMaterials: {
      illness: [material('病历/诊断证明', 'medical_diagnosis', '医院出具的病历、诊断书、出院小结等。', true)],
      disability: [material('残疾证', 'disability_certificate', '残联颁发的残疾证。', true)],
      unemployment: [material('失业证明', 'unemployment_certificate', '就业创业证或单位解除劳动关系证明。', true)],
      student_support: [material('在校学生证明', 'student_study_proof', '学校出具的在读证明或学生证。', true)]
    },
    optionalMaterials: [
      material('婚姻状况证明', 'marital_status_proof', '单亲家庭可补充离婚证、配偶死亡证明等。', false),
      material('房产证明', 'housing_asset_proof', '部分地区用于核验住房困难情况。', false),
      material('车辆登记证明', 'vehicle_registration_proof', '部分地区用于核对家庭资产。', false)
    ]
  },
  {
    value: 'marital_status',
    label: '婚姻状况证明',
    description: '用于补办结婚证/离婚证、户口迁移、财产继承、再婚登记、贷款审批等。',
    scenarios: [
      { value: 'marriage_certificate_lost', label: '结婚证丢失补办', description: '身份证、户口簿和婚姻登记档案证明为核心材料。' },
      { value: 'divorce_certificate_lost', label: '离婚证丢失补办', description: '判决离婚情形需补充法院判决书或调解书。' },
      { value: 'widowed', label: '丧偶证明', description: '需提供配偶死亡证明。' },
      { value: 'household_migration', label: '户口迁移/状态变更', description: '用于婚姻状况变更相关户籍业务。' }
    ],
    materials: [
      ...convenienceCommonMaterials,
      material('婚姻登记档案证明', 'marriage_archive_proof', '向原登记机关申请调取。', true)
    ],
    scenarioMaterials: {
      marriage_certificate_lost: [material('婚姻状况说明承诺书', 'marital_status_commitment', '无法提供完整档案时可补充说明并签字承诺。', false)],
      divorce_certificate_lost: [
        material('法院判决书/调解书', 'court_judgment_or_mediation', '判决离婚情形需提供法院出具材料。', false),
        material('结婚证/离婚证', 'marriage_or_divorce_certificate', '如有剩余证件，需提供。', false)
      ],
      widowed: [material('配偶死亡证明', 'spouse_death_certificate', '医院死亡证明或派出所注销户口证明。', true)],
      household_migration: [material('结婚证/离婚证', 'marriage_or_divorce_certificate', '用于户口迁移或婚姻状况变更。', true)]
    },
    optionalMaterials: [
      material('单位/社区证明', 'unit_or_community_marriage_proof', '无法获取档案时由单位人事部门或社区出具说明。', false),
      material('委托书', 'proxy_authorization', '委托他人代办时提交，并补充双方身份证。', false)
    ]
  },
  {
    value: 'same_person',
    label: '同一人身份证明',
    description: '用于档案材料中姓名或身份证号不一致时的更正申请。',
    scenarios: [
      { value: 'id_15_to_18', label: '身份证号15位转18位', description: '最常见，通常户口簿有记录。' },
      { value: 'name_changed', label: '姓名变更', description: '如改名，需核验户口簿曾用名页。' },
      { value: 'archive_error', label: '档案记录错误', description: '如人事档案、社保、公积金材料出现错字。' },
      { value: 'household_migration_change', label: '户籍迁移信息变化', description: '户籍迁移导致身份信息变化。' }
    ],
    materials: [
      ...convenienceCommonMaterials,
      material('同一人身份声明书', 'same_person_identity_statement', '本人签字承诺两个身份信息为同一人。', true)
    ],
    scenarioMaterials: {
      id_15_to_18: [material('原身份证件', 'old_identity_document', '旧身份证复印件或原身份证号证件。', false)],
      name_changed: [material('户口簿曾用名页', 'household_previous_name_page', '户口簿中记载曾用名的页面。', true)],
      archive_error: [material('单位/学校证明', 'unit_or_school_proof', '人事档案记录不一致时由单位或学校出具说明。', true)],
      household_migration_change: [material('户籍变更证明', 'household_change_proof', '派出所出具的户籍变更证明。', true)]
    },
    optionalMaterials: [
      material('佐证材料', 'supporting_identity_evidence', '驾驶证、护照、毕业证等能证明身份的证件。', false)
    ]
  }
]

const isResidencePermitService = computed(() => String(selectedService.value?.name || '').includes('居住证办理'))
const isConvenienceProofService = computed(() => String(selectedService.value?.name || '').includes('便民证明'))
const currentConditionOption = computed(() =>
  residencePermitConditionOptions.find(option => option.value === residencePermitCondition.value)
)
const currentHousingOption = computed(() =>
  residencePermitHousingOptions.find(option => option.value === residencePermitHousingType.value)
)
const currentProofOptions = computed(() => {
  if (residencePermitCondition.value === 'stable_residence') {
    return currentHousingOption.value?.proofOptions || []
  }
  return currentConditionOption.value?.proofOptions || []
})
const currentProofGroupHint = computed(() => currentHousingOption.value?.hint || '')
const selectedProofMaterial = computed(() =>
  currentProofOptions.value.find(material => material.materialType === residencePermitProofType.value)
)
const residencePermitProofGroupLabel = computed(() => {
  if (residencePermitCondition.value === 'stable_residence') {
    return currentHousingOption.value?.fullLabel
      ? `${currentHousingOption.value.fullLabel}证明材料`
      : '住所证明材料'
  }
  return currentConditionOption.value?.label
    ? `${currentConditionOption.value.label}证明材料`
    : '申请条件证明材料'
})
const residencePermitProofMaterials = computed(() => {
  const rows = currentProofOptions.value
  const isAlternativeGroup = rows.length > 1
  return rows.map((row: any) => ({
    ...row,
    isRequired: isAlternativeGroup ? 0 : row.isRequired,
    alternativeGroup: isAlternativeGroup ? 'residence_permit_proof' : '',
    alternativeGroupLabel: isAlternativeGroup ? residencePermitProofGroupLabel.value : '',
    preferred: row.materialType === residencePermitProofType.value,
    groupHint: isAlternativeGroup
      ? (row.materialType === residencePermitProofType.value ? '本次提交' : '可替代')
      : ''
  }))
})
const selectedResidencePermitMaterials = computed(() => [
  ...residencePermitBaseMaterials,
  ...residencePermitProofMaterials.value
].filter(Boolean))
const currentConvenienceProofOption = computed(() =>
  convenienceProofTypeOptions.find(option => option.value === convenienceProofType.value)
)
const currentConvenienceScenarios = computed(() => currentConvenienceProofOption.value?.scenarios || [])
const currentConvenienceScenario = computed(() =>
  currentConvenienceScenarios.value.find(scenario => scenario.value === convenienceScenario.value)
)
const selectedConvenienceMaterials = computed(() => {
  const option: any = currentConvenienceProofOption.value
  if (!option) return []
  const scenarioRows = option.scenarioMaterials?.[convenienceScenario.value] || []
  return mergeMaterials([...option.materials, ...scenarioRows, ...(option.optionalMaterials || [])], [])
})
const displayMaterials = computed(() => {
  if (!selectedService.value) return []
  if (isConvenienceProofService.value) return selectedConvenienceMaterials.value
  if (!isResidencePermitService.value) return selectedService.value.materials || []
  return selectedResidencePermitMaterials.value
})

const activeTemplate = computed(() => {
  if (!activeMaterial.value) return undefined
  return resolveBuiltInMaterialTemplate(
    activeMaterial.value.materialName,
    activeMaterial.value.materialType,
    activeMaterial.value.description
  )
})
const activeTemplateHtml = computed(() => activeTemplate.value ? wrapMaterialTemplateHtml(activeTemplate.value) : '')
const materialInstruction = computed(() => {
  if (!activeMaterial.value) return ''
  if (activeTemplate.value) return '可先在线查看模板内容，也可以下载模板后离线填写。填写完成后在下一步上传 DOC/DOCX/PDF 或按材料要求上传扫描件。'
  if (isIdentityDocumentMaterial(activeMaterial.value.materialName, activeMaterial.value.materialType)) {
    return '请上传证件原件或复印件的清晰照片/扫描件，确保姓名、证件号码、地址、有效期等关键信息完整可见。'
  }
  return '请上传与材料名称一致的清晰文件，格式以 PDF/JPG/PNG 为主，内容需完整、无遮挡。'
})

const formRules: FormRules = {
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
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  residenceStartDate: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (residencePermitCondition.value !== 'stable_residence') {
          callback()
          return
        }
        if (!value) {
          callback(new Error('请选择居住起始日期'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  residenceEndDate: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (residencePermitCondition.value !== 'stable_residence') {
          callback()
          return
        }
        if (!value) {
          callback(new Error('请选择居住结束日期'))
          return
        }
        if (applyForm.value.residenceStartDate && value < applyForm.value.residenceStartDate) {
          callback(new Error('结束日期不能早于起始日期'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

async function selectService(service: any) {
  selectedService.value = service
  showFormDrawer.value = true
  applyForm.value = {
    name: '',
    idCard: '',
    phone: '',
    address: '',
    residenceStartDate: '',
    residenceEndDate: '',
    employerName: '',
    workAddress: '',
    schoolName: '',
    enrollmentDate: ''
  }
  residencePermitCondition.value = 'stable_residence'
  residencePermitHousingType.value = 'rental'
  residencePermitProofType.value = 'rental_contract'
  convenienceProofType.value = 'no_criminal'
  convenienceScenario.value = 'employment'

  const templates = await getPublicMaterialTemplates(Number(service.id)).catch(() => [])
  selectedService.value = {
    ...service,
    materials: mergeMaterials(templates, service.materials)
  }
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
      materials: getFallbackMaterials(item),
      process: parseSteps(item.processSteps)
    }))
    const targetName = String(route.query.itemName || '')
    if (targetName) {
      const target = govServices.value.find(service =>
        targetName.includes(service.name) || service.name.includes(targetName)
      )
      if (target) {
        await selectService(target)
      }
    }
  } catch {
    govServices.value = []
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

function needsResidencePeriod() {
  return isResidencePermitService.value && residencePermitCondition.value === 'stable_residence'
}

function validateResidencePeriod() {
  if (!needsResidencePeriod()) return true
  if (!applyForm.value.residenceStartDate || !applyForm.value.residenceEndDate) {
    ElMessage.warning('请选择居住起始日期和结束日期')
    formRef.value?.validateField?.(['residenceStartDate', 'residenceEndDate']).catch?.(() => {})
    return false
  }
  if (applyForm.value.residenceEndDate < applyForm.value.residenceStartDate) {
    ElMessage.warning('居住结束日期不能早于起始日期')
    formRef.value?.validateField?.('residenceEndDate').catch?.(() => {})
    return false
  }
  return true
}

function disableResidenceEndDate(date: Date) {
  if (!applyForm.value.residenceStartDate) return false
  const start = new Date(`${applyForm.value.residenceStartDate} 00:00:00`).getTime()
  return date.getTime() < start
}

async function nextStep() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !validateResidencePeriod()) return

  nextLoading.value = true
  try {
    const templates = await getPublicMaterialTemplates(Number(selectedService.value.id)).catch(() => [])
    const serviceMaterials = isResidencePermitService.value
      ? selectedResidencePermitMaterials.value
      : isConvenienceProofService.value
        ? selectedConvenienceMaterials.value
      : mergeMaterials(templates, selectedService.value.materials)

    const tempData = {
      serviceId: selectedService.value.id,
      serviceName: selectedService.value.name,
      serviceCategory: selectedService.value.category,
      serviceConditions: selectedService.value.conditions,
      applicationCondition: isResidencePermitService.value
        ? {
            condition: residencePermitCondition.value,
            conditionLabel: currentConditionOption.value?.label,
            housingType: residencePermitHousingType.value,
            housingLabel: currentHousingOption.value?.fullLabel,
            proofType: residencePermitProofType.value,
            proofLabel: selectedProofMaterial.value?.materialName
          }
        : isConvenienceProofService.value
          ? {
              proofType: convenienceProofType.value,
              proofLabel: currentConvenienceProofOption.value?.label,
              scenario: convenienceScenario.value,
              scenarioLabel: currentConvenienceScenario.value?.label
            }
        : null,
      serviceMaterials,
      serviceProcess: selectedService.value.process,
      formData: {
        name: applyForm.value.name,
        idCard: applyForm.value.idCard,
        phone: applyForm.value.phone,
        address: applyForm.value.address,
        residenceStartDate: applyForm.value.residenceStartDate,
        residenceEndDate: applyForm.value.residenceEndDate,
        employerName: applyForm.value.employerName,
        workAddress: applyForm.value.workAddress,
        schoolName: applyForm.value.schoolName,
        enrollmentDate: applyForm.value.enrollmentDate,
        applicationCondition: isResidencePermitService.value ? residencePermitCondition.value : '',
        applicationConditionLabel: isResidencePermitService.value ? currentConditionOption.value?.label : '',
        housingType: isResidencePermitService.value ? residencePermitHousingType.value : '',
        housingLabel: isResidencePermitService.value ? currentHousingOption.value?.fullLabel : '',
        proofType: isResidencePermitService.value
          ? residencePermitProofType.value
          : isConvenienceProofService.value
            ? convenienceProofType.value
            : '',
        proofLabel: isResidencePermitService.value
          ? selectedProofMaterial.value?.materialName
          : isConvenienceProofService.value
            ? currentConvenienceProofOption.value?.label
            : '',
        scenario: isConvenienceProofService.value ? convenienceScenario.value : '',
        scenarioLabel: isConvenienceProofService.value ? currentConvenienceScenario.value?.label : ''
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
}

async function handleSubmit() {
  const submitData = {
    itemId: selectedItemId,
    formData: form.value,
    proxyUserId: proxyStore.currentTarget?.profileId || null,
    remark: remark.value
  }
  await submitApplication(submitData)
}

watch(residencePermitCondition, () => {
  if (residencePermitCondition.value === 'stable_residence') {
    residencePermitHousingType.value = 'rental'
  }
  residencePermitProofType.value = currentProofOptions.value[0]?.materialType || ''
})

watch(residencePermitHousingType, () => {
  if (residencePermitCondition.value === 'stable_residence') {
    residencePermitProofType.value = currentProofOptions.value[0]?.materialType || ''
  }
})

watch(() => applyForm.value.residenceStartDate, (value) => {
  if (value && applyForm.value.residenceEndDate && applyForm.value.residenceEndDate < value) {
    applyForm.value.residenceEndDate = ''
  }
})

watch(convenienceProofType, () => {
  convenienceScenario.value = currentConvenienceScenarios.value[0]?.value || ''
})

function isResidencePermitProofMaterial(row: any) {
  return isResidencePermitService.value
    && currentProofOptions.value.some(option => option.materialType === row.materialType)
}

function handleMaterialClick(row: any) {
  if (isResidencePermitProofMaterial(row)) {
    residencePermitProofType.value = row.materialType
  }
  activeMaterial.value = row
  materialDialogVisible.value = true
}

function selectResidencePermitHousing(value: string) {
  residencePermitHousingType.value = value
}

function downloadActiveTemplate() {
  if (!activeTemplate.value || !activeTemplateHtml.value) return
  const blob = new Blob([activeTemplateHtml.value], { type: 'application/msword;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = activeTemplate.value.fileName
  link.click()
  URL.revokeObjectURL(url)
}

function getFallbackMaterials(item: any) {
  const name = String(item.itemName || '')
  if (name.includes('居住证办理')) {
    return [
      ...residencePermitBaseMaterials,
      ...residencePermitConditionOptions.flatMap(option => option.proofOptions)
    ]
  }
  if (name.includes('老年补贴')) {
    return [
      material('身份证', 'id_card', '申请人身份证明材料。', true),
      material('户口簿', 'household_register', '申请人户籍信息材料。', true),
      material('近期免冠两寸照片', 'recent_two_inch_photo', '近期免冠两寸照片。', true),
      material('社保卡（银行卡）', 'social_security_card', '用于补贴发放账户核验。', true),
      material('高龄津贴申请表', 'senior_allowance_application', '请下载模板填写申请人和补贴发放账户信息并签字。', true)
    ]
  }
  if (name.includes('居住证明')) {
    return [
      material('居民身份证（或户口簿）', 'identity_or_household_register', '用于核验申请人身份和户籍信息。', true),
      material('居住情况证明', 'residence_situation_proof', '可上传房产证、租房合同、单位宿舍证明等。', true),
      material('亲属关系证明', 'family_relationship_proof', '居住在近亲属家中时提交。', false)
    ]
  }
  if (name.includes('便民证明')) {
    return [
      material('身份证', 'id_card', '申请人身份证明材料。', true),
      material('户口簿', 'household_register', '申请人户籍信息材料。', true),
      material('申请事由证明', 'application_reason_proof', '如单位介绍信、录取通知书、签证要求说明或资格审查说明。', true),
      material('申请事由说明', 'application_reason_note', '用户在线填写申请事由后生成。', true),
      material('无犯罪记录证明申请表', 'no_criminal_record_application', '现场填写或下载模板填写后上传。', false),
      material('无犯罪记录证明', 'no_criminal_record_proof', '按实际证明类型提交。', false),
      material('收入说明', 'income_statement', '包括工资、养老金、子女赡养费等家庭收入。', false),
      material('家庭情况说明', 'family_situation_statement', '说明因病、因残、因灾等致困原因。', false),
      material('低收入家庭申请表', 'low_income_family_application', '社区或街道统一制式申请表。', false),
      material('低收入/困难证明', 'low_income_hardship_proof', '按实际证明类型提交。', false),
      material('婚姻登记档案证明', 'marriage_archive_proof', '向原登记机关申请调取。', false),
      material('婚姻状况说明承诺书', 'marital_status_commitment', '无法提供完整档案时补充说明并签字承诺。', false),
      material('同一人身份证明', 'same_person_identity_proof', '按实际证明类型提交。', false)
    ]
  }
  return [
    material('申请事由说明', 'application_reason_note', '用于说明证明用途和申请原因。', true)
  ]
}

function mergeMaterials(backendMaterials: any[], fallbackMaterials: any[]) {
  const rows = [...(backendMaterials || []), ...(fallbackMaterials || [])]
  const map = new Map<string, any>()
  for (const row of rows) {
    const key = row.materialType || row.materialName
    if (!map.has(key)) {
      map.set(key, {
        templateId: row.templateId ?? null,
        materialName: row.materialName,
        materialType: row.materialType,
        description: row.description,
        sampleUrl: row.sampleUrl,
        isRequired: row.isRequired ?? 1
      })
    }
  }
  return Array.from(map.values())
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

.service-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1rem;
  margin-bottom: 1.25rem;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
}

.service-summary strong {
  display: block;
  margin-top: 0.25rem;
  color: var(--text-primary);
  font-size: 1.125rem;
}

.summary-label {
  color: var(--text-muted);
  font-size: 0.8125rem;
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

.option-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.housing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(8.5rem, 1fr));
  gap: 0.75rem;
}

.housing-card {
  border: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 0.875rem;
  min-height: 6.25rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.housing-card:hover,
.housing-card.active {
  border-color: var(--gold);
  background: rgba(212, 168, 67, 0.08);
  color: var(--text-primary);
}

.housing-card .el-icon {
  font-size: 1.5rem;
}

.housing-card strong {
  font-size: 0.95rem;
}

.housing-card span {
  font-size: 0.78rem;
  color: var(--text-muted);
}

.option-hint {
  margin-top: 0.75rem;
  color: var(--text-secondary);
  line-height: 1.6;
  word-break: break-word;
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
  border: 0.0625rem solid var(--border-color);
  background: var(--card-bg);
  padding: 0.5rem 1rem;
  border-radius: 2rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--text-secondary);
  transition: all 0.2s;
  cursor: pointer;
}

.material-item:hover {
  border-color: var(--gold);
  color: var(--text-primary);
}

.material-item.active {
  background: var(--text-primary);
  color: #fff;
  border-color: var(--text-primary);
}

.material-item small {
  color: var(--gold);
  font-size: 0.75rem;
}

.material-item.active small {
  color: var(--gold);
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

.form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 0.0625rem solid var(--border-color);
}

.material-dialog {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.dialog-summary {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  flex-wrap: wrap;
  color: var(--text-secondary);
  line-height: 1.6;
}

.dialog-block h4 {
  margin-bottom: 0.5rem;
  color: var(--text-primary);
}

.dialog-block p {
  color: var(--text-secondary);
  line-height: 1.7;
}

.template-preview-frame {
  width: 100%;
  height: 24rem;
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-sm);
  background: #fff;
}

.empty-template {
  padding: 1rem;
  border-radius: var(--radius-sm);
  background: var(--bg-tertiary);
  color: var(--text-muted);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
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

@media (max-width: 48rem) {
  .material-item {
    flex: 1 0 auto;
    white-space: normal;
    text-align: center;
    justify-content: center;
  }

  .template-preview-frame {
    height: 18rem;
  }
}
</style>
