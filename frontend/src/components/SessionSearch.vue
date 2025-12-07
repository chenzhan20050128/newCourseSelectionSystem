<template>
  <div class="session-search">
    <h2>节次条件查询</h2>
    
    <form @submit.prevent="handleSearch" class="search-form">
      <div class="form-group">
        <label>周几: <span class="required">*</span></label>
        <select v-model="query.weekday" required>
          <option value="">请选择</option>
          <option value="周一">周一</option>
          <option value="周二">周二</option>
          <option value="周三">周三</option>
          <option value="周四">周四</option>
          <option value="周五">周五</option>
          <option value="周六">周六</option>
          <option value="周日">周日</option>
        </select>
      </div>

      <div class="form-group">
        <label>开始节次: <span class="required">*</span></label>
        <input 
          type="number" 
          v-model.number="query.startPeriod" 
          min="1" 
          max="12" 
          required 
        />
        <small>范围: 1-12</small>
      </div>

      <div class="form-group">
        <label>结束节次: <span class="required">*</span></label>
        <input 
          type="number" 
          v-model.number="query.endPeriod" 
          min="1" 
          max="12" 
          required 
        />
        <small>范围: 1-12</small>
      </div>

      <div class="form-actions">
        <button type="submit" :disabled="loading || !isFormValid">查询</button>
        <button type="button" @click="handleReset">重置</button>
      </div>
    </form>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="loading" class="loading">
      查询中...
    </div>

    <div v-if="results.length > 0" class="results">
      <h3>查询结果 ({{ results.length }} 条)</h3>
      <div v-for="course in results" :key="course.courseId" class="course-card">
        <div class="course-header">
          <span class="course-id">ID: {{ course.courseId }}</span>
          <span class="course-name">{{ course.courseName }}</span>
          <span class="credits">{{ course.credits }} 学分</span>
        </div>
        <div class="course-info">
          <p><strong>学院:</strong> {{ course.college }}</p>
          <p><strong>校区:</strong> {{ course.campus }}</p>
          <p><strong>教室:</strong> {{ course.classroom }}</p>
          <p><strong>教师ID:</strong> {{ course.instructorId }}</p>
          <p><strong>周次:</strong> 第{{ course.startWeek }}周 - 第{{ course.endWeek }}周</p>
          <p v-if="course.description"><strong>描述:</strong> {{ course.description }}</p>
        </div>
        <div v-if="course.sessions && course.sessions.length > 0" class="sessions">
          <strong>上课时间:</strong>
          <ul>
            <li v-for="session in course.sessions" :key="session.sessionId">
              {{ session.weekday }} 第{{ session.startPeriod }}-{{ session.endPeriod }}节
            </li>
          </ul>
        </div>
        <div v-if="operationMessage[course.courseId]" 
             :class="['operation-message', `message-${operationMessage[course.courseId].type}`]">
          {{ operationMessage[course.courseId].message }}
        </div>
        <div class="course-actions">
          <p class="capacity-info">
            <strong>容量:</strong> {{ course.enrolledCount || 0 }}/{{ course.capacity }}
          </p>
          <div class="action-buttons">
            <button 
              class="btn-enroll"
              @click="handleEnroll(course)"
              :disabled="!studentId || enrollingCourses.has(course.courseId)"
            >
              {{ enrollingCourses.has(course.courseId) ? '选课中...' : '选课' }}
            </button>
            <button 
              class="btn-drop"
              @click="handleDrop(course)"
              :disabled="!studentId || droppingCourses.has(course.courseId)"
            >
              {{ droppingCourses.has(course.courseId) ? '退课中...' : '退课' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && results.length === 0 && searched" class="no-results">
      未找到匹配的课程
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, inject } from 'vue'
import { searchCoursesBySession, enrollCourse, dropCourse } from '../api/courseApi'

export default {
  name: 'SessionSearch',
  setup() {
    // 获取学生ID
    const studentId = inject('studentId')
    
    const query = reactive({
      weekday: '',
      startPeriod: null,
      endPeriod: null
    })

    const results = ref([])
    const loading = ref(false)
    const error = ref('')
    const searched = ref(false)
    
    // 选课/退课相关状态
    const enrollingCourses = ref(new Set()) // 正在选课的课程ID集合
    const droppingCourses = ref(new Set()) // 正在退课的课程ID集合
    const operationMessage = ref({}) // 每个课程的操作消息 { courseId: { type: 'success'|'error'|'warning', message: '' } }

    const isFormValid = computed(() => {
      return query.weekday && 
             query.startPeriod !== null && 
             query.startPeriod >= 1 && 
             query.startPeriod <= 12 &&
             query.endPeriod !== null && 
             query.endPeriod >= 1 && 
             query.endPeriod <= 12
    })

    const handleSearch = async () => {
      if (!isFormValid.value) {
        error.value = '请填写完整的查询条件'
        return
      }

      loading.value = true
      error.value = ''
      searched.value = true
      results.value = []

      try {
        const data = await searchCoursesBySession({
          weekday: query.weekday,
          startPeriod: query.startPeriod,
          endPeriod: query.endPeriod
        })
        results.value = data || []
      } catch (err) {
        error.value = err.message || '查询失败'
        console.error('Search error:', err)
      } finally {
        loading.value = false
      }
    }

    const handleReset = () => {
      query.weekday = ''
      query.startPeriod = null
      query.endPeriod = null
      results.value = []
      error.value = ''
      searched.value = false
      operationMessage.value = {}
    }

    /**
     * 处理选课
     */
    const handleEnroll = async (course) => {
      if (!studentId.value) {
        setOperationMessage(course.courseId, 'error', '请先输入学生ID')
        return
      }

      enrollingCourses.value.add(course.courseId)
      clearOperationMessage(course.courseId)

      // 获取当前选中的选课轮次ID
      const batchId = localStorage.getItem('selectedBatchId')
      if (!batchId) {
        setOperationMessage(course.courseId, 'error', '请先选择选课轮次')
        enrollingCourses.value.delete(course.courseId)
        return
      }

      try {
        const response = await enrollCourse({
          studentId: studentId.value,
          courseId: course.courseId,
          batchId: Number(batchId)
        })

        if (response.success) {
          setOperationMessage(course.courseId, 'success', response.message)
          if (response.warn) {
            // 如果有警告信息，也显示出来
            setTimeout(() => {
              setOperationMessage(course.courseId, 'warning', response.warn)
            }, 2000)
          }
          // 更新课程的已选人数
          course.enrolledCount = (course.enrolledCount || 0) + 1
        } else {
          setOperationMessage(course.courseId, 'error', response.message)
        }
      } catch (err) {
        setOperationMessage(course.courseId, 'error', err.message || '选课失败')
        console.error('Enroll error:', err)
      } finally {
        enrollingCourses.value.delete(course.courseId)
      }
    }

    /**
     * 处理退课
     */
    const handleDrop = async (course) => {
      if (!studentId.value) {
        setOperationMessage(course.courseId, 'error', '请先输入学生ID')
        return
      }

      droppingCourses.value.add(course.courseId)
      clearOperationMessage(course.courseId)

      try {
        const response = await dropCourse({
          studentId: studentId.value,
          courseId: course.courseId
        })

        if (response.success) {
          setOperationMessage(course.courseId, 'success', response.message)
          // 更新课程的已选人数
          course.enrolledCount = Math.max((course.enrolledCount || 0) - 1, 0)
        } else {
          setOperationMessage(course.courseId, 'error', response.message)
        }
      } catch (err) {
        setOperationMessage(course.courseId, 'error', err.message || '退课失败')
        console.error('Drop error:', err)
      } finally {
        droppingCourses.value.delete(course.courseId)
      }
    }

    /**
     * 设置操作消息
     */
    const setOperationMessage = (courseId, type, message) => {
      operationMessage.value[courseId] = { type, message }
      // 5秒后自动清除消息
      setTimeout(() => {
        clearOperationMessage(courseId)
      }, 5000)
    }

    /**
     * 清除操作消息
     */
    const clearOperationMessage = (courseId) => {
      if (operationMessage.value[courseId]) {
        delete operationMessage.value[courseId]
      }
    }

    return {
      studentId,
      query,
      results,
      loading,
      error,
      searched,
      isFormValid,
      enrollingCourses,
      droppingCourses,
      operationMessage,
      handleSearch,
      handleReset,
      handleEnroll,
      handleDrop
    }
  }
}
</script>

<style scoped>
.session-search {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

.search-form {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
  max-width: 500px;
}

.form-group {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 5px;
  font-weight: bold;
  color: #555;
}

.required {
  color: #ff4d4f;
}

.form-group select,
.form-group input {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group select:focus,
.form-group input:focus {
  outline: none;
  border-color: #1890ff;
}

.form-group small {
  margin-top: 5px;
  color: #999;
  font-size: 12px;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.form-actions button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.form-actions button[type="submit"] {
  background: #1890ff;
  color: white;
}

.form-actions button[type="submit"]:hover:not(:disabled) {
  background: #40a9ff;
}

.form-actions button[type="submit"]:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.form-actions button[type="button"] {
  background: #f0f0f0;
  color: #333;
}

.form-actions button[type="button"]:hover {
  background: #e0e0e0;
}

.error-message {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.results {
  margin-top: 20px;
}

.results h3 {
  margin-bottom: 15px;
  color: #333;
}

.course-card {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 15px;
  background: #fafafa;
}

.course-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.course-id {
  color: #999;
  font-size: 12px;
}

.course-name {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.credits {
  color: #1890ff;
  font-weight: bold;
}

.course-info {
  margin-bottom: 10px;
}

.course-info p {
  margin: 5px 0;
  color: #666;
}

.sessions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.sessions ul {
  margin-top: 5px;
  padding-left: 20px;
}

.sessions li {
  margin: 3px 0;
  color: #666;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #999;
}

.course-actions {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.capacity-info {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.btn-enroll,
.btn-drop {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-enroll {
  background: #52c41a;
  color: white;
}

.btn-enroll:hover:not(:disabled) {
  background: #73d13d;
}

.btn-enroll:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-drop {
  background: #ff4d4f;
  color: white;
}

.btn-drop:hover:not(:disabled) {
  background: #ff7875;
}

.btn-drop:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.operation-message {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  animation: fadeIn 0.3s;
}

.message-success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

.message-error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
}

.message-warning {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  color: #faad14;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

