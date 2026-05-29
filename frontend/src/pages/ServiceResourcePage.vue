<template>
  <div class="service-resource-container">
    <div class="page-header">
      <h2>服务资源配置</h2>
      <p>服务资源、时段和排班模块需要后端新增资源表后启用</p>
    </div>

    <el-alert
      type="warning"
      show-icon
      :closable="false"
      title="该模块暂未开放"
      description="当前后端已完成事项、申请、材料、工单、预约、通知、用户和统计等接口；服务资源排班尚未建立对应数据库表和接口，因此页面不再使用本地模拟数据。"
    />

    <div class="link-grid">
      <div class="link-card" @click="router.push('/admin/service-config')">
        <h3>事项配置</h3>
        <p>维护政务事项、办理条件和材料模板</p>
      </div>
      <div class="link-card" @click="router.push('/admin/user-manage')">
        <h3>用户管理</h3>
        <p>查看居民、家属、工作人员和管理员账号</p>
      </div>
      <div class="link-card" @click="router.push('/staff/booking')">
        <h3>服务调度</h3>
        <p>处理居民提交的社区服务预约</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.service-resource-container {
  max-width: 75rem;
  margin: 0 auto;
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

.link-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(16rem, 100%), 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.link-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: all 0.2s ease;
}

.link-card:hover {
  transform: translateY(-0.125rem);
  box-shadow: var(--shadow-md);
}

.link-card h3 {
  font-size: 1rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.link-card p {
  color: var(--text-muted);
  line-height: 1.6;
  word-break: break-word;
}
</style>
