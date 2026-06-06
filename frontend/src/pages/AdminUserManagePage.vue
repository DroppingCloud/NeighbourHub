<template>
  <div class="user-management-container">
    <!-- 头部标题 -->
    <div class="page-header">
      <h2>用户管理</h2>
      <p>管理系统用户、角色权限和账号状态</p>
    </div>

    <!-- 筛选栏：左右布局、深色圆角按钮 -->
    <div class="filter-card">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="用户名/手机号"
          clearable
          class="search-inp"
          @clear="loadUsers"
          @keyup.enter="loadUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="filterRole" placeholder="全部角色" clearable class="sel-item" @change="loadUsers">
          <el-option label="居民用户" value="resident" />
          <el-option label="家属用户" value="family" />
          <el-option label="工作人员" value="staff" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable class="sel-item" @change="loadUsers">
          <el-option label="正常" value="active" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-button class="btn-dark" @click="loadUsers">
          <el-icon><Search /></el-icon>查询
        </el-button>
      </div>
      <div class="filter-right">
        <el-button class="btn-dark" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
      </div>
    </div>

    <!-- 表格区域：浅米色表头，带图标tag，三色操作按钮 -->
    <div class="table-card">
      <el-table
        :data="users"
        stripe
        v-loading="loading"
        :header-cell-style="{ background: '#f3efe6', color: '#333', fontWeight: 500 }"
      >
        <el-table-column prop="userId" label="用户ID" width="70" align="left" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="realName" label="真实姓名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)" size="default" effect="plain" class="role-tag">
              <el-icon class="tag-icon"><User /></el-icon>
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="default" effect="plain" class="status-tag">
              <el-icon><CircleCheck v-if="row.status === 'active'" /><CircleClose v-else /></el-icon>
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="170" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="opt-btn-group">
              <el-button class="opt-edit" size="small" @click="editUser(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button class="opt-switch" size="small" @click="toggleUserStatus(row)">
                <el-icon><Switch /></el-icon>{{ row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
              <el-button class="opt-del" size="small" @click="deleteUser(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showAddDialog" :title="editingUser ? '编辑用户' : '新增用户'" width="520px" @close="resetForm">
      <el-form :model="userForm" :rules="userFormRules" ref="userFormRef" label-width="85px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width:100%">
            <el-option label="居民用户" value="resident" />
            <el-option label="家属用户" value="family" />
            <el-option label="工作人员" value="staff" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="登录密码" prop="password" v-if="!editingUser">
          <el-input v-model="userForm.password" show-password placeholder="密码不少于6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveUser">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 删除弹窗 -->
    <el-dialog v-model="showDeleteDialog" title="删除确认" width="420px">
      <div class="delete-tip">
        <p>确定删除用户 <strong>{{ deleteUserInfo?.username }}</strong>？</p>
        <p class="tip-red">删除数据无法恢复，请谨慎操作！</p>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Switch, User, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getUsersByRole } from '@/api/user'

interface UserRow {
  userId: number
  username: string
  realName?: string
  phone?: string
  role: string
  status: string
  createTime: string
}

// 筛选变量
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(100)
const total = ref(0)
const loading = ref(false)
const users = ref<UserRow[]>([])

// 弹窗
const showAddDialog = ref(false)
const showDeleteDialog = ref(false)
const editingUser = ref<UserRow | null>(null)
const submitting = ref(false)
const deleting = ref(false)
const deleteUserInfo = ref<UserRow | null>(null)

// 表单
const userFormRef = ref()
const userForm = ref({
  username: '',
  realName: '',
  phone: '',
  role: 'resident' as UserRow['role'],
  status: 'active' as UserRow['status'],
  password: ''
})

// 表单校验
const userFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 20, message: '2~20位字符', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请填写手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: true, message: '请设置密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }]
}

// 角色tag配色
function getRoleType(role: string) {
  const map: Record<string, string> = {
    resident: 'success',
    family: 'warning',
    staff: 'primary',
    admin: 'danger'
  }
  return map[role] || 'info'
}
function getRoleText(role: string) {
  const map: Record<string, string> = { resident: '居民用户', family: '家属用户', staff: '工作人员', admin: '管理员' }
  return map[role] || role
}

// 加载列表 —— 真实接口，无假数据
async function loadUsers() {
  loading.value = true
  try {
    const page = await getUsersByRole(filterRole.value, currentPage.value, pageSize.value)
    users.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

// 表单重置
function resetForm() {
  userForm.value = { username: '', realName: '', phone: '', role: 'resident', status: 'active', password: '' }
  editingUser.value = null
  userFormRef.value?.resetFields()
}

// 编辑
function editUser(user: UserRow) {
  editingUser.value = user
  // userForm.value = { ...user, password: '' }
  showAddDialog.value = true
}

// 保存
async function saveUser() {
  await userFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    await new Promise(r => setTimeout(r, 400))
    submitting.value = false
    ElMessage.success(editingUser.value ? '修改成功' : '新增成功')
    showAddDialog.value = false
    loadUsers()
  })
}

// 启用禁用
function toggleUserStatus(user: UserRow) {
  const tip = user.status === 'active' ? '禁用' : '启用'
  ElMessageBox.confirm(`确认${tip}【${user.username}】账号？`, '提示', { type: 'warning' }).then(() => {
    user.status = user.status === 'active' ? 'disabled' : 'active'
    ElMessage.success(`${tip}成功`)
    loadUsers()
  }).catch(() => {})
}

// 删除
function deleteUser(user: UserRow) {
  deleteUserInfo.value = user
  showDeleteDialog.value = true
}
async function confirmDelete() {
  deleting.value = true
  await new Promise(r => setTimeout(r, 400))
  deleting.value = false
  showDeleteDialog.value = false
  ElMessage.success('删除完成')
  loadUsers()
}

onMounted(loadUsers)
</script>

<style scoped>
.user-management-container {
  max-width: 75rem;
  margin: 0 auto;
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

/* 筛选卡片样式（白色圆角） */
.filter-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap:16px;
  background: #fff;
  padding:20px 24px;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  margin-bottom:20px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap:14px;
  flex-wrap: wrap;
}
.search-inp {
  width:220px;
}
.sel-item {
  width:145px;
}
.filter-right {
  flex-shrink: 0;
}
/* 深色填充按钮 匹配截图黑底白字 */
.btn-dark {
  background-color: #222633 !important;
  color: #fff !important;
  border: none !important;
  border-radius: 8px !important;
  padding: 0 18px !important;
}

/* 表格卡片 */
.table-card {
  background:#fff;
  border-radius:14px;
  box-shadow:0 2px 12px rgba(0,0,0,0.05);
  overflow: hidden;
}
.page-wrap {
  margin:20px 24px;
  display:flex;
  justify-content: flex-end;
}

/* 角色/状态 tag 实色填充 */
.role-tag, .status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: none !important;
  border-radius: 20px;
  font-size: 13px;
}

/* 角色颜色覆盖 */
.role-tag:deep(.el-tag) { border: none; }

/* 居民用户 - 更浅的绿 */
:deep(.el-tag--info) {
  background-color: #f0faf0 !important;
  color: #4a9e5c !important;
  border: none !important;
}

/* 家属用户 - 更浅的橙 */
:deep(.el-tag--warning) {
  background-color: #fff8ee !important;
  color: #d07a1a !important;
  border: none !important;
}

/* 工作人员 - 更浅的蓝 */
:deep(.el-tag--primary) {
  background-color: #f0f7ff !important;
  color: #3a7cc7 !important;
  border: none !important;
}

/* 管理员 - 更浅的红 */
:deep(.el-tag--danger) {
  background-color: #fff2f2 !important;
  color: #d94f4f !important;
  border: none !important;
}

/* 正常状态 */
.status-tag.el-tag--success {
  background-color: #f0faf0 !important;
  color: #4a9e5c !important;
  border: none !important;
}

/* 禁用状态 */
.status-tag.el-tag--danger {
  background-color: #fff2f2 !important;
  color: #d94f4f !important;
  border: none !important;
}

/* 操作按钮三色样式 */
.opt-btn-group {
  display: flex;
  flex-direction: column;
  gap:6px;
}
.opt-edit {
  background-color: #222633 !important;
  color: #fff !important;
  border: none;
  border-radius:6px;
}
.opt-switch {
  color:#e69500 !important;
  background: transparent;
  border: none;
}
.opt-del {
  color:#f25656 !important;
  background: transparent;
  border: none;
}

/* 删除弹窗提示 */
.delete-tip {
  text-align:center;
  padding:10px 0;
}
.tip-red {
  color:#f56c6c;
  margin-top:6px;
  font-size:13px;
}

/* 移动端适配 */
@media (max-width:640px) {
  .filter-card {
    flex-direction:column;
    align-items:stretch;
  }
  .filter-left {
    flex-direction:column;
  }
  .search-inp,.sel-item {
    width:100% !important;
  }
}
</style>