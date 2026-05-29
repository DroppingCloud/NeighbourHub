<template>
  <div class="ai-guide-container">
    <div class="chat-header">
      <h2>AI智能导办助手</h2>
      <p>输入您的问题，我将为您推荐合适的政务服务</p>
    </div>

    <div class="suggest-questions">
      <span class="suggest-label">常见问题：</span>
      <el-tag
        v-for="q in suggestQuestions"
        :key="q"
        class="suggest-tag"
        @click="sendMessage(q)"
      >
        {{ q }}
      </el-tag>
    </div>

    <div class="chat-area" ref="chatAreaRef">
      <div v-for="(msg, idx) in messages" :key="idx" class="message" :class="msg.role">
        <div class="message-avatar">
          <el-avatar v-if="msg.role === 'user'" :size="36" :icon="UserFilled" />
          <div v-else class="ai-avatar">AI</div>
        </div>
        <div class="message-content">
          <div class="message-text">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
      <div v-if="isLoading" class="message assistant">
        <div class="message-avatar">
          <div class="ai-avatar">AI</div>
        </div>
        <div class="message-content">
          <div class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="请输入您的问题，例如：老年补贴怎么办？居住证需要哪些材料？"
        @keyup.ctrl.enter="sendMessage()"
      />
      <el-button type="primary" :loading="isLoading" @click="sendMessage()">
        <el-icon><Promotion /></el-icon>
        发送
      </el-button>
    </div>

    <el-drawer v-model="showResultDrawer" title="智能匹配结果" direction="rtl" size="500px">
      <div v-if="matchedService" class="match-result">
        <div class="result-header">
          <el-tag type="success" size="large">匹配成功</el-tag>
          <h3>{{ matchedService.name }}</h3>
        </div>
        
        <div class="result-section">
          <h4>办理条件</h4>
          <p>{{ matchedService.conditions }}</p>
        </div>
        
        <div class="result-section">
          <h4>所需材料</h4>
          <ul>
            <li v-for="material in matchedService.materials" :key="material">{{ material }}</li>
          </ul>
        </div>
        
        <div class="result-section">
          <h4>办理流程</h4>
          <el-steps :active="0" align-center>
            <el-step v-for="step in matchedService.process" :key="step" :title="step" />
          </el-steps>
        </div>
        
        <div class="result-actions">
          <el-button type="primary" @click="startApply">开始办理</el-button>
          <el-button @click="showResultDrawer = false">关闭</el-button>
        </div>
      </div>
      <div v-else class="no-result">
        <el-empty description="未找到匹配事项，请尝试更具体的描述" />
        <div class="manual-help">
          <p>您也可以：</p>
          <el-button link @click="contactStaff">联系社区工作人员</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Promotion } from '@element-plus/icons-vue'
import { chatGuide, recommendGuide } from '@/api/guide'

const router = useRouter()

interface Message {
  role: 'user' | 'assistant'
  content: string
  time: string
}

const inputMessage = ref('')
const isLoading = ref(false)
const showResultDrawer = ref(false)
const matchedService = ref<any>(null)
const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: '您好！我是智慧社区AI导办助手。您可以向我咨询政务事项的办理条件、所需材料、办理流程等问题。例如："老年补贴怎么申请？"或"居住证需要什么材料？"',
    time: new Date().toLocaleTimeString()
  }
])

const suggestQuestions = [
  '老年补贴怎么办？',
  '居住证需要哪些材料？',
  '如何开具居住证明？',
  '办理流程要多久？'
]

const chatAreaRef = ref<HTMLElement>()

function scrollToBottom() {
  nextTick(() => {
    if (chatAreaRef.value) {
      chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
    }
  })
}

async function sendMessage(text?: string) {
  const question = text || inputMessage.value.trim()
  if (!question) return

  messages.value.push({
    role: 'user',
    content: question,
    time: new Date().toLocaleTimeString()
  })
  inputMessage.value = ''
  scrollToBottom()

  isLoading.value = true
  try {
    const result = await recommendGuide({
      residentType: 'local',
      needType: question,
      description: question
    })
    const answer = await chatGuide({ message: question }).catch(() => '')
    isLoading.value = false

    const matched = result.items?.[0]
    if (matched) {
      matchedService.value = {
        id: matched.itemId,
        name: matched.itemName,
        category: matched.category,
        description: matched.description,
        conditions: matched.description || result.tips,
        materials: result.materials || [],
        process: result.steps || []
      }
      messages.value.push({
        role: 'assistant',
        content: answer || `根据您的问题，我为您匹配到了「${matched.itemName}」。是否需要查看详细的办理指南？`,
        time: new Date().toLocaleTimeString()
      })
      showResultDrawer.value = true
    } else {
      messages.value.push({
        role: 'assistant',
        content: answer || '抱歉，我没有完全理解您的问题。您可以尝试更具体的描述，或者联系社区工作人员获取帮助。',
        time: new Date().toLocaleTimeString()
      })
    }
    scrollToBottom()
  } finally {
    isLoading.value = false
  }
}

function startApply() {
  showResultDrawer.value = false
  router.push('/application/submit')
  ElMessage.success('即将跳转到事项办理页面')
}

function contactStaff() {
  ElMessage.info('请联系社区服务热线：12345')
}
</script>

<style scoped>
.ai-guide-container {
  max-width: 56.25rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 8.75rem);
}

.chat-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.chat-header h2 {
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.chat-header p {
  color: var(--text-muted);
}

.suggest-questions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  padding: 0.75rem 1rem;
  background: var(--card-bg);
  border-radius: var(--radius-md);
}

.suggest-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.suggest-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.suggest-tag:hover {
  background: var(--ink);
  color: #fff;
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  margin-bottom: 1.25rem;
}

.message {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.message.user {
  flex-direction: row-reverse;
}

.message.user .message-content {
  background: var(--ink);
  color: #fff;
}

.message.assistant .message-content {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.message-content {
  max-width: 70%;
  padding: 0.75rem 1rem;
  border-radius: 1rem;
}

.message.user .message-content {
  border-radius: 1rem 0.25rem 1rem 1rem;
}

.message.assistant .message-content {
  border-radius: 0.25rem 1rem 1rem 1rem;
}

.message-time {
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 0.375rem;
}

.ai-avatar {
  width: 2.25rem;
  height: 2.25rem;
  background: linear-gradient(135deg, var(--gold) 0%, var(--gold-light) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #fff;
}

.typing-indicator {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem 0;
}

.typing-indicator span {
  width: 0.5rem;
  height: 0.5rem;
  background: var(--text-muted);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-0.625rem); opacity: 1; }
}

.chat-input {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
}

.match-result {
  padding: 1.25rem;
}

.result-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.result-header h3 {
  margin-top: 0.75rem;
  color: var(--text-primary);
}

.result-section {
  margin-bottom: 1.5rem;
}

.result-section h4 {
  font-size: 1rem;
  margin-bottom: 0.75rem;
  color: var(--text-primary);
}

.result-section p {
  color: var(--text-secondary);
}

.result-section ul {
  padding-left: 1.25rem;
}

.result-section li {
  margin: 0.5rem 0;
  color: var(--text-secondary);
}

.result-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.no-result {
  padding: 2.5rem 1.25rem;
  text-align: center;
}

.manual-help {
  margin-top: 1.25rem;
}
</style>
