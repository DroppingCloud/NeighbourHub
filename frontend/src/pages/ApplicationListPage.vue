<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>我的申请</span>
        <el-button type="primary" @click="$router.push('/application/submit')">发起新申请</el-button>
      </div>
    </template>
    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable>
          <el-option v-for="s in dictStore.dicts.applicationStatus" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="itemName" label="事项名称" />
      <el-table-column prop="category" label="类别" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <TaskStatusTag :status="row.status" type="application" />
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" link @click="$router.push(`/application/${row.applicationId}`)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getApplicationList } from '@/api/application'
import { useDictStore } from '@/stores/dictStore'
import TaskStatusTag from '@/components/TaskStatusTag.vue'

const dictStore = useDictStore()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = ref({ status: '', pageNum: 1, pageSize: 10 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getApplicationList(query.value)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
