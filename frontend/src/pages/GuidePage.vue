<template>
  <div class="ai-guide-container">
    <!-- 简洁头部 -->
    <div class="chat-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="24"><MagicStick /></el-icon>
        </div>
        <div class="header-text">
          <h2>AI智能导办助手</h2>
          <p>智能对话 | 一键办理</p>
        </div>
      </div>
    </div>

    <!-- 常见问题快捷入口 -->
    <div class="suggest-questions">
      <div class="suggest-tags">
        <el-tag
          v-for="q in displayedSuggest"
          :key="q.text"
          :type="q.type"
          class="suggest-tag"
          size="default"
          effect="plain"
          @click="sendMessage(q.text)"
        >
          {{ q.text }}
        </el-tag>
        <button
          v-if="suggestQuestions.length > 4"
          type="button"
          class="suggest-toggle"
          @click="showAllSuggest = !showAllSuggest"
        >
          {{ showAllSuggest ? '收起' : '更多' }}
        </button>
      </div>
    </div>

    <!-- 对话区域 -->
    <div class="chat-area" ref="chatAreaRef">
      <div v-for="(msg, idx) in messages" :key="idx" class="message" :class="msg.role">
        <div class="message-avatar">
          <el-avatar v-if="msg.role === 'user'" :size="36" :icon="UserFilled" />
          <div v-else class="ai-avatar">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
        </div>
        <div class="message-content-wrapper">
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
            
            <!-- 智能操作按钮 -->
            <div v-if="msg.showActions && !msg.isWelcome" class="message-actions">
              <div class="msg-type-badge">
                <el-tag size="small" type="warning">{{ msg.actionType === 'booking' ? '服务预约' : '事项办理' }}</el-tag>
              </div>
              <el-button 
                v-if="msg.actionType === 'booking'"
                type="primary" 
                size="default" 
                @click="goToBooking(msg.pendingItem)" 
                class="action-primary-btn"
              >
                📅 立即预约
              </el-button>
              <el-button 
                v-else
                type="primary" 
                size="default" 
                @click="handleStartProcess(msg.pendingItem)" 
                class="action-primary-btn"
              >
                🚀 开始办理
              </el-button>
            </div>
          </div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
      
      <!-- 加载动画：若正在流式渲染（有 currentStreamingIndex），始终隐藏全局指示器 -->
      <div v-if="isLoading && currentStreamingIndex === null" class="message assistant">
        <div class="message-avatar">
          <div class="ai-avatar">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
        </div>
        <div class="message-content-wrapper">
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-wrapper">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题，例如：老年补贴怎么申请？"
          @keyup.enter.ctrl="sendMessage()"
          :disabled="isLoading"
        />
        <div class="input-actions">
          <div class="input-hint">
            <el-icon><Promotion /></el-icon>
            Ctrl + Enter 发送
          </div>
          <el-button 
            type="primary" 
            :loading="isLoading" 
            @click="sendMessage()"
            :disabled="!inputMessage.trim()"
            size="default"
            class="send-btn"
          >
            发送消息
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  MagicStick, UserFilled, Promotion
} from '@element-plus/icons-vue'
import { chatGuideStream } from '@/api/guide'

const router = useRouter()

const GUIDE_CHAT_STORAGE_KEY_PREFIX = 'neighbourhub_ai_guide_chat_state'

const APPLICATION_ACTION_ITEMS = ['居住证办理', '老年补贴申请', '老年补贴', '居住证明开具', '居住证明', '低保申请', '低保']
const BOOKING_ACTION_ITEMS = [
  { keywords: ['助餐服务', '助餐'], type: 'dining' },
  { keywords: ['陪诊服务', '陪诊'], type: 'accompany' },
  { keywords: ['上门服务', '上门'], type: 'home_visit' }
]

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time: string
  showActions?: boolean
  isWelcome?: boolean
  actionType?: 'booking' | 'application'
  pendingItem?: string
}

interface StoredGuideChatState {
  sessionId: string
  pendingItemName: string | null
  inputMessage: string
  messages: ChatMessage[]
}

const inputMessage = ref('')
const isLoading = ref(false)
const sessionId = ref(generateSessionId())
const pendingItemName = ref<string | null>(null)
const currentStreamingIndex = ref<number | null>(null)

const welcomeMessage = `您好！我是AI导办助手 🤖

我可以帮您：
• 查询政务事项办理条件和流程
• 推荐适合您的政务服务
• 提供详细的材料清单
• 解答办事过程中的疑问

请问您想咨询什么问题？`

const messages = ref<ChatMessage[]>(getDefaultMessages())

const suggestQuestions = [
  { text: '老年补贴怎么申请？', type: 'primary' },
  { text: '居住证需要哪些材料？', type: 'success' },
  { text: '助餐服务需要符合什么条件才能预约？', type: 'warning' },
  { text: '怎么进行上门服务预约？', type: 'info' },
  { text: '办理流程要多久？', type: '' },
  { text: '怎么预约社区服务？', type: '' }
]

const showAllSuggest = ref(false)
const displayedSuggest = computed(() => showAllSuggest.value ? suggestQuestions : suggestQuestions.slice(0, 4))

const chatAreaRef = ref<HTMLElement>()

function getDefaultMessages(): ChatMessage[] {
  return [
    {
      role: 'assistant',
      content: welcomeMessage,
      time: getCurrentTime(),
      showActions: false,
      isWelcome: true
    }
  ]
}

function isValidChatMessage(value: unknown): value is ChatMessage {
  if (!value || typeof value !== 'object') return false
  const msg = value as Partial<ChatMessage>
  return (
    (msg.role === 'user' || msg.role === 'assistant') &&
    typeof msg.content === 'string' &&
    typeof msg.time === 'string'
  )
}

function sanitizeMessages(value: unknown): ChatMessage[] {
  if (!Array.isArray(value)) return getDefaultMessages()
  const sanitized = value
    .filter(isValidChatMessage)
    .filter(msg => msg.isWelcome || msg.content.trim())
    .map(msg => {
      if (msg.role !== 'assistant' || msg.isWelcome) return msg
      let { showActions, actionType, itemName } = analyzeMessage(msg.content)
      let content = normalizeActionCopy(msg.content, showActions, actionType)
      ;({ showActions, actionType, itemName } = analyzeMessage(content))
      return {
        ...msg,
        content,
        showActions,
        actionType,
        pendingItem: itemName || msg.pendingItem
      }
    })
  return sanitized.length > 0 ? sanitized : getDefaultMessages()
}

function loadGuideChatState(): StoredGuideChatState | null {
  try {
    migrateLegacyGuideChatState()
    const raw = localStorage.getItem(getGuideChatStorageKey())
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<StoredGuideChatState>
    return {
      sessionId: typeof parsed.sessionId === 'string' && parsed.sessionId ? parsed.sessionId : generateSessionId(),
      pendingItemName: typeof parsed.pendingItemName === 'string' ? parsed.pendingItemName : null,
      inputMessage: typeof parsed.inputMessage === 'string' ? parsed.inputMessage : '',
      messages: sanitizeMessages(parsed.messages)
    }
  } catch (error) {
    console.warn('Failed to load guide chat state:', error)
    return null
  }
}

function saveGuideChatState() {
  try {
    const state: StoredGuideChatState = {
      sessionId: sessionId.value,
      pendingItemName: pendingItemName.value,
      inputMessage: inputMessage.value,
      messages: sanitizeMessages(messages.value)
    }
    localStorage.setItem(getGuideChatStorageKey(), JSON.stringify(state))
  } catch (error) {
    console.warn('Failed to save guide chat state:', error)
  }
}

function getCurrentUserStorageId(): string {
  try {
    const raw = localStorage.getItem('userInfo')
    if (!raw) return 'guest'
    const user = JSON.parse(raw) as { userId?: string | number; username?: string }
    return String(user.userId || user.username || 'guest')
  } catch {
    return 'guest'
  }
}

function getGuideChatStorageKey(): string {
  return `${GUIDE_CHAT_STORAGE_KEY_PREFIX}:${getCurrentUserStorageId()}`
}

function migrateLegacyGuideChatState() {
  const legacy = localStorage.getItem(GUIDE_CHAT_STORAGE_KEY_PREFIX)
  if (!legacy) return
  const scopedKey = getGuideChatStorageKey()
  if (!localStorage.getItem(scopedKey)) {
    localStorage.setItem(scopedKey, legacy)
  }
  localStorage.removeItem(GUIDE_CHAT_STORAGE_KEY_PREFIX)
}

function restoreGuideChatState() {
  const stored = loadGuideChatState()
  if (!stored) return
  sessionId.value = stored.sessionId
  pendingItemName.value = stored.pendingItemName
  inputMessage.value = stored.inputMessage
  messages.value = stored.messages
}

function generateSessionId(): string {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

function getCurrentTime(): string {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false }).slice(0, 5)
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeGuideText(text: string): string {
  return text
    .replace(/\r\n?/g, '\n')
    .replace(/^\s*#{1,6}\s*$/gm, '')
    .replace(/[【\[]\s*[】\]]/g, '')
    .replace(/^\s*\|[\s|:-]*\|\s*$/gm, '')
    .replace(/([^\n])(\s*#{1,6}\s+)/g, '$1\n$2')
    .replace(/([^\n])([一二三四五六七八九十]+[、.．])/g, '$1\n$2')
    .replace(/([^\n])(\d+[.、．)]\s*)/g, '$1\n$2')
    .replace(/(\d+[.、．)])([^\s\n])/g, '$1 $2')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function isSectionHeading(match: RegExpMatchArray | null): boolean {
  if (!match) return false
  const tail = (match[2] || '').trim()
  return tail.length <= 24 && !/[。！？；，,;]$/.test(tail)
}

function isStandaloneSubheading(line: string): boolean {
  return line.length <= 22 &&
    /^(?:[⏳📌📋🏢⏰❓]|第[一二三四五六七八九十]+步|方式[一二三四五六七八九十]+|常见问题|重要提醒|特别提醒|办理方式|办理时间|所需材料|现在您可以)/.test(line)
}

function parseTableRow(line: string): string[] | null {
  const stripped = line.trim()
  if (!stripped.includes('|')) return null
  const cells = stripped
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map(cell => cell.trim())
    .filter(Boolean)
  return cells.length >= 2 ? cells : null
}

/**
 * 将AI返回的文本转换为正确的HTML格式
 * 支持：Markdown标题、中文章节标题、数字列表、无序列表、加粗。
 */
function formatAIResponse(text: string): string {
  if (!text) return ''

  let html = escapeHtml(normalizeGuideText(text))
  html = html.replace(/\*\*([^*\n]{1,40})\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*\*/g, '')
  // 去除仅包含星号的孤立行（如流切片导致的残留 **）
  html = html.replace(/^\s*\*+\s*$/gm, '')
  
  // 按行分割并构建块级 HTML（paragraphs 和 lists），避免滥用 <br/>
  const lines = html.split('\n')

  const parts: string[] = []
  let paragraphBuffer: string[] = []
  let listBuffer: string[] = []
  let tableBuffer: string[][] = []
  let listType: 'ul' | 'ol' | null = null
  let orderedListStart: number | null = null

  const flushParagraph = () => {
    if (paragraphBuffer.length > 0) {
      const inner = paragraphBuffer.join('<br/>').trim()
      if (inner) parts.push(`<p class="msg-paragraph">${inner}</p>`)
      paragraphBuffer = []
    }
  }

  const flushList = () => {
    if (listBuffer.length > 0 && listType) {
      const inner = listBuffer.join('')
      const startAttr = listType === 'ol' && orderedListStart && orderedListStart > 1 ? ` start="${orderedListStart}"` : ''
      parts.push(`<${listType} class="msg-list"${startAttr}>${inner}</${listType}>
`)
      listBuffer = []
      listType = null
      orderedListStart = null
    }
  }

  const flushTable = () => {
    if (tableBuffer.length === 0) return
    const [header, ...rows] = tableBuffer
    if (rows.length === 0) {
      parts.push(`<p class="msg-paragraph">${header.join(' | ')}</p>`)
    } else {
      const thead = `<thead><tr>${header.map(cell => `<th>${cell}</th>`).join('')}</tr></thead>`
      const tbody = `<tbody>${rows.map(row => `<tr>${header.map((_, index) => `<td>${row[index] || ''}</td>`).join('')}</tr>`).join('')}</tbody>`
      parts.push(`<div class="msg-table-wrap"><table class="msg-table">${thead}${tbody}</table></div>`)
    }
    tableBuffer = []
  }

  for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
    const rawLine = lines[lineIndex]
    const line = rawLine.replace(/\r$/, '')
    const trimmed = line.trim()
    // 去掉所有 <strong> 标签用于格式识别，保留原始 line 用于输出
    const stripped = trimmed.replace(/<\/?strong>/gi, '').trim()

    const tableRow = parseTableRow(line)
    if (tableRow) {
      flushParagraph()
      flushList()
      tableBuffer.push(tableRow)
      continue
    }
    flushTable()

    // Markdown 标题支持：例如 ### 标题；空标题在 normalizeGuideText 中已移除。
    const mdHeading = stripped.match(/^(#{1,6})\s+(.+)$/)
    if (mdHeading) {
      flushList()
      flushTable()
      flushParagraph()
      parts.push(`<p class="msg-heading"><strong>${mdHeading[2]}</strong></p>`)
      continue
    }

    // Detect Chinese numbered headings like 一、二、三、 or Arabic numbered headings.
    const chineseHeading = stripped.match(/^([一二三四五六七八九十]+)[、.．）)]\s*(.+)$/)
    const arabicHeading = stripped.match(/^(\d+)[.、．)]\s*(.+)$/)

    // Detect ordered list (1. item) and unordered list (• - *)
    const orderedMatch = stripped.match(/^(\d+)[.、．)]\s+(.+)$/)
    const unorderedMatch = stripped.match(/^[•\-*]\s+(.+)$/)

    // If a heading-like line (Chinese numerals or Arabic) ends with a colon or looks like a section title, render as heading
    const isHeadingLine = (m: RegExpMatchArray | null) => {
      if (!m) return false
      const tail = (m[2] || '').trim()
      return /[:：]$/.test(tail) || /步骤|材料|流程|条件|方式|服务|申请|提示|说明/.test(tail)
    }

    if (chineseHeading && (isHeadingLine(chineseHeading) || isSectionHeading(chineseHeading))) {
      flushList()
      flushTable()
      flushParagraph()
      parts.push(`<p class="msg-heading"><strong>${chineseHeading[1]}、 ${chineseHeading[2]}</strong></p>`)
      continue
    }

    if (arabicHeading && isHeadingLine(arabicHeading)) {
      flushList()
      flushTable()
      flushParagraph()
      parts.push(`<p class="msg-heading"><strong>${arabicHeading[1]}. ${arabicHeading[2]}</strong></p>`)
      continue
    }

    if (orderedMatch) {
      // start or continue ordered list
      flushParagraph()
      flushTable()
      if (listType !== 'ol') {
        flushList()
        orderedListStart = Number(orderedMatch[1])
      }
      listType = 'ol'
      // 从原始 line 中去掉序号（可能被 <strong> 包裹），保留内容中的加粗等标记
      const contentAfterNumber = line.replace(/^\s*(?:<strong>\s*)?\d+[.、．)](?:\s*<\/strong>)?\s*/i, '')
      listBuffer.push(`<li>${contentAfterNumber}</li>`)
      continue
    }

    if (unorderedMatch) {
      flushParagraph()
      flushTable()
      if (listType !== 'ul') flushList()
      listType = 'ul'
      // 从原始 line 中去掉项目符号，保留其余内容（可能含 <strong>）
      const contentAfterBullet = line.replace(/^[\s]*[•\-\*]\s*/,'')
      listBuffer.push(`<li>${contentAfterBullet}</li>`)
      continue
    }

    const nextLine = lines[lineIndex + 1]?.trim() || ''
    if (
      isStandaloneSubheading(stripped) &&
      nextLine &&
      !/^(#{1,6})\s+/.test(stripped) &&
      !/^(\d+|[一二三四五六七八九十]+)[.、．）)]\s+/.test(stripped)
    ) {
      flushList()
      flushTable()
      flushParagraph()
      parts.push(`<p class="msg-subheading"><strong>${line}</strong></p>`)
      continue
    }

    // Blank line -> paragraph separator
    if (trimmed === '') {
      flushList()
      flushTable()
      flushParagraph()
      continue
    }

    // Normal text line -> accumulate into paragraph
    if (listType) {
      // if we are in a list but hit a non-list line, close list first
      flushList()
    }
    paragraphBuffer.push(line)
  }

  // flush any remaining buffers
  flushList()
  flushTable()
  flushParagraph()

  // join parts without extra <br/> — block elements handle spacing
  let finalHtml = parts.join('')
  // 合并相邻的有序列表，避免 AI 重置序号导致中断（将多个 <ol> 合并为一个）
  try {
    finalHtml = finalHtml.replace(/<\/ol>\s*<ol[^>]*>/g, '')
  } catch (e) {
    // ignore
  }
  // collapse redundant empty paragraphs
  finalHtml = finalHtml.replace(/(<p class="msg-paragraph">\s*<\/p>)+/g, '')
  return finalHtml
}

function formatMessage(content: string): string {
  if (!content) return ''
  return formatAIResponse(content)
}

function scrollToBottom() {
  nextTick(() => {
    if (chatAreaRef.value) {
      chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
    }
  })
}

function delay(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

async function appendStreamingText(messageIndex: number, text: string) {
  if (!text) return
  if (text.length <= 12) {
    messages.value[messageIndex].content += text
    scrollToBottom()
    return
  }

  const chunks = text.match(/[\s\S]{1,8}/g) || [text]
  for (const chunk of chunks) {
    messages.value[messageIndex].content += chunk
    scrollToBottom()
    await delay(12)
  }
}

function analyzeMessage(content: string): { showActions: boolean; actionType: 'booking' | 'application'; itemName: string | null } {
  const applicationMatches = APPLICATION_ACTION_ITEMS.filter(item => content.includes(item))
  const bookingMatches = BOOKING_ACTION_ITEMS.filter(item =>
    item.keywords.some(keyword => content.includes(keyword))
  )
  const applicationItem = applicationMatches[0]
  const bookingItem = bookingMatches[0]
  const applicationCopyCount = countActionCopy(content, ['开始办理', '办理', '申请', '提交申请', '在线填写'])
  const bookingCopyCount = countActionCopy(content, ['立即预约', '预约', '服务预约'])
  const hasApplicationTopic = Boolean(applicationItem)
  const hasBookingTopic = Boolean(bookingItem) && !hasApplicationTopic

  const actionType: 'booking' | 'application' = hasBookingTopic && bookingCopyCount >= applicationCopyCount ? 'booking' : 'application'
  const showActions = actionType === 'booking'
    ? hasBookingTopic && bookingCopyCount > 0
    : hasApplicationTopic && applicationCopyCount > 0
  
  let itemName: string | null = null
  const itemPatterns = [
    /「([^」]+)」/,
    /《([^》]+)》/,
    /【([^】]+)】/,
    /老年补贴申请/,
    /老年补贴/,
    /居住证/,
    /居住证办理/,
    /居住证明开具/,
    /居住证明/,
    /低保/,
    /高龄津贴/,
    /家属代办/
  ]
  
  for (const pattern of itemPatterns) {
    if (pattern instanceof RegExp) {
      const match = content.match(pattern)
      if (match) {
        itemName = match[1] || match[0]
        break
      }
    } else if (content.includes(pattern as string)) {
      itemName = pattern as string
      break
    }
  }
  
  return {
    showActions: showActions && !content.includes('您好！我是AI导办助手'),
    actionType,
    itemName: applicationItem || bookingItem?.keywords[0] || itemName
  }
}

function countActionCopy(content: string, words: string[]): number {
  return words.reduce((total, word) => total + (content.match(new RegExp(word, 'g'))?.length || 0), 0)
}

function normalizeActionCopy(content: string, showActions: boolean, actionType: 'booking' | 'application'): string {
  let normalized = content
  if (!showActions) {
    normalized = normalized
      .replace(/(?:^|\n)[^\n。！？]*点击下方[“"「]?(?:开始办理|立即预约)[”"」]?按钮[^\n。！？]*(?:[。！？]|$)/g, '')
      .replace(/(?:^|\n)[^\n。！？]*点击[“"「]?(?:开始办理|立即预约)[”"」]?按钮[^\n。！？]*(?:[。！？]|$)/g, '')
  } else if (actionType === 'booking') {
    normalized = normalized
      .replace(/点击下方[“"「]?(?:开始办理|立即预约)[”"」]?按钮/g, '点击下方按钮')
      .replace(/点击[“"「]?(?:开始办理|立即预约)[”"」]?按钮/g, '点击下方按钮')
      .replace(/[“"「](?:开始办理|立即预约)[”"」]按钮/g, '下方按钮')
      .replace(/(?:开始办理|立即预约)按钮/g, '下方按钮')
  } else {
    normalized = normalized
      .replace(/点击下方[“"「]?(?:开始办理|立即预约)[”"」]?按钮/g, '点击下方按钮')
      .replace(/点击[“"「]?(?:开始办理|立即预约)[”"」]?按钮/g, '点击下方按钮')
      .replace(/[“"「](?:开始办理|立即预约)[”"」]按钮/g, '下方按钮')
      .replace(/(?:开始办理|立即预约)按钮/g, '下方按钮')
  }
  return normalized
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

async function sendMessage(text?: string) {
  const question = text || inputMessage.value.trim()
  if (!question || isLoading.value) return

  messages.value.push({
    role: 'user',
    content: question,
    time: getCurrentTime()
  })
  inputMessage.value = ''
  scrollToBottom()

  isLoading.value = true
  
  try {
    // 插入一个空白的 AI 消息占位，用于流式渲染
    const assistantIndex = messages.value.push({
      role: 'assistant',
      content: '',
      time: getCurrentTime(),
      showActions: false,
      isWelcome: false
    }) - 1
    currentStreamingIndex.value = assistantIndex
    scrollToBottom()

    const resp = await chatGuideStream({ message: question, sessionId: sessionId.value })
    if (!resp.ok || !resp.body) {
      throw new Error('Chat stream failed')
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let done = false
    let buffer = ''

    while (!done) {
      const { value, done: readerDone } = await reader.read()
      if (value) {
        buffer += decoder.decode(value, { stream: true })

        const parts = buffer.split(/\n\n/)
        buffer = parts.pop() || ''

        for (const part of parts) {
          if (!part) continue
          // 按行处理每个 SSE 事件块，收集所有 data: 行的内容并按原始顺序拼接，保留内部分行
          const lines = part.split(/\n/)
          const dataLines: string[] = []
          for (let line of lines) {
            if (!line) continue
            const idx = line.toLowerCase().indexOf('data:')
            if (idx === 0) {
              dataLines.push(line.slice(5))
            } else if (line.trim().startsWith('data:')) {
              // 处理可能的前导空白
              dataLines.push(line.trim().slice(5))
            } else {
              // 非 data 行：直接当作原始内容的一部分
              dataLines.push(line)
            }
          }

          let text = dataLines.join('\n')
          // 去掉行首可能残留的 data: 前缀（保险）
          text = text.replace(/(^|\n)[dD]ata:\s*/g, '$1')

          if (!text) continue
          if (text.trim() === '[DONE]') {
            done = true
            break
          }

          // 后端规则回复可能一次性返回大段文本，这里拆成小片段平滑渲染。
          await appendStreamingText(assistantIndex, text)
        }
      }
      if (readerDone) break
    }

    isLoading.value = false

    // 流结束，清除 streaming index（隐藏独立打字指示器）
    currentStreamingIndex.value = null

    let aiContent = messages.value[assistantIndex].content || '抱歉，我暂时无法回答这个问题。请稍后再试或拨打12345咨询。'

    // 后处理：在数字或中文序号前补换行，避免列表项连在一行
    try {
      // 在序号前插入换行（如 "...1." 或 "...1、" 变为 "...\n1."）
      aiContent = normalizeGuideText(aiContent.replace(/[-—]{3,}/g, '\n\n'))
    } catch (e) {
      // 忽略任何替换异常，保留原始文本
    }

    let { showActions, actionType, itemName } = analyzeMessage(aiContent)
    aiContent = normalizeActionCopy(aiContent, showActions, actionType)
    ;({ showActions, actionType, itemName } = analyzeMessage(aiContent))

    if (itemName) {
      pendingItemName.value = itemName
    }

    // 更新占位消息属性
    messages.value[assistantIndex].content = aiContent
    messages.value[assistantIndex].showActions = showActions
    messages.value[assistantIndex].actionType = actionType
    messages.value[assistantIndex].pendingItem = itemName || undefined
    messages.value[assistantIndex].time = getCurrentTime()
    scrollToBottom()
    
  } catch (error) {
    console.error('Chat error:', error)
    isLoading.value = false
    currentStreamingIndex.value = null
    messages.value.push({
      role: 'assistant',
      content: '服务暂时不可用，请稍后再试。服务热线：12345',
      time: getCurrentTime(),
      isWelcome: false
    })
    scrollToBottom()
  }
}

function handleStartProcess(itemName?: string) {
  const targetItem = itemName || pendingItemName.value
  if (targetItem) {
    router.push({ path: '/application/submit', query: { itemName: targetItem } })
    ElMessage.success(`正在为您办理「${targetItem}」`)
  } else {
    router.push('/application/submit')
    ElMessage.success('即将跳转到事项办理页面')
  }
}

function goToBooking(itemName?: string) {
  const targetItem = itemName || pendingItemName.value || ''
  const bookingItem = BOOKING_ACTION_ITEMS.find(item => item.keywords.some(keyword => targetItem.includes(keyword)))
  router.push({ path: '/booking', query: bookingItem ? { serviceType: bookingItem.type } : {} })
  ElMessage.success('即将跳转到服务预约页面')
}

watch(messages, saveGuideChatState, { deep: true })
watch([sessionId, pendingItemName, inputMessage], saveGuideChatState)

onMounted(() => {
  restoreGuideChatState()
  scrollToBottom()
})

onBeforeUnmount(() => {
  saveGuideChatState()
})
</script>

<style scoped>
.ai-guide-container {
  width: min(100%, 1180px);
  max-width: calc(100vw - 48px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  background: #f5f7fa;
  border-radius: 16px;
  overflow: hidden;
}

.chat-header {
  padding: 12px 20px;
  background: linear-gradient(135deg, var(--gold) 0%, #b38a2f 100%);
  color: white;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-text h2 {
  font-size: 18px;
  margin: 0 0 2px;
  font-weight: 600;
  color: white;
}

.header-text p {
  font-size: 12px;
  opacity: 0.85;
  margin: 0;
}

.suggest-questions {
  padding: 10px 20px 12px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.suggest-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.suggest-toggle {
  cursor: pointer;
  flex-shrink: 0;
  height: 30px;
  padding: 0 12px;
  border-radius: 15px;
  border: 1px solid #d9dce3;
  background: #fff;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 28px;
  transition: all 0.2s;
}

.suggest-toggle:hover {
  border-color: var(--gold);
  color: var(--gold);
  background: #fffaf0;
}

.suggest-tag {
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  max-width: 100%;
  font-size: 13px !important;
  padding: 0 12px !important;
  height: 30px !important;
  line-height: 28px !important;
  border-radius: 15px !important;
}

.suggest-tag:hover {
  transform: translateY(-1px);
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f5f7fa;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--gold) 0%, var(--gold-light) 100%);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.message-content-wrapper {
  max-width: 84%;
}

.message.user .message-content-wrapper {
  max-width: 76%;
}

.message-content {
  padding: 12px 16px;
  border-radius: 16px;
  background: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  font-family: var(--font-sans);
}

.message.user .message-content {
  background: linear-gradient(135deg, var(--gold) 0%, #b38a2f 100%);
  color: white;
}

.message.assistant .message-content {
  background: white;
  color: #333;
}

.message-text {
  line-height: 1.7;
  font-size: 1rem; /* 使用全局 rem 基准，和全局字体一致 */
  font-family: var(--font-sans);
  white-space: normal;
  word-break: break-word;
}

/* 列表样式 - 关键修复 */
.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 8px 0 8px 20px;
  padding-left: 0;
}

.message-text :deep(li) {
  margin: 4px 0;
  line-height: 1.6;
  font-size: 1rem;
}

/* 确保列表项正确显示 */
.message-text :deep(ul.msg-list),
.message-text :deep(ol.msg-list) {
  display: block;
  list-style-type: disc;
}

.message-text :deep(ol.msg-list) {
  list-style-type: decimal;
}

/* 加粗样式 */
.message-text :deep(strong) {
  font-weight: 600;
  color: inherit;
  font-size: inherit;
  font-family: var(--font-sans);
}

.message-text :deep(.msg-heading) {
  margin: 12px 0 6px;
  font-weight: 700;
  color: var(--text-primary);
  font-size: 1.02rem;
}

.message-text :deep(.msg-subheading) {
  margin: 10px 0 4px;
  color: var(--text-primary);
  font-size: 0.98rem;
}

.message-text :deep(.msg-table-wrap) {
  overflow-x: auto;
  margin: 10px 0;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.message-text :deep(.msg-table) {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
  font-size: 0.94rem;
  white-space: normal;
}

.message-text :deep(.msg-table th),
.message-text :deep(.msg-table td) {
  padding: 9px 10px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  vertical-align: top;
}

.message-text :deep(.msg-table th) {
  background: #f8f6ef;
  color: var(--text-primary);
  font-weight: 600;
}

.message-text :deep(.msg-table tr:last-child td) {
  border-bottom: none;
}

.message.user .message-text :deep(strong) {
  color: #fff;
}

/* 时间 */
.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 8px;
}

/* 按钮区域 */
.message-actions {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}

.msg-type-badge {
  position: absolute;
  left: 16px;
  top: -10px;
}

.msg-type-badge :deep(.el-tag) {
  font-size: 12px;
}

.action-primary-btn {
  background: linear-gradient(135deg, var(--gold) 0%, #b38a2f 100%) !important;
  border: none !important;
  color: white !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  padding: 10px 28px !important;
  height: auto !important;
  border-radius: 30px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 2px 8px rgba(180, 138, 47, 0.28) !important;
}

.action-primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(180, 138, 47, 0.36) !important;
  background: linear-gradient(135deg, #b38a2f 0%, #d4a843 100%) !important;
}

/* 输入区域 */
.chat-input-area {
  padding: 12px 20px;
  background: white;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

:deep(.el-textarea__inner) {
  font-size: 1rem !important;
  font-family: var(--font-sans) !important;
  line-height: 1.5 !important;
  border-radius: 12px !important;
  border-color: #e8e8e8 !important;
}

:deep(.el-textarea__inner):focus {
  border-color: var(--gold) !important;
  box-shadow: 0 0 0 2px rgba(212, 168, 67, 0.12) !important;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-hint {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.send-btn {
  background: linear-gradient(135deg, var(--gold) 0%, #b38a2f 100%) !important;
  border: none !important;
  color: white !important;
  padding: 6px 20px !important;
  font-size: 1rem !important;
  height: 34px !important;
  border-radius: 20px !important;
}

.send-btn:hover {
  background: linear-gradient(135deg, #b38a2f 0%, #d4a843 100%) !important;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 6px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

@media (max-width: 768px) {
  .ai-guide-container {
    width: 100%;
    max-width: 100%;
    height: calc(100vh - 60px);
    border-radius: 0;
  }
  
  .chat-header {
    padding: 10px 16px;
  }
  
  .chat-area {
    padding: 12px 16px;
  }

  .suggest-questions {
    padding: 8px 12px 10px;
  }

  .suggest-tags {
    gap: 6px;
  }

  .suggest-tag,
  .suggest-toggle {
    height: 28px !important;
    line-height: 26px !important;
    font-size: 12px !important;
    padding: 0 10px !important;
  }
  
  .chat-input-area {
    padding: 10px 16px;
  }
  
  .message-content-wrapper,
  .message.user .message-content-wrapper {
    max-width: 85%;
  }
  
  .message-text {
    font-size: 14px;
  }
  
  .action-primary-btn {
    padding: 8px 20px !important;
    font-size: 13px !important;
  }
}
</style>
