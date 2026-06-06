<template>
  <div class="service-resource-container">
    <div class="page-header">
      <div>
        <h2>服务资源管理</h2>
        <p>社区服务资源概览与快捷管理入口</p>
      </div>
    </div>

    <!-- 服务类型概览 -->
    <div class="section-title-row">
      <h3 class="section-title">服务类型</h3>
    </div>

    <div class="service-type-grid">
      <div
        v-for="(service, idx) in serviceTypes"
        :key="service.type"
        class="service-type-card"
        :style="{ '--card-idx': idx }"
        @click="router.push('/staff/booking')"
      >
        <div class="stc-icon" :class="service.color">
          <el-icon :size="24"><component :is="service.icon" /></el-icon>
        </div>
        <div class="stc-body">
          <h4>{{ service.label }}</h4>
          <p>{{ service.desc }}</p>
        </div>
        <div class="stc-stat">
          <span class="stc-count">{{ service.count }}</span>
          <span class="stc-unit">预约</span>
        </div>
      </div>
    </div>

    <!-- 快捷管理 -->
    <div class="section-title-row">
      <h3 class="section-title">快捷管理</h3>
    </div>

    <div class="quick-grid">
      <div class="quick-card" @click="router.push('/admin/service-config')">
        <div class="qc-icon"><el-icon><Document /></el-icon></div>
        <div class="qc-content">
          <h4>事项配置</h4>
          <p>维护政务事项、办理条件和材料模板</p>
        </div>
        <el-icon class="qc-arrow"><ArrowRight /></el-icon>
      </div>

      <div class="quick-card" @click="router.push('/admin/user-manage')">
        <div class="qc-icon"><el-icon><User /></el-icon></div>
        <div class="qc-content">
          <h4>用户管理</h4>
          <p>管理社区居民、家属、工作人员账号</p>
        </div>
        <el-icon class="qc-arrow"><ArrowRight /></el-icon>
      </div>

      <div class="quick-card" @click="router.push('/staff/booking')">
        <div class="qc-icon"><el-icon><Calendar /></el-icon></div>
        <div class="qc-content">
          <h4>服务调度</h4>
          <p>处理居民提交的社区服务预约请求</p>
        </div>
        <el-icon class="qc-arrow"><ArrowRight /></el-icon>
      </div>

      <div class="quick-card" @click="router.push('/admin/statistics')">
        <div class="qc-icon"><el-icon><DataAnalysis /></el-icon></div>
        <div class="qc-content">
          <h4>统计分析</h4>
          <p>查看服务数据和运营指标</p>
        </div>
        <el-icon class="qc-arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 提示信息 -->
    <div class="info-banner">
      <div class="info-icon"><el-icon><InfoFilled /></el-icon></div>
      <div class="info-text">
        <strong>资源排班功能规划中</strong>
        <p>服务时段配置、工作人员排班等高级功能正在开发中，敬请期待。目前可通过「服务调度」手动分配服务工作人员。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Document, User, Calendar, DataAnalysis, ArrowRight, InfoFilled, Bowl, FirstAidKit, House } from '@element-plus/icons-vue'
import { getStaffBookingList } from '@/api/booking'

const router = useRouter()

const serviceTypes = ref([
  { type: 'dining', label: '助餐服务', desc: '社区食堂配送到家', icon: Bowl, color: 'blue', count: 0 },
  { type: 'accompany', label: '陪诊服务', desc: '陪同就医、取药', icon: FirstAidKit, color: 'green', count: 0 },
  { type: 'home_visit', label: '上门服务', desc: '家政清洁、维修探访', icon: House, color: 'gold', count: 0 }
])

onMounted(async () => {
  try {
    const page = await getStaffBookingList(1, 200)
    const records = page?.records || []
    for (const st of serviceTypes.value) {
      st.count = records.filter((r: any) => r.serviceType === st.type).length
    }
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.service-resource-container {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.75rem;
}

.page-header h2 {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.page-header p {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.section-title-row {
  margin-bottom: 0.875rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

/* ====== Service Type Cards ====== */
.service-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(18rem, 100%), 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.service-type-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  animation: card-stagger 0.4s ease both;
  animation-delay: calc(var(--card-idx, 0) * 0.08s);
}

@keyframes card-stagger {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.service-type-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: rgba(212, 168, 67, 0.3);
}

.stc-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stc-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.stc-icon.green {
  background: rgba(39, 174, 96, 0.1);
  color: #27ae60;
}

.stc-icon.gold {
  background: rgba(212, 168, 67, 0.1);
  color: var(--gold);
}

.stc-body {
  flex: 1;
  min-width: 0;
}

.stc-body h4 {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.125rem;
}

.stc-body p {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin: 0;
}

.stc-stat {
  text-align: center;
  flex-shrink: 0;
}

.stc-count {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stc-unit {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

/* ====== Quick Cards ====== */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(16rem, 100%), 1fr));
  gap: 0.875rem;
  margin-bottom: 2rem;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: all 0.25s;
}

.quick-card:hover {
  border-color: var(--gold);
  background: rgba(212, 168, 67, 0.02);
  transform: translateX(3px);
}

.quick-card:hover .qc-arrow {
  color: var(--gold);
  transform: translateX(3px);
}

.qc-icon {
  width: 2.5rem;
  height: 2.5rem;
  background: var(--bg-tertiary);
  border-radius: 0.625rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  font-size: 1.125rem;
  flex-shrink: 0;
}

.qc-content {
  flex: 1;
  min-width: 0;
}

.qc-content h4 {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.125rem;
}

.qc-content p {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin: 0;
}

.qc-arrow {
  color: var(--text-muted);
  font-size: 0.875rem;
  flex-shrink: 0;
  transition: all 0.25s;
}

/* ====== Info Banner ====== */
.info-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  background: rgba(212, 168, 67, 0.04);
  border: 1px solid rgba(212, 168, 67, 0.15);
  border-radius: var(--radius-md);
  padding: 1.125rem 1.25rem;
}

.info-icon {
  color: var(--gold);
  font-size: 1.25rem;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.info-text strong {
  display: block;
  font-size: 0.875rem;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.info-text p {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--text-muted);
  line-height: 1.5;
}

@media (max-width: 48rem) {
  .service-type-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
