<template>
  <div class="user-management-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <p>按工作人员、居民和家属分类管理账号；工作人员账号由管理员创建，初始密码统一为 123456。</p>
    </div>

    <div class="role-tabs" role="tablist" aria-label="用户类型">
      <button
        v-for="item in roleTabs"
        :key="item.value"
        class="role-tab"
        :class="{ active: activeRole === item.value }"
        type="button"
        @click="switchRole(item.value)"
      >
        <span>{{ item.label }}</span>
        <small>{{ item.desc }}</small>
      </button>
    </div>

    <div class="filter-card">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="用户名 / 姓名 / 手机号"
          clearable
          class="search-inp"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-if="activeRole === 'staff'"
          v-model="filterStaffType"
          placeholder="全部工作人员类型"
          clearable
          class="sel-item wide"
        >
          <el-option label="事项办理工作人员" value="application" />
          <el-option label="服务预约工作人员" value="booking" />
        </el-select>

        <el-select v-model="filterStatus" placeholder="全部状态" clearable class="sel-item">
          <el-option label="正常" value="active" />
          <el-option label="禁用" value="disabled" />
        </el-select>

        <el-button class="btn-dark" @click="loadUsers">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-button v-if="activeRole === 'staff'" class="btn-dark" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        新增工作人员
      </el-button>
    </div>

    <div class="table-card">
      <el-table
        :data="filteredUsers"
        stripe
        v-loading="loading"
        :header-cell-style="tableHeaderStyle"
      >
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column prop="username" label="登录账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="130" />

        <el-table-column label="用户类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="plain">
              {{ roleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="activeRole === 'staff'" label="工作人员类型" width="170" align="center">
          <template #default="{ row }">
            <el-tag :type="row.staffType === 'booking' ? 'warning' : 'primary'" effect="plain">
              {{ staffTypeText(row.staffType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="activeRole === 'staff'" label="服务类型" width="130" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.staffType === 'booking'" type="success" effect="plain">
              {{ bookingServiceTypeText(row.bookingServiceType) }}
            </el-tag>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="communityId" label="社区ID" width="100" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="activeRole === 'staff'" label="业务情况" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStaffBusiness(row)">查看业务</el-button>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" min-width="170" />
      </el-table>

      <div class="page-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </div>

    <el-dialog v-model="showAddDialog" title="新增工作人员" width="520px" @close="resetForm">
      <el-alert
        title="工作人员账号只能由管理员创建，初始密码为 123456，登录后可自行修改。居民和家属账号仍通过注册流程创建。"
        type="info"
        show-icon
        :closable="false"
        class="dialog-tip"
      />

      <el-form :model="staffForm" :rules="staffFormRules" ref="staffFormRef" label-width="8rem">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="staffForm.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="staffForm.realName" placeholder="请输入工作人员姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="staffForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="staffForm.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="社区ID">
          <el-input-number v-model="staffForm.communityId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="工作人员类型" prop="staffType">
          <el-segmented v-model="staffForm.staffType" :options="staffTypeOptions" />
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
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveStaff">创建账号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="businessDialogVisible" title="工作人员业务情况" width="780px">
      <div v-if="activeStaff" class="business-dialog">
        <div class="staff-summary">
          <div>
            <span class="muted">工作人员</span>
            <strong>{{ activeStaff.realName || activeStaff.username }}</strong>
          </div>
          <el-tag :type="activeStaff.staffType === 'booking' ? 'warning' : 'primary'" effect="plain">
            {{ staffTypeText(activeStaff.staffType) }}
          </el-tag>
          <el-tag v-if="activeStaff.staffType === 'booking'" type="success" effect="plain">
            {{ bookingServiceTypeText(activeStaff.bookingServiceType) }}
          </el-tag>
        </div>

        <el-skeleton v-if="businessLoading" :rows="5" animated />
        <template v-else>
          <div class="business-stats">
            <div>
              <strong>{{ pendingBusiness.length }}</strong>
              <span>当前待办</span>
            </div>
            <div>
              <strong>{{ finishedBusiness.length }}</strong>
              <span>已办理</span>
            </div>
          </div>

          <div class="business-columns">
            <section>
              <h4>当前待办</h4>
              <el-empty v-if="pendingBusiness.length === 0" description="暂无待办业务" :image-size="80" />
              <div v-for="item in pendingBusiness" :key="item.key" class="business-item">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
                <el-tag effect="plain">{{ item.statusLabel }}</el-tag>
              </div>
            </section>

            <section>
              <h4>已办理</h4>
              <el-empty v-if="finishedBusiness.length === 0" description="暂无已办理业务" :image-size="80" />
              <div v-for="item in finishedBusiness" :key="item.key" class="business-item">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
                <el-tag type="success" effect="plain">{{ item.statusLabel }}</el-tag>
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
  padding: 2rem;
  background-color: var(--bg-primary);
  min-height: 100vh;
}

.page-header {
  margin-bottom: 1.75rem;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin: 0 0 0.375rem;
  color: var(--text-primary);
  font-weight: 700;
}

.page-header p {
  color: var(--text-muted);
  font-size: 0.875rem;
  margin: 0;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.role-tab {
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  text-align: left;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.role-tab span,
.role-tab small {
  display: block;
}

.role-tab span {
  font-size: 1.05rem;
  color: var(--text-primary);
  font-weight: 600;
  margin-bottom: 0.35rem;
}

.role-tab small {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.role-tab.active {
  border-color: var(--gold);
  background: var(--bg-tertiary);
  box-shadow: 0 0.25rem 1rem rgba(212, 168, 67, 0.16);
}

.filter-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  background: var(--card-bg);
  padding: 1.25rem 1.5rem;
  border-radius: 0.875rem;
  box-shadow: 0 0.125rem 0.75rem rgba(0, 0, 0, 0.05);
  margin-bottom: 1.25rem;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex-wrap: wrap;
}

.search-inp {
  width: 14rem;
}

.sel-item {
  width: 11rem;
}

.sel-item.wide {
  width: 13rem;
}

.full-select {
  width: 100%;
}

.muted-text {
  color: var(--text-muted);
}

.btn-dark {
  background-color: var(--ink) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 0.5rem !important;
}

.table-card {
  background: var(--card-bg);
  border-radius: 0.875rem;
  box-shadow: 0 0.125rem 0.75rem rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.page-wrap {
  margin: 1.25rem 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.dialog-tip {
  margin-bottom: 1rem;
}

.business-dialog {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.staff-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--bg-tertiary);
  border-radius: 0.75rem;
}

.staff-summary strong,
.staff-summary .muted {
  display: block;
}

.staff-summary strong {
  margin-top: 0.25rem;
  color: var(--text-primary);
  font-size: 1.1rem;
}

.staff-summary .muted {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.business-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.business-stats div {
  padding: 1rem;
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  background: var(--card-bg);
  text-align: center;
}

.business-stats strong,
.business-stats span {
  display: block;
}

.business-stats strong {
  color: var(--text-primary);
  font-size: 1.5rem;
}

.business-stats span {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.business-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.business-columns h4 {
  margin: 0 0 0.75rem;
  color: var(--text-primary);
}

.business-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem;
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  margin-bottom: 0.75rem;
  background: var(--card-bg);
}

.business-item strong {
  display: block;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.business-item p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.85rem;
  line-height: 1.5;
}

@media (max-width: 760px) {
  .role-tabs {
    grid-template-columns: 1fr;
  }

  .business-columns,
  .business-stats {
    grid-template-columns: 1fr;
  }

  .filter-card,
  .filter-left {
    flex-direction: column;
    align-items: stretch;
  }

  .search-inp,
  .sel-item,
  .sel-item.wide {
    width: 100% !important;
  }
}
</style>
