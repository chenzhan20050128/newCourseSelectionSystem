<template>
  <div class="my-courses">
    <div class="header">
      <h2>我的课程</h2>
      <button 
        class="btn-refresh" 
        @click="loadCourses"
        :disabled="loading || !studentId"
      >
        {{ loading ? '加载中...' : '刷新' }}
      </button>
    </div>

    <div v-if="!studentId" class="no-student-id">
      <p>请先在上方输入学生ID</p>
    </div>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="loading && courses.length === 0" class="loading">
      加载中...
    </div>

    <!-- 课程表 -->
    <div v-if="courses.length > 0" class="schedule-container">
      <div class="summary">
        <p>共选课 <strong>{{ courses.length }}</strong> 门，总学分 <strong>{{ totalCredits }}</strong> 分</p>
      </div>

      <div class="schedule-wrapper">
        <div class="schedule-grid">
          <!-- 表头：周几 -->
          <div class="grid-header time-header"></div>
          <div 
            v-for="(day, index) in weekdays" 
            :key="index" 
            class="grid-header day-header"
            :class="{ 'today': isToday(index) }"
          >
            <div class="day-name">{{ day }}</div>
          </div>

          <!-- 课程表主体 -->
          <template v-for="period in periods" :key="period.period">
            <!-- 左侧节次和时间 -->
            <div class="period-header">
              <div class="period-number">第{{ period.period }}节</div>
              <div class="period-time">{{ period.time }}</div>
            </div>

            <!-- 每天的单元格 -->
            <div 
              v-for="(day, dayIndex) in weekdays" 
              :key="`${period.period}-${dayIndex}`"
              class="schedule-cell"
              :class="{ 'empty-cell': getCellBlocks(day, period.period).length === 0 }"
              :data-day="day"
              :data-period="period.period"
              @click="handleCellClick(day, period.period)"
            >
              <!-- 该单元格的所有课程块 -->
              <div 
                v-for="(block, blockIndex) in getCellBlocks(day, period.period)"
                :key="`block-${block.course.courseId}-${block.session.sessionId}-${blockIndex}`"
                class="course-block"
                :class="{
                  'has-conflict': getCellConflictCount(block, day, period.period) > 1
                }"
                :style="getBlockStyle(block)"
                @click.stop="showCourseDetail(block.course)"
              >
                <div class="course-name-text">{{ block.course.courseName }}</div>
                <div class="course-location">{{ block.course.classroom }}</div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div v-if="!loading && courses.length === 0 && studentId && !error" class="no-results">
      您还没有选课
    </div>

    <!-- 课程详情对话框 -->
    <div v-if="selectedCourse" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content" @click.stop>
        <button class="dialog-close" @click="closeDialog">×</button>
        <div class="dialog-header">
          <h3>{{ selectedCourse.courseName }}</h3>
        </div>
        <div class="dialog-body">
          <div class="detail-item">
            <strong>课程ID:</strong> {{ selectedCourse.courseId }}
          </div>
          <div class="detail-item">
            <strong>学分:</strong> {{ selectedCourse.credits }}
          </div>
          <div class="detail-item">
            <strong>学院:</strong> {{ selectedCourse.college }}
          </div>
          <div class="detail-item">
            <strong>校区:</strong> {{ selectedCourse.campus }}
          </div>
          <div class="detail-item">
            <strong>教室:</strong> {{ selectedCourse.classroom }}
          </div>
          <div class="detail-item">
            <strong>教师ID:</strong> {{ selectedCourse.instructorId }}
          </div>
          <div class="detail-item">
            <strong>周次:</strong> 第{{ selectedCourse.startWeek }}周 - 第{{ selectedCourse.endWeek }}周
          </div>
          <div v-if="selectedCourse.description" class="detail-item">
            <strong>描述:</strong> {{ selectedCourse.description }}
          </div>
          <div class="detail-item">
            <strong>容量:</strong> {{ selectedCourse.enrolledCount || 0 }}/{{ selectedCourse.capacity }}
          </div>
          <div v-if="selectedCourse.sessions && selectedCourse.sessions.length > 0" class="detail-item">
            <strong>上课时间:</strong>
            <ul class="sessions-list">
              <li v-for="session in selectedCourse.sessions" :key="session.sessionId">
                {{ session.weekday }} 第{{ session.startPeriod }}-{{ session.endPeriod }}节
              </li>
            </ul>
          </div>
        </div>
        <div class="dialog-footer">
          <button 
            class="btn-drop-dialog"
            @click="handleDrop(selectedCourse)"
            :disabled="!studentId || droppingCourses.has(selectedCourse.courseId)"
          >
            {{ droppingCourses.has(selectedCourse.courseId) ? '退课中...' : '退课' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 可选课程列表对话框 -->
    <div v-if="showAvailableCourses" class="dialog-overlay" @click="closeAvailableCoursesDialog">
      <div class="dialog-content available-courses-dialog" @click.stop>
        <button class="dialog-close" @click="closeAvailableCoursesDialog">×</button>
        <div class="dialog-header">
          <h3>{{ queryTimeInfo }}</h3>
        </div>
        <div class="dialog-body">
          <div v-if="loadingAvailableCourses" class="loading">
            查询中...
          </div>
          <div v-else-if="availableCoursesError" class="error-message">
            {{ availableCoursesError }}
          </div>
          <div v-else-if="availableCourses.length === 0" class="no-results">
            该时间段没有可选课程
          </div>
          <div v-else class="available-courses-list">
            <div v-for="course in availableCourses" :key="course.courseId" class="course-card">
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
                <button 
                  class="btn-enroll"
                  @click="handleEnrollFromDialog(course)"
                  :disabled="!studentId || enrollingCourses.has(course.courseId)"
                >
                  {{ enrollingCourses.has(course.courseId) ? '选课中...' : '选课' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, inject, watch, onMounted } from 'vue'
import { getStudentCourses, dropCourse, searchCoursesBySession, enrollCourse } from '../api/courseApi'

export default {
  name: 'MyCourses',
  setup() {
    // 获取学生ID
    const studentId = inject('studentId')
    
    const courses = ref([])
    const loading = ref(false)
    const error = ref('')
    const selectedCourse = ref(null)
    
    // 退课相关状态
    const droppingCourses = ref(new Set())
    const operationMessage = ref({})

    // 可选课程相关状态
    const showAvailableCourses = ref(false)
    const availableCourses = ref([])
    const loadingAvailableCourses = ref(false)
    const availableCoursesError = ref('')
    const queryTimeInfo = ref('')
    const queryWeekday = ref('')
    const queryPeriod = ref(null)
    const enrollingCourses = ref(new Set())

    // 周几数组
    const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

    // 节次和时间映射
    const periods = [
      { period: 1, time: '08:00-08:50' },
      { period: 2, time: '09:00-09:50' },
      { period: 3, time: '10:10-11:00' },
      { period: 4, time: '11:10-12:00' },
      { period: 5, time: '14:00-14:50' },
      { period: 6, time: '15:00-15:50' },
      { period: 7, time: '16:10-17:00' },
      { period: 8, time: '17:10-18:00' },
      { period: 9, time: '18:30-19:20' },
      { period: 10, time: '19:30-20:20' },
      { period: 11, time: '20:30-21:20' },
      { period: 12, time: '21:30-22:20' }
    ]

    // 颜色池
    const colorPalette = [
      { bg: '#FFE5E5', border: '#FFB3B3', text: '#333' },
      { bg: '#E5F5E5', border: '#B3E5B3', text: '#333' },
      { bg: '#E5F0FF', border: '#B3D9FF', text: '#333' },
      { bg: '#FFF9E5', border: '#FFE5B3', text: '#333' },
      { bg: '#F0E5FF', border: '#D9B3FF', text: '#333' },
      { bg: '#FFE8D5', border: '#FFCCB3', text: '#333' },
      { bg: '#E5F5F5', border: '#B3E5E5', text: '#333' },
      { bg: '#F5F5F5', border: '#D9D9D9', text: '#333' }
    ]

    /**
     * 为课程分配颜色
     */
    const getCourseColor = (courseId) => {
      const index = (courseId - 1) % colorPalette.length
      return colorPalette[index]
    }

    /**
     * 检查两个时间段是否重叠
     */
    const isTimeOverlap = (start1, end1, start2, end2) => {
      return !(end1 < start2 || end2 < start1)
    }

    /**
     * 检测时间冲突并生成课程块数据
     */
    const generateCourseBlocks = computed(() => {
      const cellMap = new Map() // 存储每个单元格的课程块列表
      const sessionBlocks = [] // 存储所有session块（用于跨行显示）

      // 第一步：遍历所有课程，为每个session创建块
      courses.value.forEach(course => {
        if (!course.sessions || course.sessions.length === 0) return

        course.sessions.forEach(session => {
          const weekday = session.weekday
          const startPeriod = session.startPeriod
          const endPeriod = session.endPeriod

          // 创建一个session块（用于跨行显示）
          const sessionBlock = {
            course,
            session,
            weekday,
            startPeriod,
            endPeriod,
            span: endPeriod - startPeriod + 1
          }
          sessionBlocks.push(sessionBlock)

          // 为这个session覆盖的每个单元格添加引用
          for (let period = startPeriod; period <= endPeriod; period++) {
            const cellKey = `${weekday}-${period}`
            if (!cellMap.has(cellKey)) {
              cellMap.set(cellKey, [])
            }
            cellMap.get(cellKey).push(sessionBlock)
          }
        })
      })

      // 第二步：为每个单元格计算冲突信息
      cellMap.forEach((blocksInCell, cellKey) => {
        blocksInCell.forEach((block, index) => {
          // 为每个块添加在该单元格的冲突信息
          if (!block.cellConflicts) {
            block.cellConflicts = new Map()
          }
          block.cellConflicts.set(cellKey, {
            conflictCount: blocksInCell.length,
            conflictIndex: index
          })
        })
      })

      return sessionBlocks
    })

    /**
     * 获取指定单元格的所有课程块
     * 只返回在该单元格第一行显示的块（跨多节的课程只在第一行显示）
     */
    const getCellBlocks = (weekday, period) => {
      return generateCourseBlocks.value.filter(block => {
        // 只显示在该单元格第一行的块（即startPeriod等于当前period的块）
        return block.weekday === weekday && block.startPeriod === period
      })
    }

    /**
     * 获取课程块的样式
     * 对于跨多节的课程，使用绝对定位和计算高度来跨越多行
     */
    const getBlockStyle = (block) => {
      const color = getCourseColor(block.course.courseId)
      const cellKey = `${block.weekday}-${block.startPeriod}`

      // 使用第一行的冲突信息来确定位置
      const firstCellConflict = block.cellConflicts?.get(cellKey)
      const conflictCount = firstCellConflict?.conflictCount || 1
      const conflictIndex = firstCellConflict?.conflictIndex || 0

      // 计算跨多节课程的总高度
      // 每个单元格高度约80px（包括边框），跨N节需要跨越N个单元格
      const cellHeight = 80 // 基础单元格高度
      const borderWidth = 1 // 每个单元格的边框宽度
      const padding = 2 * 2 // 上下padding
      const totalHeight = block.span * cellHeight + (block.span - 1) * borderWidth * 2 - padding

      let style = {
        backgroundColor: color.bg,
        borderColor: color.border,
        color: color.text,
        position: 'absolute',
        top: '2px',
        zIndex: conflictCount > 1 ? 10 : 5
      }

      // 跨多节的课程，设置高度和bottom
      if (block.span > 1) {
        style.height = `calc(${block.span} * 100% + ${(block.span - 1) * 2}px)`
        style.display = 'flex'
        style.flexDirection = 'column'
        style.justifyContent = 'center'
      }

      // 处理冲突：均分单元格
      if (conflictCount > 1) {
        const widthPercent = 100 / conflictCount
        const leftPercent = conflictIndex * widthPercent
        style.width = `${widthPercent}%`
        style.left = `${leftPercent}%`
      } else {
        style.width = '100%'
        style.left = '0'
      }

      return style
    }

    /**
     * 获取单元格的冲突数量
     */
    const getCellConflictCount = (block, weekday, period) => {
      const cellKey = `${weekday}-${period}`
      return block.cellConflicts?.get(cellKey)?.conflictCount || 1
    }

    /**
     * 判断是否是今天（简化版，实际应该根据当前日期计算）
     */
    const isToday = (dayIndex) => {
      // 这里简化处理，实际应该根据当前日期判断
      return false
    }

    /**
     * 显示课程详情
     */
    const showCourseDetail = (course) => {
      selectedCourse.value = course
    }

    /**
     * 关闭对话框
     */
    const closeDialog = () => {
      selectedCourse.value = null
    }

    /**
     * 处理单元格点击事件
     * 如果单元格为空，查询该时间段的所有可选课程
     */
    const handleCellClick = async (weekday, period) => {
      // 检查该单元格是否有课程
      const cellBlocks = getCellBlocks(weekday, period)
      if (cellBlocks.length > 0) {
        // 如果有课程，不处理（课程块有自己的点击事件）
        return
      }

      // 如果单元格为空，查询该时间段的所有课程
      queryWeekday.value = weekday
      queryPeriod.value = period
      queryTimeInfo.value = `${weekday} 第${period}节`
      showAvailableCourses.value = true
      availableCourses.value = []
      availableCoursesError.value = ''
      loadingAvailableCourses.value = true

      try {
        const data = await searchCoursesBySession({
          weekday: weekday,
          startPeriod: period,
          endPeriod: period
        })
        availableCourses.value = data || []
      } catch (err) {
        availableCoursesError.value = err.message || '查询失败'
        console.error('Search available courses error:', err)
      } finally {
        loadingAvailableCourses.value = false
      }
    }

    /**
     * 关闭可选课程对话框
     */
    const closeAvailableCoursesDialog = () => {
      showAvailableCourses.value = false
      availableCourses.value = []
      availableCoursesError.value = ''
      queryTimeInfo.value = ''
      queryWeekday.value = ''
      queryPeriod.value = null
      // 清除操作消息
      operationMessage.value = {}
    }

    /**
     * 从对话框选课
     */
    const handleEnrollFromDialog = async (course) => {
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
          // 延迟刷新课程表
          setTimeout(() => {
            loadCourses()
          }, 1000)
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

    /**
     * 加载学生课程
     */
    const loadCourses = async () => {
      if (!studentId.value) {
        error.value = '请先输入学生ID'
        return
      }

      loading.value = true
      error.value = ''
      courses.value = []

      try {
        const data = await getStudentCourses(studentId.value)
        courses.value = data || []
      } catch (err) {
        error.value = err.message || '加载课程失败'
        console.error('Load courses error:', err)
      } finally {
        loading.value = false
      }
    }

    /**
     * 处理退课
     */
    const handleDrop = async (course) => {
      if (!studentId.value) {
        return
      }

      droppingCourses.value.add(course.courseId)

      try {
        const response = await dropCourse({
          studentId: studentId.value,
          courseId: course.courseId
        })

        if (response.success) {
          closeDialog()
          setTimeout(() => {
            loadCourses()
          }, 500)
        } else {
          alert(response.message || '退课失败')
        }
      } catch (err) {
        alert(err.message || '退课失败')
        console.error('Drop error:', err)
      } finally {
        droppingCourses.value.delete(course.courseId)
      }
    }

    /**
     * 计算总学分
     */
    const totalCredits = computed(() => {
      return courses.value.reduce((sum, course) => {
        return sum + (course.credits || 0)
      }, 0)
    })

    // 监听学生ID变化，自动加载课程
    watch(studentId, (newId) => {
      if (newId) {
        loadCourses()
      } else {
        courses.value = []
        error.value = ''
      }
    })

    // 组件挂载时，如果已有学生ID，自动加载
    onMounted(() => {
      if (studentId.value) {
        loadCourses()
      }
    })

    // 监听ESC键关闭对话框
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        if (selectedCourse.value) {
          closeDialog()
        } else if (showAvailableCourses.value) {
          closeAvailableCoursesDialog()
        }
      }
    }
    onMounted(() => {
      window.addEventListener('keydown', handleKeyDown)
    })

    return {
      studentId,
      courses,
      loading,
      error,
      droppingCourses,
      totalCredits,
      weekdays,
      periods,
      selectedCourse,
      loadCourses,
      handleDrop,
      getCellBlocks,
      getBlockStyle,
      getCellConflictCount,
      isToday,
      showCourseDetail,
      closeDialog,
      showAvailableCourses,
      availableCourses,
      loadingAvailableCourses,
      availableCoursesError,
      queryTimeInfo,
      enrollingCourses,
      operationMessage,
      handleCellClick,
      closeAvailableCoursesDialog,
      handleEnrollFromDialog
    }
  }
}
</script>

<style scoped>
.my-courses {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #333;
}

.btn-refresh {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-refresh:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-refresh:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.no-student-id {
  text-align: center;
  padding: 40px;
  color: #999;
  background: #f5f5f5;
  border-radius: 4px;
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
  padding: 40px;
  color: #999;
}

.summary {
  margin-bottom: 20px;
  padding: 15px;
  background: #f0f7ff;
  border-radius: 4px;
  border: 1px solid #d4e8ff;
}

.summary p {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.summary strong {
  color: #1890ff;
}

.schedule-container {
  width: 100%;
  overflow-x: auto;
}

.schedule-wrapper {
  min-width: 800px;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 100px repeat(7, 1fr);
  grid-template-rows: 50px repeat(12, 80px);
  border: 1px solid #ddd;
  background: white;
}

.grid-header {
  background: #f5f5f5;
  border: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
}

.time-header {
  border-right: 2px solid #999;
}

.day-header {
  border-bottom: 2px solid #999;
  padding: 8px;
}

.day-header.today {
  background: #e6f7ff;
  color: #1890ff;
}

.day-name {
  font-size: 16px;
}

.period-header {
  background: #fafafa;
  border: 1px solid #ddd;
  border-right: 2px solid #999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.period-number {
  font-size: 12px;
  font-weight: bold;
  color: #333;
}

.period-time {
  font-size: 10px;
  color: #666;
  margin-top: 2px;
}

.schedule-cell {
  border: 1px solid #ddd;
  position: relative;
  padding: 2px;
  min-height: 80px;
  background: #fff;
  overflow: visible;
  /* 确保单元格内容可以跨行显示 */
  grid-column: auto;
}

.schedule-cell.empty-cell {
  cursor: pointer;
  transition: background-color 0.2s;
}

.schedule-cell.empty-cell:hover {
  background: #f0f8ff;
  border-color: #1890ff;
}

.course-block {
  border: 2px solid;
  border-radius: 4px;
  padding: 6px;
  margin: 2px 0;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  overflow: hidden;
  word-break: break-word;
  box-sizing: border-box;
  /* 确保课程块可以跨越多行 */
  min-height: 0;
}

.course-block:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
  z-index: 20;
}

.course-block.has-conflict {
  position: absolute;
  top: 2px;
  bottom: 2px;
  margin: 0;
}

/* 跨多节的课程块样式 */
.course-block {
  margin: 0;
}

.course-name-text {
  font-weight: bold;
  margin-bottom: 4px;
  line-height: 1.2;
}

.course-location {
  font-size: 10px;
  opacity: 0.8;
  line-height: 1.2;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #999;
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  animation: slideUp 0.3s;
}

.dialog-close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  font-size: 24px;
  line-height: 1;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.dialog-close:hover {
  background: #e0e0e0;
  color: #333;
}

.dialog-header {
  padding: 20px 50px 15px 20px;
  border-bottom: 1px solid #eee;
}

.dialog-header h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.dialog-body {
  padding: 20px;
}

.detail-item {
  margin-bottom: 15px;
  font-size: 14px;
  line-height: 1.6;
}

.detail-item strong {
  color: #333;
  margin-right: 8px;
}

.sessions-list {
  list-style: none;
  padding: 0;
  margin: 10px 0 0 0;
}

.sessions-list li {
  padding: 5px 0;
  color: #666;
}

.dialog-footer {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
}

.btn-drop-dialog {
  padding: 8px 16px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-drop-dialog:hover:not(:disabled) {
  background: #ff7875;
}

.btn-drop-dialog:disabled {
  background: #ccc;
  cursor: not-allowed;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 可选课程对话框样式 */
.available-courses-dialog {
  max-width: 800px;
  max-height: 85vh;
}

.available-courses-list {
  max-height: 60vh;
  overflow-y: auto;
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

.btn-enroll {
  padding: 8px 16px;
  background: #52c41a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-enroll:hover:not(:disabled) {
  background: #73d13d;
}

.btn-enroll:disabled {
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

/* 响应式设计 - 平板和手机 */
@media (max-width: 1024px) {
  .my-courses {
    padding: 15px;
  }
  
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .header h2 {
    font-size: 20px;
  }
  
  .schedule-wrapper {
    min-width: 700px;
  }
  
  .schedule-grid {
    grid-template-columns: 90px repeat(7, 1fr);
    grid-template-rows: 45px repeat(12, 70px);
  }
  
  .period-header {
    padding: 3px;
  }
  
  .period-number {
    font-size: 11px;
  }
  
  .period-time {
    font-size: 9px;
  }
  
  .course-block {
    font-size: 11px;
    padding: 5px;
  }
  
  .course-name-text {
    font-size: 11px;
  }
  
  .course-location {
    font-size: 9px;
  }
}

@media (max-width: 768px) {
  .my-courses {
    padding: 10px;
  }
  
  .header h2 {
    font-size: 18px;
  }
  
  .summary {
    padding: 12px;
    font-size: 14px;
  }
  
  .schedule-wrapper {
    min-width: 600px;
  }
  
  .schedule-grid {
    grid-template-columns: 75px repeat(7, 1fr);
    grid-template-rows: 40px repeat(12, 60px);
  }
  
  .grid-header {
    font-size: 12px;
    padding: 6px;
  }
  
  .day-name {
    font-size: 14px;
  }
  
  .period-header {
    padding: 2px;
  }
  
  .period-number {
    font-size: 10px;
  }
  
  .period-time {
    font-size: 8px;
  }
  
  .schedule-cell {
    min-height: 60px;
    padding: 1px;
  }
  
  .course-block {
    font-size: 10px;
    padding: 4px;
    border-width: 1.5px;
  }
  
  .course-name-text {
    font-size: 10px;
    margin-bottom: 2px;
  }
  
  .course-location {
    font-size: 8px;
  }
  
  /* 对话框在手机上的优化 */
  .dialog-content {
    width: 95%;
    max-width: none;
    max-height: 90vh;
    margin: 10px;
  }
  
  .dialog-header {
    padding: 15px 40px 12px 15px;
  }
  
  .dialog-header h3 {
    font-size: 18px;
  }
  
  .dialog-body {
    padding: 15px;
  }
  
  .detail-item {
    font-size: 13px;
    margin-bottom: 12px;
  }
  
  .btn-drop-dialog {
    width: 100%;
    padding: 10px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .my-courses {
    padding: 8px;
  }
  
  .header h2 {
    font-size: 16px;
  }
  
  .btn-refresh {
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .summary {
    padding: 10px;
    font-size: 13px;
  }
  
  .schedule-wrapper {
    min-width: 500px;
  }
  
  .schedule-grid {
    grid-template-columns: 65px repeat(7, 1fr);
    grid-template-rows: 35px repeat(12, 55px);
  }
  
  .grid-header {
    font-size: 11px;
    padding: 4px;
  }
  
  .day-name {
    font-size: 12px;
  }
  
  .period-header {
    padding: 1px;
  }
  
  .period-number {
    font-size: 9px;
  }
  
  .period-time {
    font-size: 7px;
  }
  
  .schedule-cell {
    min-height: 55px;
    padding: 1px;
  }
  
  .course-block {
    font-size: 9px;
    padding: 3px;
    border-width: 1px;
  }
  
  .course-name-text {
    font-size: 9px;
    margin-bottom: 1px;
  }
  
  .course-location {
    font-size: 7px;
  }
  
  .dialog-content {
    width: 98%;
    margin: 5px;
    border-radius: 6px;
  }
  
  .dialog-header {
    padding: 12px 35px 10px 12px;
  }
  
  .dialog-header h3 {
    font-size: 16px;
  }
  
  .dialog-body {
    padding: 12px;
  }
  
  .detail-item {
    font-size: 12px;
    margin-bottom: 10px;
  }
  
  .dialog-close {
    width: 28px;
    height: 28px;
    font-size: 20px;
    top: 8px;
    right: 8px;
  }
}
</style>
