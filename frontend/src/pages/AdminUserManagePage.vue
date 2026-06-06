<template>
  <div class="user-management-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2>用户管理</h2>
        <p>工作人员 · 居民 · 家属账号管理</p>
      </div>
      <el-button v-if="activeRole === 'staff'" class="btn-add" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        新增工作人员
      </el-button>
    </div>

    <!-- 角色切换 + 筛选整合在一起 -->
    <div class="toolbar">
      <div class="role-tabs" role="tablist" aria-label="用户类型">
        <button
          v-for="item in roleTabs"
          :key="item.value"
          class="role-tab"
          :class="{ active: activeRole === item.value }"
          type="button"
          @click="switchRole(item.value)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="filters">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名 / 姓名 / 手机"
          clearable
          class="filter-input search-inp"
          @keyup.enter="loadUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-if="activeRole === 'staff'"
          v-model="filterStaffType"
          placeholder="工作人员类型"
          clearable
          class="filter-input sel-item"
        >
          <el-option label="事项办理" value="application" />
          <el-option label="服务预约" value="booking" />
        </el-select>

        <el-select v-model="filterStatus" placeholder="状态" clearable class="filter-input sel-item-sm">
          <el-option label="正常" value="active" />
          <el-option label="禁用" value="disabled" />
        </el-select>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="filteredUsers" v-loading="loading">
        <el-table-column prop="userId" label="ID" width="64" />
        <el-table-column prop="username" label="账号" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100">
          <template #default="{ row }">
            <span>{{ row.realName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="125" />

        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <span class="role-dot" :class="row.role">{{ roleText(row.role) }}</span>
          </template>
        </el-table-column>

        <el-table-column v-if="activeRole === 'staff'" label="类型" width="130" align="center">
          <template #default="{ row }">
            <span class="type-label" :class="row.staffType">{{ staffTypeText(row.staffType) }}</span>
          </template>
        </el-table-column>

        <el-table-column v-if="activeRole === 'staff'" label="服务类型" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.staffType === 'booking'" class="type-label booking">{{ bookingServiceTypeText(row.bookingServiceType) }}</span>
            <span v-else class="time-text">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="communityId" label="社区" width="72" align="center" />

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status">{{ row.status === 'active' ? '正常' : '禁用' }}</span>
          </template>
        </el-table-column>

        <el-table-column v-if="activeRole === 'staff'" label="操作" width="100" align="center">
          <template #default="{ row }">
            <button class="link-action" @click="openStaffBusiness(row)">查看业务</button>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" min-width="155">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="page-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </div>

    <!-- 新增弹窗 -->
    <el-dialog v-model="showAddDialog" title="新增工作人员" width="520px" @close="resetForm" class="admin-dialog" :show-close="true" append-to-body>
      <el-form :model="staffForm" :rules="staffFormRules" ref="staffFormRef" label-position="top" class="dialog-form">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="staffForm.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="staffForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="staffForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="staffForm.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="社区ID">
          <el-input-number v-model="staffForm.communityId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作人员类型" prop="staffType">
          <el-segmented v-model="staffForm.staffType" :options="staffTypeOptions" block />
        </el-form-item>
        <el-form-item
          v-if="staffForm.staffType === 'booking'"
          label="服务类型"
          prop="bookingServiceType"
        >
          <el-select v-model="staffForm.bookingServiceType" placeholder="请选择服务类型" class="full-select">
            <el-option
              v-for="item in bookingServiceTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="showAddDialog = false">取消</el-button>
          <el-button class="btn-submit" :loading="submitting" @click="saveStaff">创建账号</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 业务弹窗 -->
    <el-dialog v-model="businessDialogVisible" title="业务情况" width="720px">
      <div v-if="activeStaff" class="business-dialog">
        <div class="staff-summary">
          <div class="staff-name-block">
            <strong>{{ activeStaff.realName || activeStaff.username }}</strong>
            <span class="type-label" :class="activeStaff.staffType">{{ staffTypeText(activeStaff.staffType) }}</span>
          </div>
          <div class="business-stats">
            <div class="bs-item">
              <span class="bs-num">{{ pendingBusiness.length }}</span>
              <span class="bs-label">待办</span>
            </div>
            <div class="bs-item">
              <span class="bs-num">{{ finishedBusiness.length }}</span>
              <span class="bs-label">已办</span>
            </div>
          </div>
        </div>

        <el-skeleton v-if="businessLoading" :rows="4" animated />
        <template v-else>
          <div class="business-columns">
            <section>
              <h4>当前待办</h4>
              <el-empty v-if="pendingBusiness.length === 0" description="暂无待办" :image-size="64" />
              <div v-for="item in pendingBusiness" :key="item.key" class="business-item">
                <div class="bi-content">
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
                <span class="bi-status pending">{{ item.statusLabel }}</span>
              </div>
            </section>

            <section>
              <h4>已办理</h4>
              <el-empty v-if="finishedBusiness.length === 0" description="暂无记录" :image-size="64" />
              <div v-for="item in finishedBusiness" :key="item.key" class="business-item">
                <div class="bi-content">
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
                <span class="bi-status done">{{ item.statusLabel }}</span>
              </div>
            </section>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { createStaff, getUsersByRole, type StaffCreateRequest } from '@/api/user'
import { getStaffBookingList, type BookingVO } from '@/api/booking'
import { getWorkOrderList, type WorkOrderVO } from '@/api/workOrder'

type RoleType = 'staff' | 'resident' | 'family'

interface UserRow {
  userId: number
  username: string
  realName?: string
  phone?: string
  role: string
  staffType?: string
  bookingServiceType?: string
  communityId?: number
  status: string
  createTime: string
}

const roleTabs: Array<{ label: string; value: RoleType; desc: string }> = [
  { label: '工作人员', value: 'staff', desc: '事项办理 / 服务预约' },
  { label: '居民', value: 'resident', desc: '社区居民账号' },
  { label: '家属', value: 'family', desc: '代办授权账号' }
]

const activeRole = ref<RoleType>('staff')
const searchKeyword = ref('')
const filterStaffType = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const users = ref<UserRow[]>([])
const showAddDialog = ref(false)
const submitting = ref(false)
const staffFormRef = ref()
const businessDialogVisible = ref(false)
const businessLoading = ref(false)
const activeStaff = ref<UserRow | null>(null)
const pendingBusiness = ref<BusinessItem[]>([])
const finishedBusiness = ref<BusinessItem[]>([])

interface BusinessItem {
  key: string
  title: string
  desc: string
  statusLabel: string
}

const staffTypeOptions = [
  { label: '事项办理', value: 'application' },
  { label: '服务预约', value: 'booking' }
]

const bookingServiceTypeOptions = [
  { label: '助餐服务', value: 'dining' },
  { label: '陪诊服务', value: 'accompany' },
  { label: '上门服务', value: 'home_visit' }
]

const staffForm = ref<StaffCreateRequest>({
  username: '',
  realName: '',
  phone: '',
  email: '',
  communityId: 1,
  staffType: 'application',
  bookingServiceType: 'dining'
})

const staffFormRules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 2, max: 20, message: '账号长度为 2-20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  staffType: [{ required: true, message: '请选择工作人员类型', trigger: 'change' }],
  bookingServiceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }]
}

const filteredUsers = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return users.value.filter(user => {
    const matchKeyword = !keyword
      || user.username?.toLowerCase().includes(keyword)
      || user.realName?.toLowerCase().includes(keyword)
      || user.phone?.includes(keyword)
    const matchType = activeRole.value !== 'staff' || !filterStaffType.value || user.staffType === filterStaffType.value
    const matchStatus = !filterStatus.value || user.status === filterStatus.value
    return matchKeyword && matchType && matchStatus
  })
})

function switchRole(role: RoleType) {
  if (activeRole.value === role) return
  activeRole.value = role
  filterStaffType.value = ''
  currentPage.value = 1
  loadUsers()
}

function staffTypeText(type?: string) {
  if (type === 'booking') return '服务预约工作人员'
  if (type === 'application') return '事项办理工作人员'
  return '未设置'
}

function bookingServiceTypeText(type?: string) {
  if (type === 'dining') return '助餐服务'
  if (type === 'accompany') return '陪诊服务'
  if (type === 'home_visit') return '上门服务'
  return '未设置'
}

function roleText(role?: string) {
  if (role === 'staff') return '工作人员'
  if (role === 'family') return '家属'
  if (role === 'admin') return '管理员'
  return '居民'
}

function roleTagType(role?: string) {
  if (role === 'staff') return 'primary'
  if (role === 'family') return 'warning'
  if (role === 'admin') return 'danger'
  return 'success'
}

function tableHeaderStyle() {
  const styles = getComputedStyle(document.documentElement)
  return {
    background: styles.getPropertyValue('--bg-tertiary').trim(),
    color: styles.getPropertyValue('--text-primary').trim(),
    fontWeight: 500
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const page = await getUsersByRole(activeRole.value, currentPage.value, pageSize.value)
    users.value = page.records || []
    total.value = page.total || users.value.length
  } finally {
    loading.value = false
  }
}

function resetForm() {
  staffForm.value = {
    username: '',
    realName: '',
    phone: '',
    email: '',
    communityId: 1,
    staffType: 'application',
    bookingServiceType: 'dining'
  }
  staffFormRef.value?.resetFields()
}

async function saveStaff() {
  await staffFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { ...staffForm.value }
      if (payload.staffType !== 'booking') {
        payload.bookingServiceType = undefined
      }
      await createStaff(payload)
      ElMessage.success('工作人员账号已创建，初始密码为 123456')
      showAddDialog.value = false
      await loadUsers()
    } catch (error: any) {
      ElMessage.error(error.message || '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

async function openStaffBusiness(row: UserRow) {
  activeStaff.value = row
  businessDialogVisible.value = true
  businessLoading.value = true
  pendingBusiness.value = []
  finishedBusiness.value = []

  try {
    if (row.staffType === 'booking') {
      const page = await getStaffBookingList(1, 100, undefined, row.userId)
      const records = (page.records || []) as BookingVO[]
      pendingBusiness.value = records
        .filter(item => ['confirmed', 'in_progress'].includes(item.status))
        .map(toBookingBusinessItem)
      finishedBusiness.value = records
        .filter(item => ['completed', 'cancelled'].includes(item.status))
        .map(toBookingBusinessItem)
    } else {
      const page = await getWorkOrderList({ pageNum: 1, pageSize: 100, staffUserId: row.userId })
      const records = (page.records || []) as WorkOrderVO[]
      pendingBusiness.value = records
        .filter(item => ['pending', 'processing'].includes(item.status))
        .map(toWorkOrderBusinessItem)
      finishedBusiness.value = records
        .filter(item => ['approved', 'completed', 'rejected', 'supplement_required', 'cancelled'].includes(item.status))
        .map(toWorkOrderBusinessItem)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '业务情况加载失败')
  } finally {
    businessLoading.value = false
  }
}

function toWorkOrderBusinessItem(item: WorkOrderVO): BusinessItem {
  return {
    key: `workorder-${item.orderId}`,
    title: item.itemName || `事项工单 ${item.orderId}`,
    desc: `${item.residentName || '居民'} · ${item.createTime || ''}`,
    statusLabel: item.statusLabel || item.status
  }
}

function toBookingBusinessItem(item: BookingVO): BusinessItem {
  return {
    key: `booking-${item.bookingId}`,
    title: item.serviceTypeLabel || `服务预约 ${item.bookingId}`,
    desc: `${item.address || '未填写地址'} · ${item.expectTime || item.createTime || ''}`,
    statusLabel: item.statusLabel || item.status
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.user-management-container {
  max-width: 75rem;
  margin: 0 auto;
}

/* ====== Header ====== */
.page-header {
  margin-bottom: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin: 0 0 0.25rem;
  color: var(--text-primary);
  font-weight: 700;
}

.page-header p {
  color: var(--text-muted);
  font-size: 0.8125rem;
  margin: 0;
}

.full-select {
  width: 100%;
}

.btn-add {
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 0.5rem !important;
  padding: 0.5rem 1.125rem !important;
  font-weight: 600 !important;
  font-size: 0.8125rem !important;
  letter-spacing: 0.02em !important;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1) !important;
  box-shadow: 0 2px 6px rgba(26, 26, 46, 0.15) !important;
}

.btn-add:hover {
  transform: translateY(-2px) scale(1.02) !important;
  box-shadow: 0 6px 20px rgba(26, 26, 46, 0.22) !important;
}

/* ====== Toolbar: Tabs + Filters ====== */
.toolbar {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 0.75rem 1.25rem;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.role-tabs {
  display: flex;
  gap: 0.25rem;
  background: var(--bg-tertiary);
  border-radius: 0.5rem;
  padding: 0.2rem;
}

.role-tab {
  border: none;
  background: transparent;
  border-radius: 0.375rem;
  padding: 0.4rem 1rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.role-tab:hover {
  color: var(--text-primary);
}

.role-tab.active {
  background: var(--card-bg);
  color: var(--text-primary);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.filters {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* 统一所有筛选组件尺寸 */
.filter-input :deep(.el-input__wrapper) {
  height: 2rem !important;
  min-height: 2rem !important;
  padding: 0 0.75rem !important;
  border-radius: var(--radius-sm) !important;
  font-size: 0.8125rem !important;
  line-height: 2rem !important;
  box-sizing: border-box !important;
}

.filter-input :deep(.el-input__inner) {
  height: 2rem !important;
  line-height: 2rem !important;
  font-size: 0.8125rem !important;
}

.filter-input :deep(.el-input__prefix),
.filter-input :deep(.el-input__suffix) {
  font-size: 0.875rem !important;
}

.search-inp {
  width: 13rem;
}

.sel-item {
  width: 10rem;
}

.sel-item-sm {
  width: 7rem;
}

/* ====== Table ====== */
.table-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

:deep(.el-table) {
  background: transparent !important;
  --el-table-header-bg-color: transparent;
  font-size: 0.8125rem !important;
}

:deep(.el-table th) {
  background: var(--bg-tertiary) !important;
  color: var(--text-muted) !important;
  font-weight: 500 !important;
  font-size: 0.75rem !important;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border-color) !important;
  padding: 0.625rem 0 !important;
}

:deep(.el-table td) {
  border-bottom-color: var(--border-color) !important;
  padding: 0.75rem 0 !important;
  color: var(--text-primary) !important;
}

:deep(.el-table .cell) {
  color: inherit !important;
}

:deep(.el-table tr) {
  background: transparent !important;
}

:deep(.el-table tr:hover td) {
  background: rgba(212, 168, 67, 0.02) !important;
}

/* Custom labels replacing el-tag */
.role-dot {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.2rem 0.5rem;
  border-radius: 0.25rem;
}

.role-dot.staff { color: var(--ink); background: rgba(26, 26, 46, 0.06); }
.role-dot.resident { color: var(--jade); background: rgba(39, 174, 96, 0.06); }
.role-dot.family { color: var(--gold); background: rgba(212, 168, 67, 0.08); }
.role-dot.admin { color: var(--vermilion); background: rgba(192, 57, 43, 0.06); }

.type-label {
  font-size: 0.6875rem;
  font-weight: 500;
  padding: 0.2rem 0.5rem;
  border-radius: 0.25rem;
  white-space: nowrap;
}

.type-label.application { color: var(--ink); background: rgba(26, 26, 46, 0.05); }
.type-label.booking { color: #d97706; background: rgba(217, 119, 6, 0.06); }

.status-badge {
  font-size: 0.6875rem;
  font-weight: 600;
}

.status-badge.active { color: var(--jade); }
.status-badge.disabled { color: var(--text-muted); }

.link-action {
  background: none;
  border: none;
  color: var(--gold);
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.link-action:hover {
  color: var(--ink);
}

.time-text {
  font-size: 0.75rem;
  color: var(--text-muted) !important;
}

.page-wrap {
  padding: 0.75rem 1.25rem;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-color);
}

/* ====== Dialog System ====== */
:deep(.admin-dialog .el-dialog) {
  border-radius: 1rem !important;
  overflow: hidden;
  box-shadow:
    0 24px 80px rgba(26, 26, 46, 0.12),
    0 8px 24px rgba(26, 26, 46, 0.06),
    0 0 0 1px rgba(26, 26, 46, 0.04) !important;
}

:deep(.admin-dialog .el-dialog__header) {
  padding: 1rem 1.5rem !important;
  margin-right: 0 !important;
  border-bottom: 1px solid var(--border-color);
}

:deep(.admin-dialog .el-dialog__title) {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

:deep(.admin-dialog .el-dialog__headerbtn) {
  top: 1rem;
  right: 1.25rem;
}

:deep(.admin-dialog .el-dialog__body) {
  padding: 1.25rem 1.5rem 0.75rem !important;
}

:deep(.admin-dialog .el-dialog__footer) {
  padding: 0 !important;
}

.dialog-form {
  padding: 0;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 0.875rem !important;
}

.dialog-form :deep(.el-form-item__label) {
  font-size: 0.8125rem !important;
  color: var(--text-secondary) !important;
  font-weight: 500 !important;
  padding-bottom: 0.125rem !important;
  line-height: 1.4 !important;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.625rem;
  padding: 0.75rem 1.5rem 1.25rem;
}

.btn-cancel {
  background: transparent !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text-muted) !important;
  border-radius: 0.5rem !important;
  padding: 0.5rem 1.25rem !important;
  font-size: 0.8125rem !important;
  transition: all 0.2s !important;
}

.btn-cancel:hover {
  border-color: var(--text-secondary) !important;
  color: var(--text-primary) !important;
}

.btn-submit {
  background: linear-gradient(135deg, var(--ink) 0%, var(--ink-light) 100%) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 0.5rem !important;
  padding: 0.5rem 1.5rem !important;
  font-weight: 600 !important;
  font-size: 0.8125rem !important;
  letter-spacing: 0.03em !important;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1) !important;
  box-shadow: 0 2px 8px rgba(26, 26, 46, 0.15) !important;
}

.btn-submit:hover {
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 20px rgba(26, 26, 46, 0.22) !important;
}

.dialog-tip {
  margin-bottom: 1.25rem;
  border-radius: var(--radius-sm) !important;
}

/* ====== Business Dialog ====== */
.business-dialog {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.staff-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.staff-name-block {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.staff-name-block strong {
  font-size: 1rem;
  color: var(--text-primary);
}

.business-stats {
  display: flex;
  gap: 1.5rem;
}

.bs-item {
  text-align: center;
}

.bs-num {
  display: block;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.bs-label {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

.business-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
}

.business-columns h4 {
  margin: 0 0 0.75rem;
  color: var(--text-primary);
  font-size: 0.875rem;
  font-weight: 600;
}

.business-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  border-radius: var(--radius-sm);
  margin-bottom: 0.5rem;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  transition: border-color 0.2s;
}

.business-item:hover {
  border-color: var(--gold);
}

.bi-content strong {
  display: block;
  color: var(--text-primary);
  font-size: 0.8125rem;
  margin-bottom: 0.125rem;
}

.bi-content p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.75rem;
  line-height: 1.4;
}

.bi-status {
  font-size: 0.6875rem;
  font-weight: 500;
  padding: 0.15rem 0.4rem;
  border-radius: 0.25rem;
  white-space: nowrap;
  flex-shrink: 0;
}

.bi-status.pending { color: #d97706; background: rgba(217, 119, 6, 0.08); }
.bi-status.done { color: var(--jade); background: rgba(39, 174, 96, 0.08); }

@media (max-width: 48rem) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filters {
    flex-direction: column;
  }

  .search-inp,
  .sel-item,
  .sel-item-sm {
    width: 100% !important;
  }

  .business-columns {
    grid-template-columns: 1fr;
  }
}
</style>
