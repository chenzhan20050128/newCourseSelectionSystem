<template>
  <div class="smart-course-chat-container">
    <div class="chat-main">
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
                  <div v-if="msg.role === 'assistant'">
                    <div v-if="msg.isThinking" class="thinking-row">
                      <span class="thinking-spinner"></span>
                      <span>AI正在思考中…</span>
                    </div>
                    <div v-else class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
                  </div>
                  <div v-else class="user-text">{{ msg.content }}</div>
                </div>

                <!-- Recommended Courses List -->
                <div v-if="msg.recommendedCourses && msg.recommendedCourses.length > 0" class="recommended-courses-list">
                  <div class="recommendation-title">推荐课程详情：</div>
                  <div class="recommendation-table-header">
                    <div class="col col-info sortable" @click="toggleSort('courseId')">
                      课程号
                      <span class="sort-arrows" :class="{ active: sortKey === 'courseId' }">
                        <span class="arrow up" :class="arrowClass('courseId', 'asc')">▲</span>
                        <span class="arrow down" :class="arrowClass('courseId', 'desc')">▼</span>
                      </span>
                    </div>
                    <div class="col col-instructor sortable" @click="toggleSort('instructorName')">
                      教师
                      <span class="sort-arrows" :class="{ active: sortKey === 'instructorName' }">
                        <span class="arrow up" :class="arrowClass('instructorName', 'asc')">▲</span>
                        <span class="arrow down" :class="arrowClass('instructorName', 'desc')">▼</span>
                      </span>
                    </div>
                    <div class="col col-schedule sortable" @click="toggleSort('time')">
                      时间 / 地点
                      <span class="sort-arrows" :class="{ active: sortKey === 'time' }">
                        <span class="arrow up" :class="arrowClass('time', 'asc')">▲</span>
                        <span class="arrow down" :class="arrowClass('time', 'desc')">▼</span>
                      </span>
                    </div>
                    <div class="col col-capacity sortable" @click="toggleSort('utilization')">
                      选课人数 / 容量
                      <span class="sort-arrows" :class="{ active: sortKey === 'utilization' }">
                        <span class="arrow up" :class="arrowClass('utilization', 'asc')">▲</span>
                        <span class="arrow down" :class="arrowClass('utilization', 'desc')">▼</span>
                      </span>
                    </div>
                    <div class="col col-actions">操作</div>
                  </div>
                  <div class="course-card-wrapper" v-for="course in getSortedRecommendedCourses(msg.recommendedCourses)" :key="course.courseId">
                    <CourseCard 
                      :course="course"
                      :studentId="studentId"
                      :isEnrolled="course.isEnrolled"
                      :isEnrolling="enrollingCourses.has(course.courseId)"
                      :isDropping="droppingCourses.has(course.courseId)"
                      :message="operationMessage[course.courseId]"
                      :enrolledCourses="enrolledCourses"
                      @enroll="handleEnroll"
                      @drop="handleDrop"
                    />
                  </div>
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
import { User, Position, ArrowRight, CopyDocument, Document, VideoPause } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { getCourseRecommendationStream, getCoursesByIds, enrollCourse, dropCourse, getStudentCourses } from '../api/courseApi'
import CourseCard from './CourseCard.vue'
import { sortCourses } from '../utils/courseSorter'

const studentId = inject('studentId')
const refreshAfterCourseChange = inject('refreshAfterCourseChange', null)
const messages = ref([])
const inputMessage = ref('')
const isStreaming = ref(false)
const scrollbarRef = ref(null)
const enrollingCourses = ref(new Set())
const droppingCourses = ref(new Set())
const operationMessage = ref({})
const enrolledCourses = ref([]) // 存储已选课程列表，用于冲突检测

const sortKey = ref('')
const sortOrder = ref('asc')

const toggleSort = (key) => {
  // none -> asc -> desc -> none
  if (sortKey.value !== key) {
    sortKey.value = key
    sortOrder.value = 'asc'
    return
  }
  if (sortOrder.value === 'asc') {
    sortOrder.value = 'desc'
    return
  }
  // desc -> none
  sortKey.value = ''
  sortOrder.value = 'asc'
}

const arrowClass = (key, dir) => {
  if (sortKey.value !== key) return 'inactive'
  if (sortOrder.value !== dir) return 'inactive'
  return 'active'
}

const getSortedRecommendedCourses = (courses) => {
  if (!sortKey.value) return courses || []
  return sortCourses(courses || [], { key: sortKey.value, order: sortOrder.value })
}

const fetchEnrolledCourses = async () => {
  if (!studentId.value) return
  try {
    const courses = await getStudentCourses(studentId.value)
    enrolledCourses.value = courses || []
  } catch (e) {
    console.error('Failed to fetch enrolled courses', e)
  }
}

const setOperationMessage = (courseId, type, message) => {
  operationMessage.value[courseId] = { type, message }
  setTimeout(() => clearOperationMessage(courseId), 5000)
}
const clearOperationMessage = (courseId) => { if (operationMessage.value[courseId]) delete operationMessage.value[courseId] }

const handleEnroll = async (course) => {
  if (!studentId.value) { ElMessage.error('请先输入学生ID'); return }
  enrollingCourses.value.add(course.courseId)
  clearOperationMessage(course.courseId)
  const batchId = localStorage.getItem('selectedBatchId')
  if (!batchId) {
    ElMessage.error('请先选择选课轮次')
    enrollingCourses.value.delete(course.courseId)
    return
  }
  try {
    const response = await enrollCourse({ studentId: studentId.value, courseId: course.courseId, batchId: Number(batchId) })
    if (response.success) {
      setOperationMessage(course.courseId, 'success', response.message)
      ElMessage.success(response.message || '选课成功')
      if (response.warn) setTimeout(() => setOperationMessage(course.courseId, 'warning', response.warn), 2000)
      course.enrolledCount = (course.enrolledCount || 0) + 1
      course.isEnrolled = true
      fetchEnrolledCourses() // 刷新已选课程列表
      if (typeof refreshAfterCourseChange === 'function') {
        refreshAfterCourseChange()
      }
    } else {
      setOperationMessage(course.courseId, 'error', response.message)
    }
  } catch (err) {
    setOperationMessage(course.courseId, 'error', err.message || '选课失败')
  } finally { enrollingCourses.value.delete(course.courseId) }
}

const handleDrop = async (course) => {
  if (!studentId.value) { ElMessage.error('请先输入学生ID'); return }
  droppingCourses.value.add(course.courseId)
  clearOperationMessage(course.courseId)
  try {
    const response = await dropCourse({ studentId: studentId.value, courseId: course.courseId })
    if (response.success) {
      setOperationMessage(course.courseId, 'success', response.message)
      ElMessage.success(response.message || '退课成功')
      course.enrolledCount = Math.max((course.enrolledCount || 0) - 1, 0)
      course.isEnrolled = false
      fetchEnrolledCourses() // 刷新已选课程列表
      if (typeof refreshAfterCourseChange === 'function') {
        refreshAfterCourseChange()
      }
    } else {
      setOperationMessage(course.courseId, 'error', response.message)
    }
  } catch (err) {
    setOperationMessage(course.courseId, 'error', err.message || '退课失败')
  } finally {
    droppingCourses.value.delete(course.courseId)
  }
}

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

const exportToPDF = async (text) => {
  if (!text) return

  // 关键点：html2canvas 对“被 transform 推到极远处/不可渲染”的元素容易截图为空白。
  // 这里把导出容器挂到 DOM 上，但用 opacity:0 隐藏，确保可渲染。
  const container = document.createElement('div')
  container.setAttribute('data-pdf-export', 'true')
  container.style.position = 'fixed'
  // 不用 transform 推到很远处（可能导致 html2canvas 空白），改为温和的负 left 移出视口
  container.style.left = '-10000px'
  container.style.top = '0'
  container.style.width = '794px' // A4 width @ 96dpi approx
  container.style.padding = '24px'
  container.style.background = '#fff'
  container.style.color = '#111'
  container.style.fontFamily = 'Arial, sans-serif'
  container.style.lineHeight = '1.6'
  container.style.zIndex = '0'
  container.style.pointerEvents = 'none'
  // 关键：opacity 必须是 1，否则 html2canvas 会按透明度渲染，最终 PDF 看起来就是空白。
  container.style.opacity = '1'
  container.style.visibility = 'visible'
  container.style.transform = 'none'

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

  // 调试输出：把中间生成的 HTML 打到控制台（避免一次性打印超大字符串）
  try {
    const html = container.innerHTML || ''
    const plain = (container.textContent || '').trim()
    window.__smartCoursePdfExport = {
      html,
      text: plain,
      createdAt: new Date().toISOString(),
      htmlLength: html.length,
      textLength: plain.length
    }

    console.groupCollapsed('[SmartCourseSelection] PDF export debug')
    console.log('htmlLength:', html.length)
    console.log('textLength:', plain.length)
    console.log('htmlPreview(0..800):', html.slice(0, 800))
    console.log('textPreview(0..200):', plain.slice(0, 200))
    console.log('window.__smartCoursePdfExport 里有完整 HTML')
    console.groupEnd()
  } catch {
    // ignore
  }

  // 等待 2 帧，让布局/字体渲染更稳定（减少空白概率）
  await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

  // 等待字体加载（若浏览器支持），避免字体未就绪导致渲染异常
  try {
    if (document.fonts && typeof document.fonts.ready?.then === 'function') {
      await document.fonts.ready
    }
  } catch {
    // ignore
  }

  try {
    // 直接走 html2canvas + jsPDF：你现在的预览截图是正常的，说明空白发生在 html2pdf->jsPDF 的封装阶段。
    // 这里我们绕开 html2pdf，手动把 canvas 写入 PDF（支持自动分页）。
    const canvas = await html2canvas(container, {
      scale: 1.2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      windowWidth: container.scrollWidth,
      windowHeight: container.scrollHeight
    })

    const imgData = canvas.toDataURL('image/jpeg', 0.98)

    // 调试：把预览图也存起来，便于你确认“写入 PDF 之前”图片是正常的
    try {
      window.__smartCoursePdfExport = {
        ...(window.__smartCoursePdfExport || {}),
        previewDataUrl: imgData,
        previewCanvas: { width: canvas.width, height: canvas.height }
      }
    } catch {
      // ignore
    }

    const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' })
    const pageWidth = pdf.internal.pageSize.getWidth()
    const pageHeight = pdf.internal.pageSize.getHeight()
    const margin = 10
    const printableWidth = pageWidth - margin * 2
    const printableHeight = pageHeight - margin * 2

    const imgWidth = printableWidth
    const imgHeight = (canvas.height * imgWidth) / canvas.width

    let heightLeft = imgHeight
    let position = margin

    pdf.addImage(imgData, 'JPEG', margin, position, imgWidth, imgHeight)
    heightLeft -= printableHeight

    while (heightLeft > 0) {
      pdf.addPage()
      position = margin - (imgHeight - heightLeft)
      pdf.addImage(imgData, 'JPEG', margin, position, imgWidth, imgHeight)
      heightLeft -= printableHeight
    }

    pdf.save('course_recommendation.pdf')
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
    content: '',
    isThinking: true,
    recommendedCourses: []
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
      if (messages.value[assistantMsgIndex]?.isThinking) {
        messages.value[assistantMsgIndex].isThinking = false
      }
      messages.value[assistantMsgIndex].content += chunk
      scrollToBottom()
    },
    (err) => {
      console.error(err)
      ElMessage.error('获取推荐失败: ' + err.message)
      if (messages.value[assistantMsgIndex]) {
        messages.value[assistantMsgIndex].isThinking = false
      }
      messages.value[assistantMsgIndex].content += '\n\n[发生错误，请重试]'
      isStreaming.value = false
      cancelStream = null
      scrollToBottom()
    },
    async () => {
      isStreaming.value = false
      cancelStream = null

      if (messages.value[assistantMsgIndex]) {
        messages.value[assistantMsgIndex].isThinking = false
      }
      
      // 提取课程ID并查询
      const text = messages.value[assistantMsgIndex].content
      // 匹配所有连续数字，且长度在4到10位之间（假设课程ID是这样的）
      const matches = text.match(/\b\d{4,10}\b/g)
      
      if (matches && matches.length > 0) {
        const uniqueIds = [...new Set(matches.map(id => Number(id)))]
        try {
           const courses = await getCoursesByIds({ courseIds: uniqueIds, studentId: studentId.value })
           if (courses && courses.length > 0) {
             messages.value[assistantMsgIndex].recommendedCourses = courses
             scrollToBottom()
           }
        } catch (e) {
          console.error('Failed to fetch recommended courses details', e)
        }
      }
      
      scrollToBottom()
    }
  )
}

onUnmounted(() => {
  if (cancelStream) cancelStream()
})

onMounted(() => {
  loadHistory()
  fetchEnrolledCourses()
})

watch(
  () => studentId?.value,
  () => {
    loadHistory()
    fetchEnrolledCourses()
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
  height: 100%;
  min-height: 0;
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

/* 仅扩大 AI 回复气泡右侧宽度：让其可延伸到与用户气泡同一右边界 */
.message-row.assistant .content-col {
  flex: 1;
  max-width: 100%;
}

.message-row.assistant .message-bubble.assistant {
  width: 100%;
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

.thinking-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666;
  font-size: 14px;
}

.thinking-spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #e5e7eb;
  border-top-color: #7C1F89;
  animation: thinking-spin 0.9s linear infinite;
  flex-shrink: 0;
}

@keyframes thinking-spin {
  to {
    transform: rotate(360deg);
  }
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

.recommended-courses-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.recommendation-table-header {
  display: flex;
  background: #FAF4FC;
  padding: 10px 12px;
  color: #6a5acd;
  font-weight: 600;
  font-size: 13px;
  border: 1px solid #efe5f5;
  border-radius: 8px;
}

.recommendation-table-header .col {
  padding: 0 12px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
}

.recommendation-table-header .col-info      { flex: 2.5; min-width: 200px; }
.recommendation-table-header .col-instructor{ flex: 1;   min-width: 100px; }
.recommendation-table-header .col-schedule  { flex: 1.8; min-width: 180px; }
.recommendation-table-header .col-capacity  { flex: 1.2; min-width: 120px; }
.recommendation-table-header .col-actions   { flex: 0 0 100px; display: flex; justify-content: center; position: relative; }

.sortable {
  cursor: pointer;
  user-select: none;
}

.sort-arrows {
  margin-left: 4px;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 16px;
  vertical-align: middle;
  cursor: pointer;
  gap: 2px;
}

.sort-arrows .arrow {
  font-size: 14px;
  height: 12px;
  line-height: 12px;
}

.sort-arrows .arrow.inactive {
  color: #bfbfbf;
}

.sort-arrows .arrow.active {
  color: #7C1F89;
  font-weight: 800;
}

.recommendation-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  font-weight: 600;
}

.course-card-wrapper {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}
</style>

