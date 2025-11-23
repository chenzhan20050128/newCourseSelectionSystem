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
      </div>
    </div>

    <div v-if="!loading && results.length === 0 && searched" class="no-results">
      未找到匹配的课程
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { searchCoursesBySession } from '../api/courseApi'

export default {
  name: 'SessionSearch',
  setup() {
    const query = reactive({
      weekday: '',
      startPeriod: null,
      endPeriod: null
    })

    const results = ref([])
    const loading = ref(false)
    const error = ref('')
    const searched = ref(false)

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
    }

    return {
      query,
      results,
      loading,
      error,
      searched,
      isFormValid,
      handleSearch,
      handleReset
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
</style>

