<template>
  <div class="smart-course-chat-container">
    <div class="chat-main">
      <!-- Header -->
      <div class="chat-header">
        <div class="header-inner">
          <div class="header-right">
            <el-button size="small" class="action-btn outline-btn header-btn" @click="newChat">
              <el-icon><Plus /></el-icon>
              新对话
            </el-button>
          </div>
        </div>
      </div>

      <!-- Messages Area -->
      <el-scrollbar ref="scrollbarRef" class="chat-messages" wrap-class="scrollbar-wrapper">
        <div class="messages-inner">
          <!-- Welcome Screen -->
          <div v-if="messages.length === 0" class="welcome-container">
            <div class="welcome-card">
              <div class="welcome-icon">🎓</div>
              <h3>你好，我是智能选课助手</h3>
              <p>我可以根据你的兴趣、时间安排和学分需求为你推荐课程。</p>
              
              <div class="suggestion-grid">
                <div 
                  v-for="(prompt, index) in quickPrompts" 
                  :key="index" 
                  class="suggestion-card"
                  @click="useQuickPrompt(prompt)"
                >
                  <p>{{ prompt }}</p>
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <!-- Message List -->
          <div v-else class="message-list">
            <div v-for="(msg, index) in messages" :key="index" :class="['message-row', msg.role]">
              <div class="avatar-col">
                <div class="avatar" :class="msg.role">
                  <el-icon v-if="msg.role === 'user'"><User /></el-icon>
                  <span v-else>AI</span>
                </div>
              </div>
              <div class="content-col">
                <div class="message-bubble" :class="msg.role">
                  <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                  <div v-else class="user-text">{{ msg.content }}</div>
                </div>
                
                <!-- Action Buttons for Assistant Messages -->
                <div v-if="msg.role === 'assistant' && (!isStreaming || index !== messages.length - 1)" class="message-actions">
                  <el-button size="small" class="action-btn copy-btn" @click="copyContent(msg.content)">
                    <el-icon><CopyDocument /></el-icon> 复制
                  </el-button>
                  <el-button size="small" class="action-btn outline-btn" @click="exportToMarkdown(msg.content)">
                    <el-icon><Document /></el-icon> Markdown
                  </el-button>
                  <el-button size="small" class="action-btn outline-btn" @click="exportToWord(msg.content)">
                    <el-icon><Document /></el-icon> Word
                  </el-button>
                  <el-button size="small" class="action-btn outline-btn" @click="exportToPDF(msg.content)">
                    <el-icon><Document /></el-icon> PDF
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>

      <!-- Input Area -->
      <div class="chat-input-area">
        <div class="input-box">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入你的选课需求..."
            resize="none"
            @keydown.enter.prevent="handleEnter"
            :disabled="isStreaming"
            class="custom-textarea"
          />
          <div class="input-actions">
            <el-button 
              v-if="!isStreaming"
              type="primary" 
              circle 
              :disabled="!inputMessage.trim()" 
              @click="sendMessage"
              class="send-btn"
            >
              <el-icon><Position /></el-icon>
            </el-button>
            <el-button 
              v-else
              type="danger" 
              circle 
              @click="stopStreaming"
              class="stop-btn"
            >
              <el-icon><VideoPause /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="input-footer">
          <span>按 Enter 发送，Shift + Enter 换行</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, nextTick, onUnmounted, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Position, ArrowRight, CopyDocument, Document, VideoPause, Plus } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { getCourseRecommendationStream } from '../api/courseApi'

const studentId = inject('studentId')
const messages = ref([])
const inputMessage = ref('')
const isStreaming = ref(false)
const scrollbarRef = ref(null)
let cancelStream = null

const quickPrompts = [
  '推荐3门适合我的计算机课程',
  '我希望课程集中在周一到周三',
  '我想要难度适中、学分3-4的课',
  '有哪些关于人工智能的通识课？'
]

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

const renderMarkdown = (text) => {
  return md.render(text || '')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollbarRef.value) {
      const wrap = scrollbarRef.value.wrapRef
      if (wrap) {
        wrap.scrollTop = wrap.scrollHeight
      }
    }
  })
}

const getStorageKey = () => {
  const id = studentId?.value ? String(studentId.value) : 'anonymous'
  return `smart-course-chat:${id}`
}

const persistHistory = () => {
  try {
    localStorage.setItem(getStorageKey(), JSON.stringify(messages.value))
  } catch {
    // ignore
  }
}

const loadHistory = () => {
  try {
    const raw = localStorage.getItem(getStorageKey())
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      messages.value = parsed
      scrollToBottom()
    }
  } catch {
    // ignore
  }
}

const useQuickPrompt = (text) => {
  inputMessage.value = text
}

const handleEnter = (e) => {
  if (e.shiftKey) return
  sendMessage()
}

// Export Functions
const copyContent = async (text) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const exportToMarkdown = (text) => {
  if (!text) return
  const blob = new Blob([text], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'course_recommendation.md'
  a.click()
  URL.revokeObjectURL(url)
}

const exportToWord = (text) => {
  if (!text) return
  // Simple HTML export which Word can open
  let htmlContent = `
    <html>
      <head>
        <meta charset="utf-8">
        <title>选课推荐</title>
        <style>
          body { font-family: Arial, sans-serif; }
        </style>
      </head>
      <body>
        ${renderMarkdown(text)}
      </body>
    </html>
  `
  
  const blob = new Blob([htmlContent], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'course_recommendation.doc'
  a.click()
  URL.revokeObjectURL(url)
}

const getHtml2Pdf = async () => {
  const mod = await import('html2pdf.js')
  return mod?.default ?? mod
}

const exportToPDF = async (text) => {
  if (!text) return

  const html2pdf = await getHtml2Pdf()
  const container = document.createElement('div')
  container.style.position = 'fixed'
  // Avoid extremely large negative coordinates (can produce blank output in html2canvas)
  container.style.left = '0'
  container.style.top = '0'
  container.style.transform = 'translateX(-200vw)'
  container.style.width = '794px' // A4 width @ 96dpi approx
  container.style.padding = '24px'
  container.style.background = '#fff'
  container.style.color = '#111'
  container.style.fontFamily = 'Arial, sans-serif'
  const rendered = renderMarkdown(text)
  container.innerHTML = rendered
  // If markdown renders to empty HTML (or unexpected), fallback to raw text
  if (!container.textContent || container.textContent.trim().length === 0) {
    container.innerHTML = ''
    const pre = document.createElement('pre')
    pre.style.whiteSpace = 'pre-wrap'
    pre.style.wordBreak = 'break-word'
    pre.style.fontFamily = 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace'
    pre.style.fontSize = '12px'
    pre.textContent = text
    container.appendChild(pre)
  }
  document.body.appendChild(container)

  try {
    await html2pdf()
      .set({
        margin: 10,
        filename: 'course_recommendation.pdf',
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { scale: 2, useCORS: true },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
      })
      .from(container)
      .save()
  } catch (err) {
    console.error(err)
    ElMessage.error('PDF 导出失败')
  } finally {
    document.body.removeChild(container)
  }
}

const newChat = () => {
  stopStreaming()
  messages.value = []
  persistHistory()
}

const stopStreaming = () => {
  if (cancelStream) {
    cancelStream()
    cancelStream = null
  }
  isStreaming.value = false
}

const sendMessage = () => {
  const content = inputMessage.value.trim()
  if (!content || isStreaming.value) return
  
  if (!studentId.value) {
    ElMessage.warning('请先设置学生ID')
    return
  }

  // Add user message
  messages.value.push({
    role: 'user',
    content: content
  })
  inputMessage.value = ''
  scrollToBottom()

  // Add placeholder for assistant message
  const assistantMsgIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: ''
  })

  isStreaming.value = true
  
  const request = {
    studentId: studentId.value,
    prompt: content
  }

  cancelStream = getCourseRecommendationStream(
    request,
    (chunk) => {
      // Append chunk to current message
      messages.value[assistantMsgIndex].content += chunk
      scrollToBottom()
    },
    (err) => {
      console.error(err)
      ElMessage.error('获取推荐失败: ' + err.message)
      messages.value[assistantMsgIndex].content += '\n\n[发生错误，请重试]'
      isStreaming.value = false
      cancelStream = null
      scrollToBottom()
    },
    () => {
      isStreaming.value = false
      cancelStream = null
      scrollToBottom()
    }
  )
}

onUnmounted(() => {
  if (cancelStream) cancelStream()
})

onMounted(() => {
  loadHistory()
})

watch(
  () => studentId?.value,
  () => {
    loadHistory()
  }
)

watch(
  messages,
  () => {
    persistHistory()
  },
  { deep: true }
)
</script>

<style scoped>
.smart-course-chat-container {
  height: 80vh;
  min-height: 600px;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.chat-header {
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  z-index: 10;
  flex-shrink: 0;
}

.header-inner {
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-btn {
  height: 32px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.subtitle {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #999;
}

.chat-messages {
  flex: 1 1 auto;
  min-height: 0;
  background-color: #f9fafb;
  overflow: hidden;
}

.chat-messages :deep(.el-scrollbar__wrap) {
  overflow-y: auto;
}

.messages-inner {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.welcome-container {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.welcome-card {
  text-align: center;
  max-width: 600px;
  width: 100%;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-card h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}

.welcome-card p {
  color: #666;
  margin-bottom: 32px;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.suggestion-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
  text-align: left;
}

.suggestion-card:hover {
  border-color: #7C1F89;
  background-color: #f3e5f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.suggestion-card p {
  margin: 0;
  font-size: 14px;
  color: #333;
  flex: 1;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message-row {
  display: flex;
  gap: 16px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.avatar.user {
  background-color: #f3e5f5;
  color: #7C1F89;
}

.avatar.assistant {
  background-color: #7C1F89;
  color: #fff;
}

.content-col {
  max-width: 80%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  position: relative;
}

.message-bubble.user {
  background-color: #7C1F89;
  color: #fff;
  border-top-right-radius: 4px;
}

.message-bubble.assistant {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  color: #333;
  border-top-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.user-text {
  white-space: pre-wrap;
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.action-btn {
  border-radius: 8px;
  font-size: 12px;
}

.copy-btn {
  background-color: #7C1F89;
  border-color: #7C1F89;
  color: #fff;
}
.copy-btn:hover {
  background-color: #9c27b0;
  border-color: #9c27b0;
  color: #fff;
}

.outline-btn {
  background-color: #fff;
  border-color: #7C1F89;
  color: #7C1F89;
}
.outline-btn:hover {
  background-color: #f3e5f5;
  border-color: #7C1F89;
  color: #7C1F89;
}

/* Markdown Styles */
.markdown-body :deep(p) {
  margin: 0 0 10px 0;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 10px 0;
}
.markdown-body :deep(code) {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 2px 4px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.9em;
}
.markdown-body :deep(pre) {
  background-color: #f6f8fa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 10px 0;
}
.markdown-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
}
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
}
.markdown-body :deep(a) {
  color: #7C1F89;
  text-decoration: none;
}

.chat-input-area {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.input-box {
  position: relative;
  max-width: 900px;
  margin: 0 auto;
  border: 1px solid #dcdfe6;
  border-radius: 24px;
  padding: 6px 6px 6px 16px;
  background: #fff;
  display: flex;
  align-items: flex-end;
  transition: border-color 0.2s;
}

.input-box:focus-within {
  border-color: #7C1F89;
  box-shadow: 0 0 0 2px #f3e5f5;
}

.custom-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 8px 0;
  background: transparent;
  max-height: 150px;
}

.input-actions {
  padding-bottom: 4px;
  padding-right: 4px;
}

.send-btn {
  background-color: #7C1F89;
  border-color: #7C1F89;
}
.send-btn:hover {
  background-color: #9c27b0;
  border-color: #9c27b0;
}
.send-btn.is-disabled {
  background-color: #e1bee7;
  border-color: #e1bee7;
}

.input-footer {
  max-width: 900px;
  margin: 8px auto 0;
  text-align: center;
  font-size: 12px;
  color: #999;
}
</style>

