<template>
  <div class="smart-course-selection">
    <div class="header">
      <h2>智能选课</h2>
      <p class="subtitle">AI 智能推荐适合您的课程</p>
    </div>

    <div v-if="!studentId" class="no-student-id">
      <p>请先在上方输入学生ID</p>
    </div>

    <div v-else class="content-wrapper">
      <!-- 输入区域 -->
      <div class="input-section">
        <div class="input-group">
          <label for="prompt">请描述您的选课需求：</label>
          <textarea
            id="prompt"
            v-model="prompt"
            placeholder="例如：我想选一些计算机相关的课程，希望是周一到周三的课，学分在3-4分之间..."
            rows="4"
            :disabled="isStreaming"
          ></textarea>
        </div>
        <div class="button-group">
          <button
            class="btn-submit"
            @click="startRecommendation"
            :disabled="!prompt.trim() || isStreaming"
          >
            {{ isStreaming ? '推荐中...' : '开始推荐' }}
          </button>
          <button
            v-if="isStreaming"
            class="btn-cancel"
            @click="cancelRecommendation"
          >
            取消
          </button>
          <button
            v-if="recommendationText && !isStreaming"
            class="btn-clear"
            @click="clearRecommendation"
          >
            清空
          </button>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="error-message">
        <strong>错误：</strong>{{ error }}
      </div>

      <!-- 推荐结果区域 -->
      <div v-if="recommendationText || isStreaming" class="result-section">
        <div class="result-header">
          <h3>AI 推荐结果</h3>
          <div v-if="isStreaming" class="streaming-indicator">
            <span class="dot"></span>
            <span>正在生成推荐...</span>
          </div>
        </div>
        <div class="result-content" ref="resultContent">
          <div class="markdown-content" v-html="formattedText"></div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!recommendationText && !isStreaming && !error" class="empty-state">
        <div class="empty-icon">🤖</div>
        <p>请输入您的选课需求，AI 将为您智能推荐合适的课程</p>
        <div class="example-prompts">
          <p class="example-title">示例：</p>
          <div class="example-item" @click="prompt = '我想选一些计算机相关的课程'">
            "我想选一些计算机相关的课程"
          </div>
          <div class="example-item" @click="prompt = '希望是周一到周三的课，学分在3-4分之间'">
            "希望是周一到周三的课，学分在3-4分之间"
          </div>
          <div class="example-item" @click="prompt = '推荐一些适合初学者的课程，不要太难'">
            "推荐一些适合初学者的课程，不要太难"
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, inject, onUnmounted } from 'vue'
import { getCourseRecommendationStream } from '../api/courseApi'

export default {
  name: 'SmartCourseSelection',
  setup() {
    const studentId = inject('studentId')
    const prompt = ref('')
    const recommendationText = ref('')
    const isStreaming = ref(false)
    const error = ref('')
    const resultContent = ref(null)
    let cancelFunction = null

    /**
     * 格式化文本为 HTML（简单的 Markdown 支持）
     */
    const formattedText = computed(() => {
      if (!recommendationText.value) return ''
      
      let text = recommendationText.value
      
      // 转义 HTML
      text = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      
      // 标题
      text = text.replace(/^### (.*$)/gim, '<h3>$1</h3>')
      text = text.replace(/^## (.*$)/gim, '<h2>$1</h2>')
      text = text.replace(/^# (.*$)/gim, '<h1>$1</h1>')
      
      // 粗体
      text = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      
      // 列表
      text = text.replace(/^\* (.*$)/gim, '<li>$1</li>')
      text = text.replace(/^- (.*$)/gim, '<li>$1</li>')
      text = text.replace(/^(\d+)\. (.*$)/gim, '<li>$2</li>')
      
      // 换行
      text = text.replace(/\n/g, '<br>')
      
      // 包装列表项
      text = text.replace(/(<li>.*<\/li>)/g, '<ul>$1</ul>')
      
      return text
    })

    /**
     * 开始推荐
     */
    const startRecommendation = () => {
      if (!studentId.value || !prompt.value.trim()) {
        error.value = '请输入选课需求'
        return
      }

      error.value = ''
      recommendationText.value = ''
      isStreaming.value = true

      const request = {
        studentId: studentId.value,
        prompt: prompt.value.trim()
      }

      cancelFunction = getCourseRecommendationStream(
        request,
        // onMessage
        (content) => {
          recommendationText.value += content
          // 自动滚动到底部
          if (resultContent.value) {
            setTimeout(() => {
              resultContent.value.scrollTop = resultContent.value.scrollHeight
            }, 0)
          }
        },
        // onError
        (err) => {
          error.value = err.message || '获取推荐失败，请稍后重试'
          isStreaming.value = false
          cancelFunction = null
        },
        // onComplete
        () => {
          isStreaming.value = false
          cancelFunction = null
        }
      )
    }

    /**
     * 取消推荐
     */
    const cancelRecommendation = () => {
      if (cancelFunction) {
        cancelFunction()
        cancelFunction = null
      }
      isStreaming.value = false
    }

    /**
     * 清空推荐结果
     */
    const clearRecommendation = () => {
      recommendationText.value = ''
      error.value = ''
    }

    // 组件卸载时取消请求
    onUnmounted(() => {
      if (cancelFunction) {
        cancelFunction()
      }
    })

    return {
      studentId,
      prompt,
      recommendationText,
      isStreaming,
      error,
      resultContent,
      formattedText,
      startRecommendation,
      cancelRecommendation,
      clearRecommendation
    }
  }
}
</script>

<style scoped>
.smart-course-selection {
  padding: 20px;
}

.header {
  margin-bottom: 30px;
}

.header h2 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 24px;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.no-student-id {
  padding: 40px;
  text-align: center;
  color: #999;
  background: #f9f9f9;
  border-radius: 8px;
}

.content-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.input-section {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.input-group {
  margin-bottom: 15px;
}

.input-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #333;
}

.input-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: border-color 0.3s;
}

.input-group textarea:focus {
  outline: none;
  border-color: #1890ff;
}

.input-group textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.button-group {
  display: flex;
  gap: 10px;
}

.btn-submit,
.btn-cancel,
.btn-clear {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-submit {
  background: #1890ff;
  color: white;
}

.btn-submit:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-submit:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.btn-cancel {
  background: #ff4d4f;
  color: white;
}

.btn-cancel:hover {
  background: #ff7875;
}

.btn-clear {
  background: #f0f0f0;
  color: #333;
}

.btn-clear:hover {
  background: #d9d9d9;
}

.error-message {
  padding: 12px 16px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #cf1322;
  margin-bottom: 20px;
}

.result-section {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.result-header {
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1890ff;
  font-size: 14px;
}

.dot {
  width: 8px;
  height: 8px;
  background: #1890ff;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.result-content {
  padding: 20px;
  max-height: 600px;
  overflow-y: auto;
  line-height: 1.8;
  color: #333;
}

.markdown-content {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  margin: 16px 0 8px 0;
  color: #333;
}

.markdown-content h1 {
  font-size: 24px;
}

.markdown-content h2 {
  font-size: 20px;
}

.markdown-content h3 {
  font-size: 18px;
}

.markdown-content ul {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-content li {
  margin: 4px 0;
}

.markdown-content strong {
  font-weight: bold;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 30px;
}

.example-prompts {
  max-width: 600px;
  margin: 0 auto;
  text-align: left;
}

.example-title {
  font-weight: bold;
  color: #666;
  margin-bottom: 12px;
}

.example-item {
  padding: 12px 16px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: #333;
}

.example-item:hover {
  border-color: #1890ff;
  background: #f0f7ff;
  transform: translateX(4px);
}

/* 滚动条样式 */
.result-content::-webkit-scrollbar {
  width: 8px;
}

.result-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.result-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.result-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

