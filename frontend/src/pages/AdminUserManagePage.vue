<template>
  <div class="user-management-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-select v-model="filterRole" clearable placeholder="角色筛选" @change="loadUsers">
        <el-option label="居民" value="resident" />
        <el-option label="家属" value="family" />
        <el-option label="工作人员" value="staff" />
        <el-option label="管理员" value="admin" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="users" border>
      <el-table-column prop="userId" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)">{{ getRoleText(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
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

const users = ref<UserRow[]>([])
const loading = ref(false)
const filterRole = ref('')

function getRoleType(role: string) {
  const map: Record<string, string> = {
    resident: '',
    family: 'warning',
    staff: 'primary',
    admin: 'danger'
  }
  return map[role] || 'info'
}

function getRoleText(role: string) {
  const map: Record<string, string> = {
    resident: '居民用户',
    family: '家属用户',
    staff: '工作人员',
    admin: '管理员'
  }
  return map[role] || role
}

async function loadUsers() {
  loading.value = true
  try {
    const page = await getUsersByRole(filterRole.value, 1, 100)
    users.value = page.records || []
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.user-management-container {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: var(--text-primary);
}
</style>
