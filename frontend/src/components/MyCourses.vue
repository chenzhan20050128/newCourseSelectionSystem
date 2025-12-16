<template>
  <div class="my-courses">
    <div class="header">
      <div class="header-left">
        <h2>我的课程</h2>
        <div v-if="courses.length > 0" class="header-stats">
          <span class="stat-item">共 <strong>{{ courses.length }}</strong> 门</span>
          <span class="stat-divider">|</span>
          <span class="stat-item">总学分 <strong>{{ totalCredits }}</strong> 分</span>
        </div>
      </div>
      <div class="header-actions">
        <button 
          class="btn-list-view" 
          @click="fetchRecommendations"
          :disabled="loading || !studentId"
        >
          查看选课建议
        </button>
        <button 
          class="btn-list-view" 
          @click="showCourseList = true"
          :disabled="loading || !studentId || courses.length === 0"
        >
          查看已选课程
        </button>
        <button 
          class="btn-refresh" 
          @click="loadCourses"
          :disabled="loading || !studentId"
        >
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="!studentId" class="no-student-id">
      <p>请先在上方输入学生ID</p>
    </div>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <div v-if="successMessage" class="success-message-toast">
      {{ successMessage }}
    </div>

    <div v-if="loading && courses.length === 0" class="loading">
      加载中...
    </div>

    <!-- 课程表 -->
    <div v-if="courses.length > 0" class="schedule-container">

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
            <!-- 上午时段标签（第1节） -->
            <div v-if="period.period === 1" class="time-period-label morning-label">
              <span class="period-label-text">上午</span>
            </div>
            
            <!-- 午间分隔行（在第4节后） -->
            <div v-if="period.period === 5" class="time-break time-break-with-text">
              <span class="break-text">午 间</span>
            </div>
            
            <!-- 下午时段标签（第5节） -->
            <div v-if="period.period === 5" class="time-period-label afternoon-label">
              <span class="period-label-text">下午</span>
            </div>
            
            <!-- 傍晚分隔行（在第8节后） -->
            <div v-if="period.period === 9" class="time-break time-break-with-text">
              <span class="break-text">傍 晚</span>
            </div>
            
            <!-- 晚间时段标签（第9节） -->
            <div v-if="period.period === 9" class="time-period-label evening-label">
              <span class="period-label-text">晚间</span>
            </div>

            <!-- 左侧节次和时间（不显示时段标签的行） -->
            <div v-if="![1, 5, 9].includes(period.period)" class="period-header" :data-period="period.period">
              <div class="period-number">第{{ period.period }}节</div>
              <div class="period-time">{{ period.time }}</div>
            </div>
            
            <!-- 有时段标签的行，节次时间部分 -->
            <div v-else class="period-header period-header-with-label" :data-period="period.period">
              <div class="period-number">第{{ period.period }}节</div>
              <div class="period-time">{{ period.time }}</div>
            </div>

            <!-- 每天的单元格 -->
            <div 
              v-for="(day, dayIndex) in weekdays" 
              :key="`${period.period}-${dayIndex}`"
              class="schedule-cell"
              :class="{ 
                'empty-cell': getCellBlocks(day, period.period).length === 0,
                'show-confirm': isShowingConfirm(day, period.period)
              }"
              :data-day="day"
              :data-period="period.period"
              @mouseenter="handleCellHover(day, period.period)"
              @mouseleave="handleCellLeave(day, period.period)"
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
                <div v-if="hasMultipleCampuses" class="campus-badge" :style="getCampusBadgeStyle(block.course.courseId)">{{ (block.course.campus || '未知').replace('杀区', '') }}</div>
                <div v-if="block.session.weekType && block.session.weekType !== 0" class="week-type-badge" :style="getCampusBadgeStyle(block.course.courseId)">{{ block.session.weekType === 1 ? '单' : '双' }}</div>
                <div class="course-name-text">{{ block.course.courseName }}</div>
                <div class="course-location">{{ block.course.classroom }}</div>
              </div>
              
              <!-- 空单元格的确认按钮 -->
              <div 
                v-if="isShowingConfirm(day, period.period)" 
                class="confirm-buttons"
                @click.stop
              >
                <p class="confirm-text">查询可选课程？</p>
                <button class="btn-confirm-small" @click.stop="confirmSearchCourses">确认</button>
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

    <!-- 课程列表对话框 -->
    <div v-if="showCourseList" class="dialog-overlay" @click.self="showCourseList = false">
      <div class="dialog-content course-list-dialog">
        <div class="dialog-header">
          <h3>已选课程列表</h3>
          <button class="dialog-close" @click="showCourseList = false">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="course-list-table-container">
            <table class="course-list-table">
              <thead>
                <tr>
                  <th>课程名称</th>
                  <th>学分</th>
                  <th>校区</th>
                  <th>上课时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="course in courses" :key="course.courseId">
                  <td class="course-name-cell">
                    <div class="course-name-main">{{ course.courseName }}</div>
                    <div class="course-id-sub">{{ course.courseId }}</div>
                  </td>
                  <td>{{ course.credits }}</td>
                  <td>{{ course.campus || '-' }}</td>
                  <td class="course-time-cell">
                    <div v-for="session in course.sessions" :key="session.sessionId" class="session-item">
                      {{ session.weekday }} {{ session.startPeriod }}-{{ session.endPeriod }}节
                      <span v-if="session.weekType === 1" class="week-tag single">单</span>
                      <span v-if="session.weekType === 2" class="week-tag double">双</span>
                    </div>
                  </td>
                  <td>
                    <button 
                      class="btn-drop-small"
                      @click="handleDrop(course)"
                      :disabled="droppingCourses.has(course.courseId)"
                    >
                      {{ droppingCourses.has(course.courseId) ? '退课中...' : '退课' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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

    <!-- 选课建议对话框 -->
        <div v-if="showRecommendations" class="dialog-overlay" @click="showRecommendations = false">
          <div class="dialog-content available-courses-dialog" @click.stop>
            <button class="dialog-close" @click="showRecommendations = false">×</button>
            <div class="dialog-header">
              <h3>学分进度与选课建议</h3>
            </div>
            <div class="dialog-body">
              <div v-if="loadingRecommendations" class="loading">
                获取建议中...
              </div>
              <div v-else-if="recommendationsError" class="error-message">
                {{ recommendationsError }}
              </div>
              <div v-else-if="!creditProgress || creditProgress.length === 0" class="no-results">
                暂无进度数据
              </div>
              
              <!-- 新的布局：进度条列表 -->
              <div v-else class="progress-list">
                <div v-for="item in creditProgress" :key="item.type" class="progress-item-container">
                  
                  <!-- 进度条行 -->
                  <div class="progress-row">
                    <div class="progress-label">{{ item.type }}</div>
                    
                    <div class="progress-track-wrapper">
                      <div class="progress-track">
                        <div 
                          class="progress-bar"
                          :class="{ 'bar-green': item.earned >= item.required, 'bar-red': item.earned < item.required }"
                          :style="{ width: getProgressPercent(item.earned, item.required) + '%' }"
                        ></div>
                      </div>
                    </div>

                    <!-- 进度数值 (仅未完成时显示) -->
                    <div class="progress-value">
                      <span v-if="item.earned < item.required">
                        {{ item.earned }}/{{ item.required }}
                      </span>
                      <span v-else class="status-ok">
                        已完成
                      </span>
                    </div>

                    <!-- 操作按钮 (仅未完成且有推荐课程时显示) -->
                    <div class="progress-action">
                      <button 
                        v-if="item.earned < item.required" 
                        class="btn-recommend"
                        @click="toggleCategory(item.type)"
                      >
                        {{ expandedCategory === item.type ? '收起' : '推荐课程' }}
                      </button>
                    </div>
                  </div>

                  <!-- 展开的推荐课程列表 -->
                  <div v-if="expandedCategory === item.type" class="recommendation-dropdown">
                    
                    <div v-if="!recommendations[item.type] || recommendations[item.type].length === 0" class="no-recs-hint">
                      该类别暂无推荐课程
                    </div>

                    <div v-else class="course-grid">
                      <!-- 复用之前的课程卡片样式，稍微调整布局 -->
                      <div v-for="course in recommendations[item.type]" :key="course.courseId" class="course-card mini-card">
                        <div class="course-header">
                          <span class="course-name">{{ course.courseName }}</span>
                          <span class="credits">{{ course.credits }} 分</span>
                        </div>
                        <div class="course-info compact">
                          <p><strong>教师:</strong> {{ course.instructorName }}</p>
                          <p><strong>时间:</strong> 
                            <span v-for="(session, idx) in course.sessions" :key="idx">
                              {{ session.weekday }}{{ session.startPeriod }}-{{ session.endPeriod }} 
                            </span>
                          </p>
                          <p><strong>容量:</strong> {{ course.enrolledCount }}/{{ course.capacity }}</p>
                        </div>
                        
                        <!-- 操作反馈消息 -->
                        <div v-if="operationMessage[course.courseId]" 
                            :class="['operation-message', `message-${operationMessage[course.courseId].type}`]">
                          {{ operationMessage[course.courseId].message }}
                        </div>

                        <div class="course-actions">
                          <button 
                            class="btn-enroll small"
                            @click="handleEnrollFromDialog(course)"
                            :disabled="!studentId || enrollingCourses.has(course.courseId)"
                          >
                            {{ enrollingCourses.has(course.courseId) ? '...' : '选课' }}
                          </button>
                        </div>
                      </div>
                    </div>
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
import { getStudentCourses, dropCourse, searchCoursesBySession, enrollCourse, getRecommendations } from '../api/courseApi'

export default {
  name: 'MyCourses',
  setup() {
    // 获取学生ID
    const studentId = inject('studentId')
    
    const courses = ref([])
    const loading = ref(false)
    const error = ref('')
    const selectedCourse = ref(null)
    const showCourseList = ref(false)
    const successMessage = ref('')
    
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
    
    // 选课建议相关状态
    const showRecommendations = ref(false)
    const recommendations = ref([])
    const loadingRecommendations = ref(false)
    const recommendationsError = ref('')
    // 新增：学分进度数据
    const creditProgress = ref([])
    // 新增：当前展开的推荐课程分类（控制显示哪个分类的课程列表）
    const expandedCategory = ref('')

    const fetchRecommendations = async () => {
      if (!studentId.value) return
      
      loadingRecommendations.value = true
      recommendationsError.value = ''
      showRecommendations.value = true
      // 重置状态
      expandedCategory.value = ''
      
      try {
        const data = await getRecommendations({ studentId: parseInt(studentId.value) })
        
        // 1. 处理学分进度
        if (data && data.creditProgress) {
          creditProgress.value = data.creditProgress
        } else {
          creditProgress.value = []
        }

        // 2. 处理推荐课程字典 (保持 Object 结构以便通过 key 查找)
        // 注意：这里不需要像之前那样转成数组了，因为我们是通过点击进度条后的按钮，
        // 根据 type 去这个对象里取数据的
        if (data && data.recommendations) {
          recommendations.value = data.recommendations
        } else {
          recommendations.value = {}
        }

      } catch (err) {
        console.error('获取选课建议失败:', err)
        recommendationsError.value = err.message || '获取选课建议失败'
      } finally {
        loadingRecommendations.value = false
      }
    }

    // 新增：切换展开/收起推荐课程
    const toggleCategory = (categoryType) => {
      if (expandedCategory.value === categoryType) {
        expandedCategory.value = '' // 如果当前已展开，则收起
      } else {
        expandedCategory.value = categoryType // 展开新的
      }
    }

    // 新增：计算进度百分比
    const getProgressPercent = (earned, required) => {
      if (required === 0) return 100
      const percent = (earned / required) * 100
      return Math.min(percent, 100) // 不超过100%
    }


    const enrollingCourses = ref(new Set())
    
    // 确认对话框状态
    const showConfirmDialog = ref(false)
    const confirmDialogInfo = ref({ weekday: '', period: null })

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
      { period: 11, time: '20:30-21:20' }
    ]

    /**
     * 检测是否有跨校区课程
     */
    const hasMultipleCampuses = computed(() => {
      const campuses = new Set()
      courses.value.forEach(course => {
        if (course.campus) {
          campuses.add(course.campus)
        }
      })
      return campuses.size > 1
    })

    // 颜色池 - 50种丰富多彩的颜色
    const colorPalette = [
      { bg: '#FFE5E7', border: '#FF6B81', text: '#333' }, { bg: '#E0F7FA', border: '#00BCD4', text: '#333' },
      { bg: '#FFF9E6', border: '#FFB74D', text: '#333' }, { bg: '#E8F5E9', border: '#66BB6A', text: '#333' },
      { bg: '#F3E5F5', border: '#9C27B0', text: '#333' }, { bg: '#E3F2FD', border: '#42A5F5', text: '#333' },
      { bg: '#FFF3E0', border: '#FF9800', text: '#333' }, { bg: '#FCE4EC', border: '#EC407A', text: '#333' },
      { bg: '#E8EAF6', border: '#5C6BC0', text: '#333' }, { bg: '#F1F8E9', border: '#9CCC65', text: '#333' },
      { bg: '#FFF8E1', border: '#FFCA28', text: '#333' }, { bg: '#E0F2F1', border: '#26A69A', text: '#333' },
      { bg: '#FFEBEE', border: '#EF5350', text: '#333' }, { bg: '#F9FBE7', border: '#D4E157', text: '#333' },
      { bg: '#EDE7F6', border: '#7E57C2', text: '#333' }, { bg: '#FBE9E7', border: '#FF7043', text: '#333' },
      { bg: '#E1F5FE', border: '#29B6F6', text: '#333' }, { bg: '#F3E5F5', border: '#AB47BC', text: '#333' },
      { bg: '#FFF9C4', border: '#FDD835', text: '#333' }, { bg: '#E8F5E9', border: '#4CAF50', text: '#333' },
      { bg: '#FFE0B2', border: '#FB8C00', text: '#333' }, { bg: '#F8BBD0', border: '#F06292', text: '#333' },
      { bg: '#D1C4E9', border: '#7E57C2', text: '#333' }, { bg: '#C5CAE9', border: '#5C6BC0', text: '#333' },
      { bg: '#BBDEFB', border: '#42A5F5', text: '#333' }, { bg: '#B2EBF2', border: '#26C6DA', text: '#333' },
      { bg: '#B2DFDB', border: '#26A69A', text: '#333' }, { bg: '#C8E6C9', border: '#66BB6A', text: '#333' },
      { bg: '#DCEDC8', border: '#9CCC65', text: '#333' }, { bg: '#F0F4C3', border: '#D4E157', text: '#333' },
      { bg: '#FFF9C4', border: '#FFEE58', text: '#333' }, { bg: '#FFECB3', border: '#FFCA28', text: '#333' },
      { bg: '#FFE0B2', border: '#FFA726', text: '#333' }, { bg: '#FFCCBC', border: '#FF7043', text: '#333' },
      { bg: '#D7CCC8', border: '#8D6E63', text: '#333' }, { bg: '#CFD8DC', border: '#78909C', text: '#333' },
      { bg: '#E1BEE7', border: '#BA68C8', text: '#333' }, { bg: '#CE93D8', border: '#AB47BC', text: '#fff' },
      { bg: '#B39DDB', border: '#9575CD', text: '#fff' }, { bg: '#9FA8DA', border: '#7986CB', text: '#fff' },
      { bg: '#90CAF9', border: '#64B5F6', text: '#333' }, { bg: '#81D4FA', border: '#4FC3F7', text: '#333' },
      { bg: '#80DEEA', border: '#4DD0E1', text: '#333' }, { bg: '#80CBC4', border: '#4DB6AC', text: '#333' },
      { bg: '#A5D6A7', border: '#81C784', text: '#333' }, { bg: '#C5E1A5', border: '#AED581', text: '#333' },
      { bg: '#E6EE9C', border: '#DCE775', text: '#333' }, { bg: '#FFF59D', border: '#FFF176', text: '#333' },
      { bg: '#FFE082', border: '#FFD54F', text: '#333' }, { bg: '#FFCC80', border: '#FFB74D', text: '#333' }
    ]

    /**
     * 为课程分配颜色 - 使用哈希算法使颜色分布更随机
     */
    const getCourseColor = (courseId) => {
      // 使用简单的哈希算法让颜色分布更随机
      let hash = courseId * 2654435761 // 使用大质数
      hash = ((hash ^ (hash >> 16)) * 0x85ebca6b) >>> 0
      hash = ((hash ^ (hash >> 13)) * 0xc2b2ae35) >>> 0
      hash = (hash ^ (hash >> 16)) >>> 0
      const index = hash % colorPalette.length
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
      const sessionBlocks = [] // 存储所有session块
      const timeSlotGroups = new Map() // 按时间段分组

      // 第一步：创建所有课程块
      courses.value.forEach(course => {
        if (!course.sessions || course.sessions.length === 0) return

        course.sessions.forEach(session => {
          const sessionBlock = {
            course,
            session,
            weekday: session.weekday,
            startPeriod: session.startPeriod,
            endPeriod: session.endPeriod,
            span: session.endPeriod - session.startPeriod + 1,
            id: `${course.courseId}-${session.sessionId}`
          }
          sessionBlocks.push(sessionBlock)
        })
      })

      // 第二步：按weekday分组处理
      const weekdayGroups = new Map()
      sessionBlocks.forEach(block => {
        if (!weekdayGroups.has(block.weekday)) {
          weekdayGroups.set(block.weekday, [])
        }
        weekdayGroups.get(block.weekday).push(block)
      })

      // 第三步：为每个weekday的课程分配列位置
      weekdayGroups.forEach((blocks, weekday) => {
        // 检测冲突关系
        const conflicts = new Map() // 存储每个块与哪些块冲突
        blocks.forEach(block => {
          conflicts.set(block.id, new Set())
        })

        // 构建冲突图
        for (let i = 0; i < blocks.length; i++) {
          for (let j = i + 1; j < blocks.length; j++) {
            const b1 = blocks[i]
            const b2 = blocks[j]
            // 检查时间是否重叠
            if (!(b1.endPeriod < b2.startPeriod || b2.endPeriod < b1.startPeriod)) {
              conflicts.get(b1.id).add(b2.id)
              conflicts.get(b2.id).add(b1.id)
            }
          }
        }

        // 使用贪心算法分配列位置
        blocks.forEach(block => {
          const conflictingBlocks = conflicts.get(block.id)
          const usedColumns = new Set()
          
          // 找出冲突块已使用的列
          conflictingBlocks.forEach(conflictId => {
            const conflictBlock = blocks.find(b => b.id === conflictId)
            if (conflictBlock && conflictBlock.columnIndex !== undefined) {
              usedColumns.add(conflictBlock.columnIndex)
            }
          })

          // 分配第一个未使用的列
          let columnIndex = 0
          while (usedColumns.has(columnIndex)) {
            columnIndex++
          }
          block.columnIndex = columnIndex

          // 计算该块需要的总列数
          let maxColumn = columnIndex
          conflictingBlocks.forEach(conflictId => {
            const conflictBlock = blocks.find(b => b.id === conflictId)
            if (conflictBlock && conflictBlock.columnIndex !== undefined) {
              maxColumn = Math.max(maxColumn, conflictBlock.columnIndex)
            }
          })
          block.totalColumns = maxColumn + 1
        })

        // 更新所有冲突块的totalColumns为最大值
        blocks.forEach(block => {
          const conflictingBlocks = conflicts.get(block.id)
          let maxColumns = block.totalColumns || 1
          conflictingBlocks.forEach(conflictId => {
            const conflictBlock = blocks.find(b => b.id === conflictId)
            if (conflictBlock && conflictBlock.totalColumns) {
              maxColumns = Math.max(maxColumns, conflictBlock.totalColumns)
            }
          })
          block.totalColumns = maxColumns
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

      // 使用图着色算法分配的列信息
      const totalColumns = block.totalColumns || 1
      const columnIndex = block.columnIndex || 0

      let style = {
        background: color.bg,
        borderLeftColor: color.border,
        color: color.text,
        position: 'absolute',
        top: '3px',
        bottom: '3px',
        zIndex: totalColumns > 1 ? 10 : 5
      }

      // 跨多节的课程，设置高度
      if (block.span > 1) {
        style.height = `calc(${block.span} * 100% + ${(block.span - 1) * 2}px - 6px)`
        style.display = 'flex'
        style.flexDirection = 'column'
        style.justifyContent = 'center'
      }

      // 计算宽度和位置
      if (totalColumns > 1) {
        const widthPercent = 100 / totalColumns
        const leftPercent = columnIndex * widthPercent
        style.width = `calc(${widthPercent}% - 4px)`
        style.left = `calc(${leftPercent}% + 2px)`
      } else {
        style.width = 'calc(100% - 6px)'
        style.left = '3px'
      }

      return style
    }

    /**
     * 获取校区标识的样式（使用课程颜色的深色版本）
     */
    const getCampusBadgeStyle = (courseId) => {
      const color = getCourseColor(courseId)
      // 使用border颜色作为校区标识的背景色（通常是较深的颜色）
      return {
        background: color.border,
        color: 'white'
      }
    }

    /**
     * 获取单元格的冲突数量
     */
    const getCellConflictCount = (block, weekday, period) => {
      return block.totalColumns || 1
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
     * 处理单元格鼠标悬停事件
     * 如果单元格为空，在单元格内显示确认按钮
     */
    const handleCellHover = (weekday, period) => {
      // 检查该单元格是否有课程
      const cellBlocks = getCellBlocks(weekday, period)
      if (cellBlocks.length > 0) {
        // 如果有课程，不处理
        return
      }

      // 如果单元格为空，在单元格内显示确认按钮
      confirmDialogInfo.value = { weekday, period }
      showConfirmDialog.value = true
    }

    /**
     * 处理单元格鼠标离开事件
     */
    const handleCellLeave = (weekday, period) => {
      // 鼠标离开时关闭确认按钮
      if (showConfirmDialog.value && 
          confirmDialogInfo.value.weekday === weekday && 
          confirmDialogInfo.value.period === period) {
        cancelSearchCourses()
      }
    }

    /**
     * 判断是否显示确认按钮
     */
    const isShowingConfirm = (weekday, period) => {
      return showConfirmDialog.value && 
             confirmDialogInfo.value.weekday === weekday && 
             confirmDialogInfo.value.period === period
    }

    /**
     * 确认查询课程
     */
    const confirmSearchCourses = async () => {
      const { weekday, period } = confirmDialogInfo.value
      showConfirmDialog.value = false
      
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
     * 取消查询
     */
    const cancelSearchCourses = () => {
      showConfirmDialog.value = false
      confirmDialogInfo.value = { weekday: '', period: null }
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
        if (showConfirmDialog.value) {
          cancelSearchCourses()
        } else if (selectedCourse.value) {
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
      successMessage,
      error,
      droppingCourses,
      totalCredits,
      weekdays,
      periods,
      showCourseList,
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
      handleCellHover,
      handleCellLeave,
      hasMultipleCampuses,
      getCampusBadgeStyle,
      closeAvailableCoursesDialog,
      handleEnrollFromDialog,
      showConfirmDialog,
      confirmDialogInfo,
      confirmSearchCourses,
      cancelSearchCourses,
      isShowingConfirm,
      showRecommendations,
      recommendations,
      loadingRecommendations,
      recommendationsError,
      creditProgress,
      expandedCategory,
      toggleCategory,
      getProgressPercent,
      fetchRecommendations
    }
  }
}
</script>

<style scoped>
.my-courses {
  padding: 24px;
  background: #f8f5fa;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header h2 {
  margin: 0;
  color: #7C1F89;
  font-size: 24px;
  font-weight: 700;
}

.header-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #666;
}

.stat-item strong {
  color: #7C1F89;
  font-size: 18px;
  font-weight: 600;
}

.stat-divider {
  color: #ddd;
  font-weight: 300;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-list-view {
  padding: 8px 16px;
  background: white;
  color: #7C1F89;
  border: 1px solid #7C1F89;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-list-view:hover:not(:disabled) {
  background: #f3e5f5;
}

.btn-list-view:disabled {
  border-color: #ccc;
  color: #999;
  cursor: not-allowed;
}

.btn-refresh {
  padding: 8px 20px;
  background: #7C1F89;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(124, 31, 137, 0.3);
}

.btn-refresh:hover:not(:disabled) {
  background: #9C27B0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(124, 31, 137, 0.4);
}

.btn-refresh:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.no-student-id {
  text-align: center;
  padding: 40px;
  color: #6c757d;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin: 20px 0;
}

.error-message {
  background: linear-gradient(135deg, #fff5f5 0%, #ffe0e0 100%);
  border: 1px solid #ffb3b3;
  color: #d63031;
  padding: 14px 18px;
  border-radius: 10px;
  margin-bottom: 20px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(214, 48, 49, 0.1);
}

.loading {
  text-align: center;
  padding: 40px;
  color: #667eea;
  font-size: 16px;
  font-weight: 500;
}

.schedule-container {
  width: 100%;
  overflow-x: auto;
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.schedule-wrapper {
  min-width: 750px;
  max-width: 1200px;
  margin: 0 auto;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 35px 55px repeat(7, 1fr);
  grid-template-rows: 45px repeat(4, 65px) 16px repeat(4, 65px) 16px repeat(3, 65px);
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.grid-header:first-child {
  grid-column: 1 / 3;
  background: #7C1F89;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
}

.grid-header.day-header {
  background: #7C1F89;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.5px;
}

.time-header {
  background: #7C1F89;
  border-right: 2px solid rgba(255,255,255,0.3);
}

.day-header {
  border-right: 1px solid rgba(255,255,255,0.2);
  padding: 8px;
}

.day-header.today {
  background: #9C27B0;
  box-shadow: inset 0 0 0 2px rgba(255,255,255,0.3);
}

.day-name {
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

/* 时段标签（上午/下午/晚间） */
.time-period-label {
  background: linear-gradient(180deg, #f3e5f5 0%, #e1bee7 100%);
  color: #6a1b9a;
  display: flex;
  align-items: center;
  justify-content: center;
  writing-mode: vertical-rl;
  text-orientation: upright;
  border-right: 2px solid #dee2e6;
  border-bottom: 1px solid #e9ecef;
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 3px;
  padding: 8px 0;
}

.morning-label {
  grid-row: 2 / 6;
}

.afternoon-label {
  grid-row: 7 / 11;
}

.evening-label {
  grid-row: 12 / 15;
}

.period-label-text {
  display: inline-block;
}

.period-header {
  background: linear-gradient(90deg, #f8f9fa 0%, #e9ecef 100%);
  border: none;
  border-right: 2px solid #dee2e6;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4px 2px;
}

.period-header-with-label {
  background: linear-gradient(90deg, #f8f9fa 0%, #e9ecef 100%);
}

.period-number {
  font-size: 11px;
  font-weight: 600;
  color: #495057;
  margin-bottom: 2px;
}

.period-time {
  font-size: 9px;
  color: #6c757d;
  line-height: 1.2;
}

.schedule-cell {
  border-right: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
  position: relative;
  padding: 3px;
  min-height: 65px;
  background: #fff;
  overflow: visible;
  grid-column: auto;
  transition: all 0.2s ease;
}

/* 时间分隔行 */
.time-break-with-text {
  grid-column: 1 / -1;
  background: white;
  border-top: 1px solid #dee2e6;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.break-text {
  font-size: 12px;
  font-weight: 600;
  color: #333;
  letter-spacing: 30px;
}

.schedule-cell.empty-cell {
  cursor: pointer;
}

.schedule-cell.empty-cell:hover {
  background: #f8f5fa;
  box-shadow: inset 0 0 0 1px #7C1F89;
}

.schedule-cell.show-confirm {
  background: #f8f5fa;
  box-shadow: inset 0 0 0 2px #7C1F89;
}

/* 单元格内确认按钮 - 自然融入样式 */
.confirm-buttons {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(135deg, rgba(124, 31, 137, 0.05) 0%, rgba(156, 39, 176, 0.05) 100%);
  backdrop-filter: blur(2px);
  z-index: 15;
  padding: 8px;
}

.confirm-text {
  margin: 0;
  font-size: 10px;
  color: #7C1F89;
  font-weight: 600;
  white-space: nowrap;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.button-group {
  display: flex;
  gap: 4px;
}

.btn-confirm-small {
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 10px;
  font-weight: 600;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  background: #7C1F89;
  color: white;
}

.btn-confirm-small:hover {
  background: #9C27B0;
  box-shadow: 0 2px 5px rgba(124, 31, 137, 0.3);
}

.course-block {
  border: none;
  border-left: 3px solid;
  border-radius: 6px;
  padding: 6px 8px;
  margin: 1px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 11px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  overflow: hidden;
  word-break: break-word;
  box-sizing: border-box;
  min-height: 0;
  backdrop-filter: blur(10px);
  position: relative;
}

/* 校区标识 */
.campus-badge {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 5px;
  border-bottom-left-radius: 6px;
  box-shadow: -1px 1px 2px rgba(0,0,0,0.1);
  z-index: 2;
}

/* 单双周标识 */
.week-type-badge {
  position: absolute;
  top: 0;
  left: 0;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 5px;
  border-bottom-right-radius: 6px;
  box-shadow: 1px 1px 2px rgba(0,0,0,0.1);
  z-index: 2;
}

.course-block:hover .campus-badge {
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
  transform: translateY(-1px);
}

.course-block:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 20;
  border-left-width: 4px;
}

.course-block.has-conflict {
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 3px;
  right: 3px;
  margin: 0;
}

/* 跨多节的课程块样式 */
.course-block {
  margin: 0;
}

.course-name-text {
  font-weight: bold;
}

/* 课程列表对话框样式 */
.course-list-dialog {
  max-width: 800px;
}

.course-list-table-container {
  overflow-x: auto;
}

.course-list-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.course-list-table th {
  background: #f8f9fa;
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 2px solid #dee2e6;
}

.course-list-table td {
  padding: 12px;
  border-bottom: 1px solid #e9ecef;
  vertical-align: middle;
}

.course-name-main {
  font-weight: 600;
  color: #333;
}

.course-id-sub {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.session-item {
  margin-bottom: 2px;
  font-size: 13px;
  color: #666;
}

.week-tag {
  display: inline-block;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 3px;
  margin-left: 4px;
  color: white;
}

.week-tag.single {
  background-color: #7C1F89;
}

.week-tag.double {
  background-color: #1976D2;
}

.btn-drop-small {
  padding: 4px 10px;
  background: white;
  color: #ff4d4f;
  border: 1px solid #ff4d4f;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.btn-drop-small:hover:not(:disabled) {
  background: #fff1f0;
}

.btn-drop-small:disabled {
  border-color: #ccc;
  color: #ccc;
  cursor: not-allowed;
}

/* ont-weight: 600;
  margin-bottom: 3px;
  line-height: 1.3;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.course-location {
  font-size: 10px;
  opacity: 0.85;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-results {
  text-align: center;
  padding: 40px;
  color: #6c757d;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  font-size: 15px;
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
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.2);
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
    min-width: 650px;
  }
  
  .schedule-grid {
    grid-template-columns: 32px 50px repeat(7, 1fr);
    grid-template-rows: 42px repeat(4, 60px) 16px repeat(4, 60px) 16px repeat(3, 60px);
  }
  
  .time-period-label {
    font-size: 12px;
  }
  
  .period-header {
    padding: 3px 2px;
  }
  
  .period-number {
    font-size: 10px;
  }
  
  .period-time {
    font-size: 8px;
  }
  
  .course-block {
    font-size: 10px;
    padding: 5px 6px;
  }
  
  .campus-badge {
    font-size: 8px;
    padding: 1px 4px;
  }

  .week-type-badge {
    font-size: 8px;
    padding: 1px 4px;
  }
  
  .course-name-text {
    font-size: 10px;
    -webkit-line-clamp: 4;
  }
  
  .course-location {
    font-size: 8px;
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
    padding: 10px 14px;
    font-size: 13px;
  }
  
  .schedule-wrapper {
    min-width: 550px;
  }
  
  .schedule-grid {
    grid-template-columns: 30px 45px repeat(7, 1fr);
    grid-template-rows: 38px repeat(4, 55px) 14px repeat(4, 55px) 14px repeat(3, 55px);
  }
  
  .time-period-label {
    font-size: 11px;
  }
  
  .grid-header {
    font-size: 11px;
    padding: 5px;
  }
  
  .day-name {
    font-size: 12px;
  }
  
  .period-header {
    padding: 2px 1px;
  }
  
  .period-number {
    font-size: 9px;
  }
  
  .period-time {
    font-size: 7px;
  }
  
  .schedule-cell {
    min-height: 55px;
    padding: 2px;
  }
  
  .course-block {
    font-size: 9px;
    padding: 4px 5px;
    border-left-width: 2px;
  }
  
  .campus-badge {
    font-size: 7px;
    padding: 1px 3px;
  }

  .week-type-badge {
    font-size: 7px;
    padding: 1px 3px;
  }
  
  .course-name-text {
    font-size: 9px;
    margin-bottom: 2px;
    -webkit-line-clamp: 3;
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
    padding: 8px 12px;
    font-size: 12px;
  }
  
  .summary strong {
    font-size: 16px;
  }
  
  .schedule-wrapper {
    min-width: 480px;
  }
  
  .schedule-grid {
    grid-template-columns: 28px 42px repeat(7, 1fr);
    grid-template-rows: 32px repeat(4, 50px) 12px repeat(4, 50px) 12px repeat(3, 50px);
  }
  
  .time-period-label {
    font-size: 10px;
  }
  
  .grid-header {
    font-size: 10px;
    padding: 3px;
  }
  
  .day-name {
    font-size: 11px;
  }
  
  .period-header {
    padding: 1px;
  }
  
  .period-number {
    font-size: 8px;
  }
  
  .period-time {
    font-size: 7px;
  }
  
  .schedule-cell {
    min-height: 50px;
    padding: 2px;
  }
  
  .course-block {
    font-size: 8px;
    padding: 3px 4px;
    border-left-width: 2px;
  }
  
  .campus-badge {
    font-size: 7px;
    padding: 1px 2px;
  }

  .week-type-badge {
    font-size: 7px;
    padding: 1px 2px;
  }
  
  .course-name-text {
    font-size: 8px;
    margin-bottom: 1px;
    -webkit-line-clamp: 2;
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

.category-title {
  margin: 20px 0 10px;
  padding-left: 10px;
  border-left: 4px solid #7C1F89;
  color: #333;
  font-size: 18px;
}

.recommendation-group:first-child .category-title {
  margin-top: 0;
}

/* 进度条列表样式 */
.progress-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 10px 5px;
}

.progress-item-container {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 15px;
}

.progress-item-container:last-child {
  border-bottom: none;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 15px;
  height: 40px;
}

.progress-label {
  width: 100px; /* 固定宽度，对齐 */
  font-weight: 600;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.progress-track-wrapper {
  flex: 1; /* 占据剩余空间 */
  display: flex;
  align-items: center;
}

.progress-track {
  width: 100%;
  height: 12px;
  background-color: #e9ecef;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.1);
}

.progress-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s ease;
}

.bar-green {
  background: linear-gradient(90deg, #52c41a 0%, #73d13d 100%);
}

.bar-red {
  background: linear-gradient(90deg, #ff4d4f 0%, #ff7875 100%);
}

.progress-value {
  width: 60px;
  text-align: right;
  font-size: 13px;
  font-weight: 500;
  color: #666;
}

.status-ok {
  color: #52c41a;
  font-size: 12px;
}

.progress-action {
  width: 80px;
  display: flex;
  justify-content: flex-end;
}

.btn-recommend {
  padding: 4px 10px;
  background-color: white;
  color: #1890ff;
  border: 1px solid #1890ff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.btn-recommend:hover {
  background-color: #e6f7ff;
}

/* 推荐课程下拉区域 */
.recommendation-dropdown {
  margin-top: 15px;
  background: #fcfcfc;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 15px;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.no-recs-hint {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 10px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 15px;
}

/* 迷你课程卡片样式调整 */
.course-card.mini-card {
  margin-bottom: 0;
  padding: 12px;
  background: white;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.course-card.mini-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border-color: #1890ff;
}

.course-info.compact p {
  margin: 3px 0;
  font-size: 12px;
}

.btn-enroll.small {
  padding: 4px 12px;
  font-size: 12px;
}

/* 移动端适配调整 */
@media (max-width: 600px) {
  .progress-row {
    flex-wrap: wrap;
    height: auto;
    gap: 8px;
  }
  
  .progress-label {
    width: 100%;
    margin-bottom: 4px;
  }
  
  .progress-track-wrapper {
    min-width: 150px;
  }
}
</style>
