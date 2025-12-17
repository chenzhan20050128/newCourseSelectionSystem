<template>
  <div class="course-search">    
    <SearchForm 
      @search="handleSearch" 
      @reset="handleReset" 
      :loading="loading" 
    />

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="loading" class="loading">
      查询中...
    </div>

    <div v-if="results.length > 0" class="results">
      <h3>查询结果 ({{ results.length }} 条)</h3>
      
      <!-- 表头 -->
      <div class="list-header">
        <div class="col col-id">课程号</div>
        <div class="col col-name">课程名</div>
        <div class="col col-credit">学分</div>
        <div class="col col-instructor">教师</div>
        <div class="col col-time">时间</div>
        <div class="col col-weeks">周次</div>
        <div class="col col-location">地点</div>
        <div class="col col-campus">校区</div>
        <div class="col col-college">学院</div>
        <div class="col col-capacity">容量</div>
        <div class="col col-actions">操作</div>
      </div>

      <div class="course-list">
        <CourseCard 
          v-for="course in results" 
          :key="course.courseId" 
          :course="course"
          :student-id="studentIdValue"
          :is-enrolling="enrollingCourses.has(course.courseId)"
          :is-dropping="droppingCourses.has(course.courseId)"
          :message="operationMessage[course.courseId]"
          @enroll="handleEnroll"
          @drop="handleDrop"
        />
      </div>
    </div>

    <div v-if="!loading && results.length === 0 && searched" class="no-results">
      未找到匹配的课程
    </div>
  </div>
</template>

<script>
import { ref, reactive, watch, onMounted, onUnmounted, inject, computed } from 'vue'
import { searchCombinedCourses, enrollCourse, dropCourse } from '../api/courseApi'
import CourseCard from './CourseCard.vue'
import SearchForm from './SearchForm.vue'

export default {
  name: 'CourseSearch',
  components: {
    CourseCard,
    SearchForm
  },
  setup() {
    // 获取学生ID
    const studentId = inject('studentId')

    // 解析 studentId：优先使用 provide/inject，其次从 storage / user 信息兜底
    const resolveStudentId = () => {
      const injected = studentId?.value
      const fromStorage = sessionStorage.getItem('studentId') || localStorage.getItem('studentId')

      let fromUser = null
      try {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          fromUser = JSON.parse(userStr)?.userId
        }
      } catch (e) {
        fromUser = null
      }

      const raw = injected ?? fromStorage ?? fromUser
      if (raw === null || raw === undefined || raw === '') return null
      const num = Number(raw)
      return Number.isFinite(num) ? num : null
    }

    const studentIdValue = computed(() => resolveStudentId())
    
    const results = ref([])
    const loading = ref(false)
    const error = ref('')
    const searched = ref(false)
    
    // 选课/退课相关状态
    const enrollingCourses = ref(new Set()) // 正在选课的课程ID集合
    const droppingCourses = ref(new Set()) // 正在退课的课程ID集合
    const operationMessage = ref({}) // 每个课程的操作消息 { courseId: { type: 'success'|'error'|'warning', message: '' } }

    const handleSearch = async (query) => {
      loading.value = true
      error.value = ''
      searched.value = true
      results.value = []

      try {
        const combinedRequest = {
          courseCondition: null,
          sessionCondition: null
        }

        // 让后端能够标记 isEnrolled：带上 studentId（Long）
        const numericStudentId = studentIdValue.value
        if (numericStudentId !== null) {
          combinedRequest.studentId = numericStudentId
        }

        // 1. 构建 courseCondition
        const courseCondition = {}
        const normalKeys = ['courseId', 'courseName', 'credits', 'description', 'college', 'campus', 'classroom', 'startWeek', 'endWeek']
        
        let hasCourseCondition = false
        normalKeys.forEach(key => {
          if (query[key] !== null && query[key] !== '' && query[key] !== undefined) {
            courseCondition[key] = query[key]
            hasCourseCondition = true
          }
        })
        
        // 如果 query 中有 instructorName 则传递
        if (query.instructorName) {
             courseCondition.instructorName = query.instructorName
             hasCourseCondition = true
        }

        if (hasCourseCondition) {
          combinedRequest.courseCondition = courseCondition
        }

        // 2. 构建 sessionCondition
        const hasTimeQuery = query.weekday || (query.startPeriod !== null && query.startPeriod !== '') || (query.endPeriod !== null && query.endPeriod !== '')
        
        if (hasTimeQuery) {
          combinedRequest.sessionCondition = {
            weekdays: query.weekday ? [query.weekday] : null,
            startPeriod: query.startPeriod ? Number(query.startPeriod) : null,
            endPeriod: query.endPeriod ? Number(query.endPeriod) : null
          }
        }

        const data = await searchCombinedCourses(combinedRequest) || []
        results.value = data
      } catch (err) {
        error.value = err.message || '查询失败'
        console.error('Search error:', err)
      } finally {
        loading.value = false
      }
    }

    const handleReset = () => {
      results.value = []
      error.value = ''
      searched.value = false
      operationMessage.value = {}
    }

    // 默认加载：自动查询一次（无条件时返回全部课程），并携带 studentId
    const autoLoaded = ref(false)
    const tryAutoLoad = async () => {
      if (autoLoaded.value) return
      autoLoaded.value = true
      await handleSearch({})
    }

    onMounted(() => {
      // 直接触发一次默认查询；studentId 通过 resolveStudentId() 从 storage/user 兜底
      tryAutoLoad()
    })

    // 如果页面首次渲染时 user 还没读入，studentId 可能晚一点才有；此处保证一旦出现也会触发默认查询
    watch(
      () => studentId?.value,
      (newVal, oldVal) => {
        if (autoLoaded.value) return
        if (newVal !== null && newVal !== undefined && newVal !== '') {
          tryAutoLoad()
        }
      }
    )

    /**
     * 处理选课
     */
    const handleEnroll = async (course) => {
      const numericStudentId = studentIdValue.value
      if (!numericStudentId) {
        setOperationMessage(course.courseId, 'error', '请先输入学生ID')
        return
      }

      enrollingCourses.value.add(course.courseId)
      clearOperationMessage(course.courseId)

      const batchId = localStorage.getItem('selectedBatchId')
      if (!batchId) {
        setOperationMessage(course.courseId, 'error', '请先选择选课轮次')
        enrollingCourses.value.delete(course.courseId)
        return
      }

      // 检查选课轮次是否在有效时间内
      const selectedBatchStr = localStorage.getItem('selectedBatch')
      if (selectedBatchStr) {
        try {
          const selectedBatch = JSON.parse(selectedBatchStr)
          const now = new Date()
          const startTime = new Date(selectedBatch.startTime)
          const endTime = new Date(selectedBatch.endTime)
          
          if (now < startTime) {
            setOperationMessage(course.courseId, 'error', `选课轮次尚未开始，开始时间：${selectedBatch.startTime}`)
            enrollingCourses.value.delete(course.courseId)
            return
          }
          
          if (now > endTime) {
            setOperationMessage(course.courseId, 'error', `选课轮次已结束，结束时间：${selectedBatch.endTime}`)
            enrollingCourses.value.delete(course.courseId)
            return
          }
        } catch (err) {
          console.error('解析选课轮次信息失败:', err)
        }
      }

      try {
        const response = await enrollCourse({
          studentId: numericStudentId,
          courseId: course.courseId,
          batchId: Number(batchId)
        })

        if (response.success) {
          setOperationMessage(course.courseId, 'success', response.message)
          if (response.warn) {
            setTimeout(() => {
              setOperationMessage(course.courseId, 'warning', response.warn)
            }, 2000)
          }
          course.enrolledCount = (course.enrolledCount || 0) + 1
          course.isEnrolled = true
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
      const numericStudentId = studentIdValue.value
      if (!numericStudentId) {
        setOperationMessage(course.courseId, 'error', '请先输入学生ID')
        return
      }

      droppingCourses.value.add(course.courseId)
      clearOperationMessage(course.courseId)

      try {
        const response = await dropCourse({
          studentId: numericStudentId,
          courseId: course.courseId
        })

        if (response.success) {
          setOperationMessage(course.courseId, 'success', response.message)
          course.enrolledCount = Math.max((course.enrolledCount || 0) - 1, 0)
          course.isEnrolled = false
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

    const setOperationMessage = (courseId, type, message) => {
      operationMessage.value[courseId] = { type, message }
      setTimeout(() => {
        clearOperationMessage(courseId)
      }, 5000)
    }

    const clearOperationMessage = (courseId) => {
      if (operationMessage.value[courseId]) {
        delete operationMessage.value[courseId]
      }
    }

    return {
      studentId,
      studentIdValue,
      results,
      loading,
      error,
      searched,
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
.course-search {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  color: #333;
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

.list-header {
  display: flex;
  align-items: center;
  background: #7C1F89; /* 深紫色 */
  padding: 12px 5px;
  gap: 8px;
  font-size: 13px;
  font-weight: bold;
  color: white; /* 文字白色 */
  border-radius: 4px 4px 0 0;
  border-bottom: none;
}

.col {
  padding: 0 4px;
  white-space: nowrap;
}

/* Column Widths - Must match CourseCard.vue */
.col-id { flex: 0 0 80px; }
.col-name { flex: 1 1 140px; }
.col-credit { flex: 0 0 40px; text-align: left; padding-left: 8px; }
.col-instructor { flex: 0 0 150px; }
.col-time { flex: 1 1 100px; }
.col-weeks { flex: 0 0 60px; text-align: center; }
.col-location { flex: 0 0 180px; }
.col-campus { flex: 0 0 70px; }
.col-college { flex: 0 0 150px; }
.col-capacity { flex: 0 0 70px; text-align: center; }
.col-actions { flex: 0 0 100px; text-align: center; }

.course-list {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8e8e8;
  border-top: none;
  border-radius: 0 0 4px 4px;
  background: white;
}

/* 斑马纹背景 */
.course-list :deep(.course-card-row) {
  background-color: white;
}

.course-list :deep(.course-card-row:nth-child(even)) {
  background-color: #fcf5ff; /* 浅紫色 */
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>

