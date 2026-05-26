<template>
  <el-card>
    <template #header>
      <span>工单管理</span>
    </template>
    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable>
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="需补件" value="supplement_required" />
          <el-option label="已办结" value="completed" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderId" label="工单号" width="80" />
      <el-table-column prop="itemName" label="事项名称" />
      <el-table-column prop="residentName" label="申请人" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <TaskStatusTag :status="row.status" type="workorder" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" link @click="$router.push(`/workorder/${row.orderId}`)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWorkOrderList } from '@/api/workOrder'
import TaskStatusTag from '@/components/TaskStatusTag.vue'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = ref({ status: '', pageNum: 1, pageSize: 10 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWorkOrderList(query.value)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
