<template>
  <div class="guide-page">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card header="智能导办 · AI 咨询">
          <div class="chat-box" ref="chatBoxRef">
            <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-msg', msg.role]">
              <span>{{ msg.content }}</span>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="inputText" placeholder="描述您的办事需求，例如：我想申请老年补贴" @keyup.enter="sendMessage" />
            <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="推荐事项">
          <el-empty v-if="!guideResult" description="请在左侧描述您的需求" />
          <template v-else>
            <ServiceCard v-for="item in guideResult.items" :key="item.itemId" :item="item" @apply="goApply" />
            <el-divider>所需材料</el-divider>
            <el-tag v-for="(m, i) in guideResult.materials" :key="i" style="margin:4px">{{ m }}</el-tag>
            <el-divider>办理步骤</el-divider>
            <el-steps :active="guideResult.steps.length" direction="vertical" finish-status="success">
              <el-step v-for="(step, i) in guideResult.steps" :key="i" :title="step" />
            </el-steps>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import ServiceCard from '@/components/ServiceCard.vue'
import { recommendGuide } from '@/api/guide'
import type { GuideResult } from '@/api/guide'

const router = useRouter()
const inputText = ref('')
const loading = ref(false)
const guideResult = ref<GuideResult | null>(null)
const messages = ref<Array<{ role: 'user' | 'assistant'; content: string }>>([
  { role: 'assistant', content: '您好！请描述您的办事需求，我来帮您推荐合适的服务事项。' }
])

const sendMessage = async () => {
  if (!inputText.value.trim()) return
  const text = inputText.value
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  try {
    guideResult.value = await recommendGuide({ description: text })
    messages.value.push({ role: 'assistant', content: `已为您推荐 ${guideResult.value.items.length} 个相关事项，请查看右侧结果。` })
  } finally {
    loading.value = false
  }
}

const goApply = (itemId: number) => {
  router.push({ path: '/application/submit', query: { itemId } })
}
</script>

<style scoped>
.chat-box {
  height: 360px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
}
.chat-msg { margin: 8px 0; max-width: 80%; }
.chat-msg.user { text-align: right; margin-left: auto; }
.chat-msg.user span { background: #409eff; color: #fff; padding: 8px 12px; border-radius: 8px; display: inline-block; }
.chat-msg.assistant span { background: #fff; color: #303133; padding: 8px 12px; border-radius: 8px; display: inline-block; border: 1px solid #e4e7ed; }
.chat-input { display: flex; gap: 8px; }
</style>
