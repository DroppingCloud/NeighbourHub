<template>
  <div class="booking-container">
    <!-- 服务类型卡片 -->
    <div class="service-types">
      <div
        v-for="type in serviceTypes"
        :key="type.value"
        class="type-card"
        :class="{ active: selectedType === type.value }"
        @click="selectedType = type.value"
      >
        <div class="type-icon">{{ type.icon }}</div>
        <div class="type-name">{{ type.label }}</div>
        <div class="type-desc">{{ type.desc }}</div>
      </div>
    </div>

    <!-- 预约表单 -->
    <div class="booking-form">
      <h3>填写预约信息</h3>
      <el-form
        :model="bookingForm"
        :rules="formRules"
        ref="formRef"
        label-width="auto"
        label-position="top"
      >
        <!-- 预约时间（修改后） -->
        <el-form-item label="预约时间" prop="expectTime">
          <el-date-picker
            v-model="bookingForm.expectTime"
            type="datetime"
            placeholder="选择期望服务时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width-date"
            popper-class="compact-datetime-picker"
            :teleported="true"
            :default-value="defaultDateTime"
          />
        </el-form-item>

        <!-- 服务地址：支持手动输入 + 结构化选择 -->
        <el-form-item label="服务地址" prop="address" required>
          <div class="address-picker">
            <el-input
              ref="addressInputRef"
              v-model="bookingForm.address"
              placeholder="请选择或输入服务地址（如：3栋2单元15楼1502室）"
              clearable
              :suffix-icon="Grid"
              @click="openAddressPopover"
              @clear="handleManualClear"
            />
            <el-popover
              v-model:visible="addressPopoverVisible"
              :reference="addressInputRef"
              placement="bottom-start"
              width="460"
              trigger="manual"
              :hide-after="0"
              :teleported="false"
              :persistent="true"
              popper-class="address-popover-popper"
            >
              <div class="address-selector">
                <div class="selector-title">快速选择地址</div>
                <el-row :gutter="12">
                  <el-col :span="6">
                    <el-select
                      v-model="tempBuilding"
                      placeholder="栋"
                      clearable
                      size="small"
                      :teleported="false"
                      style="width: 100%"
                    >
                      <el-option v-for="n in 10" :key="n" :label="`${n}栋`" :value="n" />
                    </el-select>
                  </el-col>
                  <el-col :span="6">
                    <el-select
                      v-model="tempUnit"
                      placeholder="单元"
                      clearable
                      size="small"
                      :teleported="false"
                      style="width: 100%"
                    >
                      <el-option v-for="n in 4" :key="n" :label="`${n}单元`" :value="n" />
                    </el-select>
                  </el-col>
                  <el-col :span="6">
                    <el-select
                      v-model="tempFloor"
                      placeholder="楼层"
                      clearable
                      size="small"
                      :teleported="false"
                      style="width: 100%"
                    >
                      <el-option v-for="n in 26" :key="n" :label="`${n}楼`" :value="n" />
                    </el-select>
                  </el-col>
                  <el-col :span="6">
                    <el-select
                      v-model="tempRoom"
                      placeholder="门牌"
                      clearable
                      size="small"
                      :teleported="false"
                      style="width: 100%"
                    >
                      <el-option label="01室" value="01室" />
                      <el-option label="02室" value="02室" />
                      <el-option label="03室" value="03室" />
                      <el-option label="04室" value="04室" />
                    </el-select>
                  </el-col>
                </el-row>
                <div class="selector-actions">
                  <el-button size="small" @click="clearTempSelection">清空</el-button>
                  <el-button size="small" type="primary" @click="applyAddressSelection">确定</el-button>
                </div>
              </div>
            </el-popover>
          </div>
        </el-form-item>

        <!-- 特殊需求 -->
        <el-form-item label="特殊需求">
          <el-input
            v-model="bookingForm.remark"
            type="textarea"
            :rows="3"
            placeholder="如有特殊需求，请在此说明（如忌口、需要轮椅通道等）"
          />
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
import { Grid } from '@element-plus/icons-vue'
import { createBooking } from '@/api/booking'
import { useAuthStore } from '@/stores/auth'
import { useProxyStore } from '@/stores/proxy'

type ServiceType = 'dining' | 'accompany' | 'home_visit'
const route = useRoute()
const authStore = useAuthStore()
const proxyStore = useProxyStore()

const serviceTypes = [
  { value: 'dining' as const, label: '助餐服务', desc: '社区食堂配送到家', icon: '🍱' },
  { value: 'accompany' as const, label: '陪诊服务', desc: '陪同就医、取药', icon: '🏥' },
  { value: 'home_visit' as const, label: '上门服务', desc: '家政清洁、维修探访', icon: '🏠' }
]

const selectedType = ref<ServiceType>('dining')
const submitting = ref(false)
const formRef = ref()
const addressInputRef = ref<any>(null)

const bookingForm = ref({
  expectTime: '',
  address: '',
  remark: ''
})

// 地址弹窗相关
const addressPopoverVisible = ref(false)
const tempBuilding = ref<number | null>(null)
const tempUnit = ref<number | null>(null)
const tempFloor = ref<number | null>(null)
const tempRoom = ref<string | null>(null)

// 默认时间：当前系统时间（用于定位日期选择器）
const defaultDateTime = ref(new Date())

const isFamily = computed(() => normalizeRole(authStore.userInfo?.role || '') === 'family')
const familyNeedsBinding = computed(() => isFamily.value && !proxyStore.currentTarget)

const formRules = {
  expectTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  address: [{ required: true, message: '请填写服务地址', trigger: 'change' }]
}

// 打开弹窗时根据已有地址预填临时选择
function openAddressPopover() {
  parseAddressToTemp(bookingForm.value.address)
  addressPopoverVisible.value = true
}

// 解析地址字符串到临时选择项
function parseAddressToTemp(address: string) {
  if (!address) {
    clearTempSelection()
    return
  }

  const buildingMatch = address.match(/(\d+)栋/)
  tempBuilding.value = buildingMatch ? parseInt(buildingMatch[1], 10) : null

  const unitMatch = address.match(/(\d+)单元/)
  tempUnit.value = unitMatch ? parseInt(unitMatch[1], 10) : null

  const floorMatch = address.match(/(\d+)楼/)
  tempFloor.value = floorMatch ? parseInt(floorMatch[1], 10) : null

  let roomValue: string | null = null
  const roomSuffixMatch = address.match(/(\d+)室/)
  if (roomSuffixMatch) {
    const roomNum = parseInt(roomSuffixMatch[1], 10)
    if (roomNum >= 1 && roomNum <= 4) {
      roomValue = `${roomNum.toString().padStart(2, '0')}室`
    }
  } else {
    const fourDigitMatch = address.match(/\b(\d{4})\b/)
    if (fourDigitMatch) {
      const lastTwo = fourDigitMatch[1].slice(-2)
      const roomNum = parseInt(lastTwo, 10)
      if (roomNum >= 1 && roomNum <= 4) {
        roomValue = `${roomNum.toString().padStart(2, '0')}室`
      }
    }
  }
  tempRoom.value = roomValue
}

function clearTempSelection() {
  tempBuilding.value = null
  tempUnit.value = null
  tempFloor.value = null
  tempRoom.value = null
}

// 应用结构化选择的地址（要求四项都选）
function applyAddressSelection() {
  if (
    !tempBuilding.value ||
    !tempUnit.value ||
    !tempFloor.value ||
    !tempRoom.value
  ) {
    ElMessage.warning('请完整选择栋、单元、楼层和门牌号')
    return
  }

  const selectedAddress =
    `${tempBuilding.value}栋` +
    `${tempUnit.value}单元` +
    `${tempFloor.value}楼` +
    tempRoom.value

  bookingForm.value.address = selectedAddress
  addressPopoverVisible.value = false
  formRef.value?.validateField('address')
}

function handleManualClear() {
  bookingForm.value.address = ''
  clearTempSelection()
  formRef.value?.validateField('address')
}

async function submitBooking() {
  if (familyNeedsBinding.value) {
    ElMessage.warning('请先绑定社区用户后再代办预约')
    return
  }
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!bookingForm.value.address.trim()) {
    ElMessage.warning('请填写服务地址')
    return
  }
  submitting.value = true
  try {
    await createBooking({
      serviceType: selectedType.value,
      expectTime: bookingForm.value.expectTime,
      address: bookingForm.value.address,
      remark: bookingForm.value.remark
    })
    bookingForm.value = { expectTime: '', address: '', remark: '' }
    clearTempSelection()
    ElMessage.success('预约提交成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('预约失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (isFamily.value) {
    proxyStore.restoreTarget()
  } else {
    proxyStore.clearTarget()
  }
  const queryType = String(route.query.serviceType || '')
  if (serviceTypes.some(type => type.value === queryType)) {
    selectedType.value = queryType as ServiceType
  }
})

function normalizeRole(role: string) {
  return role.replace(/^ROLE_/, '').toLowerCase()
}
</script>

<style scoped>
.booking-container {
  width: 100%;
  max-width: 75rem;
  margin: 0 auto;
  padding: 0 1rem;
}

.service-types {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  justify-content: center;
  margin-bottom: 2rem;
}

.type-card {
  flex: 1;
  min-width: 160px;
  max-width: 200px;
  background: var(--card-bg);
  border-radius: 1rem;
  padding: 1.5rem 0.75rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid var(--border-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.type-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 20px -12px rgba(0, 0, 0, 0.15);
  border-color: var(--gold-light, #e4c27a);
}

.type-card.active {
  border-color: var(--gold, #d4a843);
  background: linear-gradient(145deg, #fffdf8, #fef9ef);
  box-shadow: 0 8px 18px rgba(212, 168, 67, 0.15);
}

.type-icon {
  font-size: 1.8rem;
  margin-bottom: 0.75rem;
}

.type-name {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.375rem;
  color: var(--text-primary);
}

.type-desc {
  font-size: 0.75rem;
  color: var(--text-muted);
  line-height: 1.4;
}

.booking-form {
  background: var(--card-bg);
  border-radius: 1.25rem;
  padding: 2rem;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border-color);
}

.booking-form h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1.75rem;
  color: var(--text-primary);
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--gold-light, #e9d5a7);
  display: inline-block;
}

.full-width-date {
  width: 100%;
  max-width: 320px;
}
:deep(.full-width-date .el-input__wrapper) {
  width: 100%;
}
:deep(.el-date-editor.el-input) {
  width: 100%;
}

.address-picker {
  width: 100%;
  position: relative;
}
:deep(.el-input__suffix) {
  cursor: pointer;
}
.address-selector {
  padding: 0.75rem;
}
.selector-title {
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.75rem;
  color: var(--text-primary);
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-color);
}
.selector-actions {
  margin-top: 0.75rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.submit-btn {
  width: 100%;
  padding: 0.75rem 0;
  font-size: 1rem;
  font-weight: 500;
  border-radius: 0.75rem;
  margin-top: 0.5rem;
  background: var(--gold, #d4a843);
  border-color: var(--gold, #d4a843);
}
.submit-btn:hover,
.submit-btn:focus {
  background: var(--gold-dark, #b88a2c);
  border-color: var(--gold-dark, #b88a2c);
}

@media (max-width: 640px) {
  .booking-container {
    padding: 0 0.75rem;
  }
  .booking-form {
    padding: 1.5rem;
  }
  .service-types {
    gap: 0.75rem;
  }
  .type-card {
    min-width: 100px;
    padding: 1rem 0.5rem;
  }
  .type-icon {
    font-size: 1.5rem;
  }
  :deep(.el-popover) {
    width: 90vw !important;
  }
  .full-width-date {
    max-width: 100%;
  }
}
</style>

<style>
/* 全局样式：自定义日期时间选择器弹窗 */
.compact-datetime-picker {
  /* 缩小整个弹窗宽度 */
  width: 480px !important;
  /* 可选：缩小字体 */
  font-size: 12px;
}

/* 日期面板（日历部分）缩小内边距和单元格尺寸 */
.compact-datetime-picker .el-date-picker__header {
  padding: 6px 8px;
}
.compact-datetime-picker .el-date-table td {
  padding: 4px 0;
}
.compact-datetime-picker .el-date-table td .cell {
  width: 28px;
  height: 28px;
  line-height: 28px;
  font-size: 12px;
}
.compact-datetime-picker .el-date-table th {
  font-size: 12px;
  padding: 6px 0;
}
/* 年份/月份切换按钮缩小 */
.compact-datetime-picker .el-picker-panel__icon-btn {
  font-size: 14px;
}
.compact-datetime-picker .el-date-picker__header-label {
  font-size: 12px;
}

/* 时间选择面板（时/分）增大尺寸 */
.compact-datetime-picker .el-time-panel {
  width: 180px !important;  /* 默认约160px，适当加宽 */
  border-radius: 8px;
}
.compact-datetime-picker .el-time-spinner__wrapper {
  width: 90px !important;   /* 时和分两列，各加宽 */
}
.compact-datetime-picker .el-time-spinner__list {
  font-size: 14px;          /* 增大数字字体 */
}
.compact-datetime-picker .el-time-spinner__item {
  height: 32px;             /* 每个选项更高 */
  line-height: 32px;
  font-size: 14px;
}
/* 隐藏时间选择器底部的取消和确认按钮 */
.compact-datetime-picker .el-time-panel__footer {
  display: none;
}
/* 同时隐藏日期时间选择器底部可能出现的按钮（如果有） */
.compact-datetime-picker .el-picker-panel__footer {
  display: none;
}

/* 地址弹窗保持原有样式 */
.address-popover-popper {
  z-index: 2000 !important;
}
</style>