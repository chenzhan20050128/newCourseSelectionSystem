<template>
  <div class="course-search">
    <h2>课程条件查询</h2>
    
    <form @submit.prevent="handleSearch" class="search-form">
      <div class="form-row">
        <div class="form-group">
          <label>课程ID:</label>
          <input type="number" v-model.number="query.courseId" />
        </div>
        <div class="form-group">
          <label>课程名称:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.courseName === 'input'"
              type="text" 
              v-model="query.courseName"
              placeholder="输入课程名称"
            />
            <select 
              v-else
              v-model="query.courseName"
              :disabled="loadingOptions.courseName"
            >
              <option value="">请选择</option>
              <option v-for="option in options.courseName" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('courseName')"
              :title="fieldModes.courseName === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.courseName === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>学分:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.credits === 'input'"
              type="number" 
              v-model.number="query.credits"
              placeholder="输入学分"
            />
            <select 
              v-else
              v-model.number="query.credits"
              :disabled="loadingOptions.credits"
            >
              <option :value="null">请选择</option>
              <option v-for="option in options.credits" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('credits')"
              :title="fieldModes.credits === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.credits === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>学院:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.college === 'input'"
              type="text" 
              v-model="query.college"
              placeholder="输入学院"
            />
            <select 
              v-else
              v-model="query.college"
              :disabled="loadingOptions.college"
            >
              <option value="">请选择</option>
              <option v-for="option in options.college" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('college')"
              :title="fieldModes.college === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.college === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>教师ID:</label>
          <input type="number" v-model.number="query.instructorId" />
        </div>
        <div class="form-group">
          <label>校区:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.campus === 'input'"
              type="text" 
              v-model="query.campus"
              placeholder="输入校区"
            />
            <select 
              v-else
              v-model="query.campus"
              :disabled="loadingOptions.campus"
            >
              <option value="">请选择</option>
              <option v-for="option in options.campus" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('campus')"
              :title="fieldModes.campus === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.campus === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>教室:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.classroom === 'input'"
              type="text" 
              v-model="query.classroom"
              placeholder="输入教室"
            />
            <select 
              v-else
              v-model="query.classroom"
              :disabled="loadingOptions.classroom"
            >
              <option value="">请选择</option>
              <option v-for="option in options.classroom" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('classroom')"
              :title="fieldModes.classroom === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.classroom === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>开始周:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.startWeek === 'input'"
              type="number" 
              v-model.number="query.startWeek"
              placeholder="输入开始周"
            />
            <select 
              v-else
              v-model.number="query.startWeek"
              :disabled="loadingOptions.startWeek"
            >
              <option :value="null">请选择</option>
              <option v-for="option in options.startWeek" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('startWeek')"
              :title="fieldModes.startWeek === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.startWeek === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label>结束周:</label>
          <div class="input-with-mode">
            <input 
              v-if="fieldModes.endWeek === 'input'"
              type="number" 
              v-model.number="query.endWeek"
              placeholder="输入结束周"
            />
            <select 
              v-else
              v-model.number="query.endWeek"
              :disabled="loadingOptions.endWeek"
            >
              <option :value="null">请选择</option>
              <option v-for="option in options.endWeek" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <button 
              type="button" 
              class="mode-toggle"
              @click="toggleFieldMode('endWeek')"
              :title="fieldModes.endWeek === 'input' ? '切换到下拉框' : '切换到输入框'"
            >
              {{ fieldModes.endWeek === 'input' ? '📋' : '✏️' }}
            </button>
          </div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group full-width">
          <label>描述:</label>
          <input type="text" v-model="query.description" placeholder="输入描述" />
        </div>
      </div>

      <div class="form-actions">
        <button type="submit" :disabled="loading">查询</button>
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
import { ref, reactive, watch, onMounted, onUnmounted, inject } from 'vue'
import { searchCourses, getAttributeValues, enrollCourse, dropCourse } from '../api/courseApi'

// 支持下拉框的字段列表
const DROPDOWN_FIELDS = ['courseName', 'credits', 'college', 'campus', 'classroom', 'startWeek', 'endWeek']

export default {
  name: 'CourseSearch',
  setup() {
    // 获取学生ID
    const studentId = inject('studentId')
    
    const query = reactive({
      courseId: null,
      courseName: '',
      credits: null,
      description: '',
      college: '',
      instructorId: null,
      campus: '',
      classroom: '',
      startWeek: null,
      endWeek: null
    })

    // 字段模式：'input' 或 'dropdown'
    const fieldModes = reactive({
      courseName: 'input',
      credits: 'dropdown',
      college: 'dropdown',
      campus: 'dropdown',
      classroom: 'dropdown',
      startWeek: 'input',
      endWeek: 'input'
    })

    // 下拉框选项数据
    const options = reactive({
      courseName: [],
      credits: [],
      college: [],
      campus: [],
      classroom: [],
      startWeek: [],
      endWeek: []
    })

    // 加载选项的状态
    const loadingOptions = reactive({
      courseName: false,
      credits: false,
      college: false,
      campus: false,
      classroom: false,
      startWeek: false,
      endWeek: false
    })

    const results = ref([])
    const loading = ref(false)
    const error = ref('')
    const searched = ref(false)
    
    // 选课/退课相关状态
    const enrollingCourses = ref(new Set()) // 正在选课的课程ID集合
    const droppingCourses = ref(new Set()) // 正在退课的课程ID集合
    const operationMessage = ref({}) // 每个课程的操作消息 { courseId: { type: 'success'|'error'|'warning', message: '' } }

    // 防抖定时器
    const debounceTimers = {}

    /**
     * 切换字段的输入模式
     */
    const toggleFieldMode = (fieldName) => {
      if (fieldModes[fieldName] === 'input') {
        fieldModes[fieldName] = 'dropdown'
        // 切换到下拉框时，加载选项
        loadFieldOptions(fieldName)
      } else {
        fieldModes[fieldName] = 'input'
      }
    }

    /**
     * 构建查询条件（排除指定字段）
     */
    const buildCondition = (excludeField = null) => {
      const condition = {}
      Object.keys(query).forEach(key => {
        if (key !== excludeField) {
          const value = query[key]
          if (value !== null && value !== '' && value !== undefined) {
            condition[key] = value
          }
        }
      })
      return condition
    }

    /**
     * 加载指定字段的下拉框选项
     */
    const loadFieldOptions = async (fieldName) => {
      if (!DROPDOWN_FIELDS.includes(fieldName)) {
        return
      }

      // 如果已经在加载中，取消之前的请求
      if (debounceTimers[fieldName]) {
        clearTimeout(debounceTimers[fieldName])
      }

      // 防抖处理
      debounceTimers[fieldName] = setTimeout(async () => {
        loadingOptions[fieldName] = true
        try {
          const condition = buildCondition(fieldName)
          const response = await getAttributeValues({
            condition: condition,
            attributeName: fieldName
          })
          options[fieldName] = response || []
        } catch (err) {
          console.error(`加载 ${fieldName} 选项失败:`, err)
          options[fieldName] = []
        } finally {
          loadingOptions[fieldName] = false
        }
      }, 300)
    }

    /**
     * 初始化时加载所有下拉框字段的选项
     */
    const initializeOptions = async () => {
      const promises = DROPDOWN_FIELDS
        .filter(field => fieldModes[field] === 'dropdown')
        .map(field => loadFieldOptions(field))
      await Promise.all(promises)
    }

    // 监听查询条件变化，自动更新相关字段的下拉框选项
    watch(
      () => [
        query.courseId,
        query.courseName,
        query.credits,
        query.college,
        query.instructorId,
        query.campus,
        query.classroom,
        query.startWeek,
        query.endWeek
      ],
      () => {
        // 当条件变化时，更新所有下拉框模式的字段选项
        DROPDOWN_FIELDS.forEach(field => {
          if (fieldModes[field] === 'dropdown') {
            loadFieldOptions(field)
          }
        })
      },
      { deep: true }
    )

    const handleSearch = async () => {
      loading.value = true
      error.value = ''
      searched.value = true
      results.value = []

      // 构建请求对象，只包含有值的字段
      const request = buildCondition()

      try {
        const data = await searchCourses(request)
        results.value = data || []
      } catch (err) {
        error.value = err.message || '查询失败'
        console.error('Search error:', err)
      } finally {
        loading.value = false
      }
    }

    const handleReset = () => {
      Object.keys(query).forEach(key => {
        if (typeof query[key] === 'number') {
          query[key] = null
        } else {
          query[key] = ''
        }
      })
      results.value = []
      error.value = ''
      searched.value = false
      operationMessage.value = {}
      // 重置后重新加载下拉框选项
      initializeOptions()
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

      try {
        const response = await enrollCourse({
          studentId: studentId.value,
          courseId: course.courseId
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

    // 组件挂载时初始化选项
    onMounted(() => {
      initializeOptions()
    })

    // 组件卸载时清理定时器
    onUnmounted(() => {
      Object.values(debounceTimers).forEach(timer => {
        if (timer) clearTimeout(timer)
      })
    })

    return {
      studentId,
      query,
      fieldModes,
      options,
      loadingOptions,
      results,
      loading,
      error,
      searched,
      enrollingCourses,
      droppingCourses,
      operationMessage,
      toggleFieldMode,
      handleSearch,
      handleReset,
      handleEnroll,
      handleDrop
    }
  }
}
</script>

<style scoped>
.course-search {
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
}

.form-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  flex: 1 1 100%;
}

.form-group label {
  margin-bottom: 5px;
  font-weight: bold;
  color: #555;
}

.form-group input,
.form-group select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #1890ff;
}

.form-group select:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.input-with-mode {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-mode input,
.input-with-mode select {
  flex: 1;
  padding-right: 35px;
}

.mode-toggle {
  position: absolute;
  right: 5px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 16px;
  color: #666;
  transition: color 0.3s;
  z-index: 1;
}

.mode-toggle:hover {
  color: #1890ff;
}

.mode-toggle:active {
  transform: scale(0.95);
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

