<template>
  <div class="home">

    <!-- 欢迎区 -->
    <div class="welcome-bar">
      <div class="welcome-left">
        <div class="welcome-avatar">{{ avatarChar }}</div>
        <div>
          <h1 class="welcome-title">你好，{{ userStore.userInfo?.username }}</h1>
          <p class="welcome-sub">{{ greeting }} · {{ dateStr }}</p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="role-tag">{{ roleLabel }}</div>
      </div>
    </div>

    <!-- 统计数字（居民/家属） -->
    <div v-if="isResidentOrFamily" class="stat-row">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="`background:${s.bg}`">
          <component :is="s.icon" style="font-size:17px" :style="`color:${s.color}`" />
        </div>
        <div>
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 功能入口卡片 -->
    <h2 class="section-title">功能入口</h2>
    <div class="entry-grid">
      <div
        v-for="(card, i) in entryCards"
        :key="card.path"
        class="entry-card"
        :style="`animation-delay:${i * 0.06}s`"
        @click="$router.push(card.path)"
      >
        <div class="entry-icon-wrap" :style="`background:${card.bg}`">
          <component :is="card.icon" style="font-size:22px" :style="`color:${card.color}`" />
        </div>
        <div class="entry-info">
          <div class="entry-title">{{ card.title }}</div>
          <div class="entry-desc">{{ card.desc }}</div>
        </div>
        <div class="entry-arrow">
          <svg viewBox="0 0 12 12" fill="none" width="12" height="12">
            <path d="M3 6h6M7 4l2 2-2 2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- 最近通知 -->
    <div class="notice-section" v-if="noticeStore.unreadCount">
      <h2 class="section-title">
        未读消息
        <span class="notice-count">{{ noticeStore.unreadCount }}</span>
      </h2>
      <div class="notice-tip" @click="$router.push('/notice')">
        <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16" style="color:#5B7FA6;flex-shrink:0">
          <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zm0 16a2 2 0 01-2-2h4a2 2 0 01-2 2z"/>
        </svg>
        <span>您有 <strong>{{ noticeStore.unreadCount }}</strong> 条未读消息，点击查看详情</span>
        <svg viewBox="0 0 12 12" fill="none" width="11" height="11" style="margin-left:auto;opacity:.5">
          <path d="M3 6h6M7 4l2 2-2 2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { useNoticeStore } from '@/stores/noticeStore'

const userStore   = useUserStore()
const noticeStore = useNoticeStore()

const avatarChar = computed(() => (userStore.userInfo?.username || '?')[0].toUpperCase())

const isResidentOrFamily = computed(() =>
  userStore.isResident || userStore.hasRole('ROLE_FAMILY')
)

// 问候语
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了，注意休息'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 日期
const dateStr = computed(() => {
  const d = new Date()
  const days = ['周日','周一','周二','周三','周四','周五','周六']
  return `${d.getMonth()+1}月${d.getDate()}日 ${days[d.getDay()]}`
})

// 角色标签
const roleLabel = computed(() => {
  if (userStore.isAdmin) return '系统管理员'
  if (userStore.isStaff) return '社区工作人员'
  if (userStore.hasRole('ROLE_FAMILY')) return '家属代理人'
  return '社区居民'
})

// 统计数字（占位）
const stats = [
  { label: '进行中申请', value: '—', icon: 'Document',     color: '#5B7FA6', bg: 'rgba(91,127,166,0.1)' },
  { label: '待处理补件', value: '—', icon: 'Warning',       color: '#D4935A', bg: 'rgba(212,147,90,0.1)' },
  { label: '服务预约',   value: '—', icon: 'Calendar',      color: '#5A9E7A', bg: 'rgba(90,158,122,0.1)' },
  { label: '已完成事项', value: '—', icon: 'CircleCheck',   color: '#7A9EC0', bg: 'rgba(122,158,192,0.1)' },
]

// 功能入口
const entryCards = computed(() => {
  const cards: any[] = []
  if (isResidentOrFamily.value) {
    cards.push(
      { path: '/guide',            title: '智能导办',   desc: 'AI 助您快速找到所需服务', icon: 'MagicStick', color: '#5B7FA6', bg: 'rgba(91,127,166,0.1)' },
      { path: '/application/list', title: '我的申请',   desc: '查看事项申请进度与状态',   icon: 'Document',   color: '#7A9EC0', bg: 'rgba(122,158,192,0.1)' },
      { path: '/booking',          title: '服务预约',   desc: '预约助餐、陪诊等便民服务', icon: 'Calendar',   color: '#5A9E7A', bg: 'rgba(90,158,122,0.1)' },
      { path: '/progress',         title: '进度查询',   desc: '实时了解业务办理进度',     icon: 'Search',     color: '#8A7AC0', bg: 'rgba(138,122,192,0.1)' },
      { path: '/notice',           title: '消息通知',   desc: '查看审核结果与系统通知',   icon: 'Bell',       color: '#C09A5A', bg: 'rgba(192,154,90,0.1)' },
    )
  }
  if (userStore.isStaff) {
    cards.push(
      { path: '/workorder', title: '工单管理', desc: '处理待审核事项与居民申请', icon: 'Files',        color: '#5B7FA6', bg: 'rgba(91,127,166,0.1)' },
      { path: '/notice',    title: '消息通知', desc: '查看系统通知',           icon: 'Bell',         color: '#C09A5A', bg: 'rgba(192,154,90,0.1)' },
    )
  }
  if (userStore.isAdmin) {
    cards.push(
      { path: '/admin/dashboard',     title: '统计看板', desc: '查看平台运营数据与趋势', icon: 'DataAnalysis', color: '#5A9E7A', bg: 'rgba(90,158,122,0.1)' },
      { path: '/admin/service-config', title: '事项配置', desc: '管理政务事项规则与模板', icon: 'Setting',      color: '#5B7FA6', bg: 'rgba(91,127,166,0.1)' },
      { path: '/admin/user-manage',   title: '用户管理', desc: '管理平台用户与权限分配', icon: 'User',         color: '#8A7AC0', bg: 'rgba(138,122,192,0.1)' },
      { path: '/workorder',           title: '工单管理', desc: '查看全部工单与处理状态', icon: 'Files',        color: '#7A9EC0', bg: 'rgba(122,158,192,0.1)' },
      { path: '/notice',              title: '消息通知', desc: '查看系统通知',           icon: 'Bell',         color: '#C09A5A', bg: 'rgba(192,154,90,0.1)' },
    )
  }
  return cards
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500;600&display=swap');

.home {
  max-width: 900px;
  margin: 0 auto;
  font-family: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  animation: page-in .45s cubic-bezier(0.16,1,0.3,1) both;
}
@keyframes page-in {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: none; }
}

/* ─── 欢迎横幅 ─── */
.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #F0F5FB;
  border: 1px solid rgba(91,127,166,0.15);
  border-radius: 16px;
  padding: 22px 28px;
  margin-bottom: 22px;
}
.welcome-left { display: flex; align-items: center; gap: 16px; }
.welcome-avatar {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #7BAFD4, #4A7FA8);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #1E2E3E;
  margin: 0 0 4px;
}
.welcome-sub {
  font-size: 13px;
  color: #6A7F96;
  margin: 0;
}
.role-tag {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #5B7FA6;
  background: rgba(91,127,166,0.1);
  border: 1px solid rgba(91,127,166,0.2);
  letter-spacing: .4px;
}

/* ─── 统计行 ─── */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 28px;
}
.stat-card {
  background: #F7F9FC;
  border: 1px solid rgba(91,127,166,0.12);
  border-radius: 13px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: box-shadow 0.2s;
}
.stat-card:hover { box-shadow: 0 4px 16px rgba(60,90,140,0.1); }
.stat-icon {
  width: 40px; height: 40px;
  border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-num { font-size: 20px; font-weight: 700; color: #1E2E3E; line-height: 1.2; }
.stat-label { font-size: 11.5px; color: #8A9BB0; margin-top: 2px; }

/* ─── 区块标题 ─── */
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #3A526A;
  margin: 0 0 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px; height: 20px;
  border-radius: 50%;
  background: #E06060;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

/* ─── 功能卡片 ─── */
.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}
.entry-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: #F7F9FC;
  border: 1px solid rgba(91,127,166,0.12);
  border-radius: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s, background 0.18s;
  animation: card-in .4s cubic-bezier(0.16,1,0.3,1) both;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: none; }
}
.entry-card:hover {
  box-shadow: 0 6px 22px rgba(60,90,140,0.12);
  transform: translateY(-2px);
  background: #F0F5FB;
}
.entry-icon-wrap {
  width: 44px; height: 44px;
  border-radius: 13px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.entry-info { flex: 1; min-width: 0; }
.entry-title {
  font-size: 14px;
  font-weight: 600;
  color: #1E2E3E;
  margin-bottom: 3px;
}
.entry-desc { font-size: 12px; color: #8A9BB0; }
.entry-arrow {
  color: #B0C0D4;
  flex-shrink: 0;
  transition: transform 0.18s, color 0.18s;
}
.entry-card:hover .entry-arrow { transform: translateX(3px); color: #5B7FA6; }

/* ─── 通知提示条 ─── */
.notice-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 16px;
  background: rgba(91,127,166,0.06);
  border: 1px solid rgba(91,127,166,0.18);
  border-radius: 12px;
  font-size: 13px;
  color: #4A6A8A;
  cursor: pointer;
  transition: background 0.18s;
}
.notice-tip:hover { background: rgba(91,127,166,0.11); }
.notice-tip strong { color: #2C4A6E; }
</style>
