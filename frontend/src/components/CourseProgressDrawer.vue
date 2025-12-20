<!-- src/components/CourseAssistant.vue -->
<template>
  <!-- ================================================= -->
  <!-- 1. 悬浮入口卡片 -->
  <!-- 
      修改点：
      1. ref="cardRef": 用于获取元素
      2. :style: 动态绑定 top 和 right，实现跟随鼠标
      3. @mousedown: 绑定鼠标按下事件，开始拖拽
  -->
  <!-- ================================================= -->
  <div 
    class="assistant-widget-card" 
    ref="cardRef"
    :style="{ 
      top: currentTop + 'px', 
      right: currentRight + 'px',
      cursor: isDragging ? 'grabbing' : 'grab' 
    }"
    @mousedown="startDrag"
  >
    
    <!-- 状态概览区 -->
    <div class="widget-stats">
      <!-- 左侧：课程与学分 -->
      <div class="stat-item">
        <span class="stat-label">已选课程</span>
        <div class="stat-value-group">
          <span class="stat-num">{{ selectedCourseCount }}</span>
          <span class="stat-unit">门</span>
          <span class="stat-divider">/</span>
          <span class="stat-num">{{ selectedTotalCredits }}</span>
          <span class="stat-unit">学分</span>
        </div>
      </div>

      <!-- 右侧：进度环 -->
      <div class="progress-wrapper">
        <div class="mini-progress-ring" :style="progressRingStyle">
          <div class="ring-center">{{ Math.round(totalProgress) }}%</div>
        </div>
        <span class="progress-label">毕业进度</span>
      </div>
    </div>

    <!-- 按钮操作区 (阻止冒泡，防止点击按钮时触发拖拽) -->
    <div class="widget-actions" @mousedown.stop>
      <button class="btn-primary-gradient" @click="openDrawer('selected')">
        查看已选列表
      </button>
      <button class="btn-secondary-outline" @click="openDrawer('plan')">
        培养方案建议
      </button>
    </div>
  </div>

  <!-- ================================================= -->
  <!-- 2. 抽屉弹窗 (逻辑保持不变) -->
  <!-- ================================================= -->
  <teleport to="body">
    <transition name="drawer-slide">
      <div v-if="drawerVisible" class="drawer-overlay" @click="closeDrawer">
        <div class="drawer-container" @click.stop>
          
          <!-- 头部 -->
          <div class="drawer-header">
            <div class="header-title">
              <h3>课程助手</h3>
              <button class="refresh-btn-mini" @click="fetchData" :disabled="loading" title="刷新数据">
                <span :class="{ 'spinning': loading }">↻</span>
              </button>
            </div>
            <button class="close-btn" @click="closeDrawer">✕</button>
          </div>

          <!-- 内容区 -->
          <div class="drawer-body">
            <!-- 省略中间内容，保持不变，仅为了节省篇幅 ... -->
            <!-- ... 这里放你原有的 drawer-tabs 和 template 内容 ... -->
             <div class="drawer-tabs">
              <button class="drawer-tab" :class="{ active: activeTab === 'selected' }" @click="activeTab = 'selected'">已选课程</button>
              <button class="drawer-tab" :class="{ active: activeTab === 'plan' }" @click="activeTab = 'plan'">培养方案建议</button>
            </div>
            
            <div v-if="loading && !selectedCourses.length && !progressData.length" class="state-box">
              <div class="spinner"></div>
              {{ activeTab === 'selected' ? '正在加载已选课程...' : '正在分析培养方案...' }}
            </div>
            <div v-else-if="error" class="state-box error">{{ error }}</div>

            <template v-else>
              <!-- 视图A: 已选 -->
              <template v-if="activeTab === 'selected'">
                <div class="summary-card selected-summary">
                  <div class="summary-info">
                    <span class="summary-label">当前已选</span>
                    <span class="summary-value"><span class="current">{{ selectedCourseCount }}</span> 门 / {{ selectedTotalCredits }} 学分</span>
                  </div>
                </div>
                <div v-if="selectedCourses.length === 0" class="no-rec-text">您还没有选课</div>
                <div v-else class="selected-list">
                  <div v-for="course in selectedCourses" :key="course.courseId" class="rec-course-card">
                    <div class="rec-course-main">
                      <div class="rec-course-name">{{ course.courseName }}</div>
                      <div class="rec-course-info">
                        <span class="rec-tag">{{ course.credits }}分</span>
                        <span class="rec-tag gray">{{ course.campus || '-' }}</span>
                        <span class="rec-teacher">{{ course.instructorName?.split(',')[0] }}</span>
                      </div>
                      <div v-if="course.sessions && course.sessions.length > 0" class="selected-sessions">
                         <div v-for="session in course.sessions" :key="session.sessionId" class="selected-session-item">
                          {{ session.weekday }} {{ session.startPeriod }}-{{ session.endPeriod }}节
                        </div>
                      </div>
                    </div>
                    <div class="rec-course-action">
                      <div v-if="operationMessage[course.courseId]" :class="['msg-toast', operationMessage[course.courseId].type]">{{ operationMessage[course.courseId].message }}</div>
                      <button class="btn-drop-mini" :disabled="droppingCourses.has(course.courseId)" @click="onDropClick(course)">{{ droppingCourses.has(course.courseId) ? '...' : '退课' }}</button>
                    </div>
                  </div>
                </div>
              </template>
              <!-- 视图B: 培养方案 -->
              <template v-else>
                 <div class="summary-card">
                  <div class="summary-info">
                    <span class="summary-label">毕业总学分</span>
                    <span class="summary-value"><span class="current">{{ totalEarned }}</span> / {{ totalRequired }}</span>
                  </div>
                  <div class="progress-bar-bg"><div class="progress-bar-fill" :style="{ width: totalProgress + '%' }"></div></div>
                </div>
                <div class="modules-list">
                  <div v-for="(category, index) in progressData" :key="index" class="module-group">
                    <div class="category-header"><h4 class="category-title">{{ category.name }}</h4></div>
                    <div class="sub-items-list">
                      <div v-for="item in category.items" :key="item.name" class="sub-item-container">
                        <div class="waffle-item">
                          <div class="waffle-header">
                            <span class="course-label">{{ item.name }}</span>
                            <div class="header-right">
                              <span class="course-score">{{ item.earned }}/{{ item.required }}</span>
                              <button v-if="item.earned < item.required" class="btn-recommend-small" @click="toggleExpand(item.name)">{{ expandedCategories.has(item.name) ? '收起' : '推荐课程' }}</button>
                            </div>
                          </div>
                          <div class="waffle-cells">
                            <template v-if="item.required > 0">
                               <div v-for="n in Math.min(item.required, 20)" :key="n" class="cell" :class="{ 'filled': n <= item.earned, 'enrolled': n > item.earned && n <= (item.earned + (item.enrolled || 0)), 'empty': n > (item.earned + (item.enrolled || 0)) }"></div>
                            </template>
                            <span v-else class="no-req-text">无硬性要求</span>
                          </div>
                        </div>
                        <div v-show="expandedCategories.has(item.name)" class="recommendation-list">
                           <div v-if="getCategoryRecommendations(item.name).length > 0">
                             <div v-for="course in getCategoryRecommendations(item.name)" :key="course.courseId" class="rec-course-card">
                               <div class="rec-course-main">
                                 <div class="rec-course-name">{{ course.courseName }}</div>
                                 <div class="rec-course-info"><span class="rec-tag">{{ course.credits }}分</span><span class="rec-tag gray">{{ course.campus }}</span></div>
                               </div>
                               <div class="rec-course-action">
                                 <div v-if="operationMessage[course.courseId]" :class="['msg-toast', operationMessage[course.courseId].type]">{{ operationMessage[course.courseId].message }}</div>
                                 <button class="btn-enroll-mini" :disabled="enrollingCourses.has(course.courseId)" @click="onEnrollClick(course)">选课</button>
                               </div>
                             </div>
                           </div>
                           <div v-else class="no-rec-text">暂无推荐</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </template>
          </div>
        </div>
      </div>
    </transition>
  </teleport>

  <ConfirmDialog
    :visible="showDialog"
    :title="dialogTitle"
    :message="dialogMessage"
    :confirm-text="dialogConfirmText"
    :type="dialogType"
    :course="currentCourse"
    :conflicts="conflictList"
    :capacity-warning="capacityWarning"
    :loading="enrollingCourses.has(currentCourse?.courseId) || droppingCourses.has(currentCourse?.courseId)"
    @confirm="handleConfirm"
    @cancel="showDialog = false"
  />
</template>

<script>
import { computed, ref, inject, reactive, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecommendations as fetchRecommendationsApi, enrollCourse, getStudentCourses, dropCourse } from '../api/courseApi'
import ConfirmDialog from './ConfirmDialog.vue'
import { checkTimeConflict } from '../utils/conflictChecker'

export default {
  name: 'CourseAssistant',
  components: { ConfirmDialog },
  props: {
    // 初始 top 位置，支持传入 '200px' 或数字 200
    initialTop: { type: [String, Number], default: 245 },
    initialRight: { type: [String, Number], default: 40 }
  },
  emits: ['course-enrolled', 'course-dropped'],
  setup(props, { emit }) {
    const studentId = inject('studentId')

    // --- 拖拽核心逻辑 Start ---
    const cardRef = ref(null)
    const isDragging = ref(false)
    
    // 将初始位置转化为数字进行计算
    const parsePos = (val) => typeof val === 'string' ? parseInt(val.replace('px', '')) : val
    const currentTop = ref(parsePos(props.initialTop))
    const currentRight = ref(parsePos(props.initialRight))
    
    // 记录拖拽开始时的状态
    let startX = 0
    let startY = 0
    let startTop = 0
    let startRight = 0

    const startDrag = (e) => {
      // 只有左键点击才触发
      if (e.button !== 0) return
      
      isDragging.value = true
      startX = e.clientX
      startY = e.clientY
      startTop = currentTop.value
      startRight = currentRight.value

      // 防止选中文本
      e.preventDefault()

      // 绑定全局事件，防止鼠标移出浏览器失效
      document.addEventListener('mousemove', onDrag)
      document.addEventListener('mouseup', stopDrag)
    }

    const onDrag = (e) => {
      if (!isDragging.value) return

      // 计算位移量
      const deltaX = e.clientX - startX
      const deltaY = e.clientY - startY

      // 更新位置
      // 注意：这里使用的是 right 定位，鼠标向右移(deltaX > 0)，right 应该减小
      currentTop.value = startTop + deltaY
      currentRight.value = startRight - deltaX 
    }

    const stopDrag = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', onDrag)
      document.removeEventListener('mouseup', stopDrag)
    }
    // --- 拖拽核心逻辑 End ---

    // 业务逻辑保持原样
    const drawerVisible = ref(false)
    const activeTab = ref('plan')
    const progressData = ref([])
    const selectedCourses = ref([])
    const recommendationDict = ref({})
    const loading = ref(false)
    const error = ref('')
    const enrollingCourses = ref(new Set())
    const droppingCourses = ref(new Set())
    const operationMessage = ref({})
    const expandedCategories = reactive(new Set())

    const openDrawer = (tab) => {
      activeTab.value = tab
      drawerVisible.value = true
      if (selectedCourses.value.length === 0) fetchData()
    }
    const closeDrawer = () => drawerVisible.value = false

    // 分类映射表 (同之前)
    const CATEGORY_MAP = {
      '大学数学': '必修课程', '大学英语': '必修课程', '思政': '必修课程',
      '体育': '必修课程', '军事课': '必修课程', '军理课': '必修课程',
      '学科基础课程': '专业课程', '专业选修': '专业课程', '毕业论文': '专业课程',
      '科光': '通识教育', '悦读': '通识教育', '阅读': '通识教育',
      '美育': '通识教育', '通识课': '通识教育', '通识': '通识教育'
    }

    const fetchData = async () => {
      if (!studentId.value) return
      loading.value = true
      error.value = ''
      try {
        const [recData, enrolledData] = await Promise.all([
          fetchRecommendationsApi({ studentId: parseInt(studentId.value) }),
          getStudentCourses(parseInt(studentId.value))
        ])
        selectedCourses.value = Array.isArray(enrolledData) ? enrolledData : []
        const enrolledCreditsMap = {}
        if (selectedCourses.value.length > 0) {
          selectedCourses.value.forEach(course => {
            const type = course.type || course.courseType || '其他'
            if (!enrolledCreditsMap[type]) enrolledCreditsMap[type] = 0
            enrolledCreditsMap[type] += (course.credits || 0)
          })
        }
        if (recData && recData.creditProgress) {
          const groups = { '必修课程': [], '专业课程': [], '通识教育': [] }
          recData.creditProgress.forEach(item => {
             const type = item.type
             let groupName = CATEGORY_MAP[type] || '专业课程'
             groups[groupName].push({
               name: item.type, required: item.required, earned: item.earned, enrolled: enrolledCreditsMap[type] || 0
             })
          })
          const order = ['必修课程', '专业课程', '通识教育']
          progressData.value = order
            .filter(key => groups[key] && groups[key].length > 0)
            .map(key => ({ name: key, items: groups[key] }))
        } else { progressData.value = [] }
        recommendationDict.value = recData?.recommendations || {}
      } catch (err) { error.value = "无法获取数据"; console.error(err) } finally { loading.value = false }
    }

    const getCategoryRecommendations = (n) => recommendationDict.value[n] || []
    const toggleExpand = (n) => expandedCategories.has(n) ? expandedCategories.delete(n) : expandedCategories.add(n)
    const setOperationMessage = (id, type, msg) => {
      operationMessage.value[id] = { type, message: msg }
      setTimeout(() => { if (operationMessage.value[id]) delete operationMessage.value[id] }, 5000)
    }

    const showDialog = ref(false)
    const dialogType = ref('enroll')
    const currentCourse = ref(null)
    const conflictList = ref([])
    const dialogTitle = computed(() => dialogType.value === 'enroll' ? '确认选课' : '确认退课')
    const dialogMessage = computed(() => {
      if (!currentCourse.value) return ''
      if (dialogType.value === 'enroll') return conflictList.value.length > 0 ? `冲突与${conflictList.value[0].courseName}。继续？` : `确定选择“${currentCourse.value.courseName}”吗？`
      return `确定退选“${currentCourse.value.courseName}”吗？`
    })
    const dialogConfirmText = computed(() => dialogType.value === 'enroll' ? '选课' : '退课')
    const capacityWarning = computed(() => {
      if (dialogType.value !== 'enroll') return ''
      const enrolled = Number(currentCourse.value?.enrolledCount || 0)
      const cap = Number(currentCourse.value?.capacity || 0)
      if (cap > 0 && enrolled >= cap) return '人数已满，抽签风险大'
      return ''
    })

    const onEnrollClick = (c) => { currentCourse.value = c; dialogType.value = 'enroll'; conflictList.value = checkTimeConflict(c, selectedCourses.value); showDialog.value = true }
    const onDropClick = (c) => { currentCourse.value = c; dialogType.value = 'drop'; conflictList.value = []; showDialog.value = true }
    const handleConfirm = async () => { if(dialogType.value === 'enroll') await handleEnroll(currentCourse.value); else await handleDrop(currentCourse.value); showDialog.value = false }

    const handleEnroll = async (course) => {
      if (!studentId.value) return setOperationMessage(course.courseId, 'error', '未登录')
      enrollingCourses.value.add(course.courseId)
      const batchId = localStorage.getItem('selectedBatchId')
      if (!batchId) return enrollingCourses.value.delete(course.courseId) || setOperationMessage(course.courseId, 'error', '未选轮次')
      try {
        const res = await enrollCourse({ studentId: studentId.value, courseId: course.courseId, batchId: Number(batchId) })
        if (res.success) { setOperationMessage(course.courseId, 'success', res.message); ElMessage.success(res.message); course.enrolledCount = (course.enrolledCount||0)+1; emit('course-enrolled'); fetchData() }
        else setOperationMessage(course.courseId, 'error', res.message)
      } catch (e) { setOperationMessage(course.courseId, 'error', e.message) } finally { enrollingCourses.value.delete(course.courseId) }
    }

    const handleDrop = async (course) => {
      if (!studentId.value) return
      droppingCourses.value.add(course.courseId)
      try {
        const res = await dropCourse({ studentId: studentId.value, courseId: course.courseId })
        if (res.success) { setOperationMessage(course.courseId, 'success', '退课成功'); ElMessage.success('退课成功'); emit('course-dropped'); fetchData() }
        else setOperationMessage(course.courseId, 'error', res.message)
      } catch (e) { setOperationMessage(course.courseId, 'error', e.message) } finally { droppingCourses.value.delete(course.courseId) }
    }

    const totalRequired = computed(() => { let s=0; progressData.value.forEach(c=>c.items.forEach(i=>s+=i.required)); return s; })
    const totalEarned = computed(() => { let s=0; progressData.value.forEach(c=>c.items.forEach(i=>s+=i.earned)); return s; })
    const totalEnrolled = computed(() => { let s=0; progressData.value.forEach(c=>c.items.forEach(i=>s+=(i.enrolled||0))); return s; })
    const totalProgress = computed(() => totalRequired.value===0?0:Math.min(100, (totalEarned.value/totalRequired.value)*100))
    const selectedCourseCount = computed(() => selectedCourses.value.length)
    const selectedTotalCredits = computed(() => selectedCourses.value.reduce((sum, c) => sum + (c.credits || 0), 0))
    const progressRingStyle = computed(() => ({ background: `conic-gradient(#7C1F89 ${totalProgress.value}%, #e0e0e0 ${totalProgress.value}% 100%)` }))

    onMounted(() => { if (studentId.value) fetchData() })
    // 清理事件监听，虽然 stopDrag 里移除了，但为了保险起见
    watch(
      () => studentId.value, 
      (newId) => {
        if (newId) {
          console.log('检测到登录状态变化，自动加载数据...')
          fetchData()
        }
    },
    { immediate: true } // immediate: true 确保如果组件创建时已有 ID 也会执行一次
  )
    onUnmounted(() => {
      document.removeEventListener('mousemove', onDrag)
      document.removeEventListener('mouseup', stopDrag)
    })

    return {
      // 拖拽相关
      cardRef, isDragging, currentTop, currentRight, startDrag,
      // 业务相关
      activeTab, drawerVisible, openDrawer, closeDrawer,
      progressData, selectedCourses, loading, error,
      totalRequired, totalEarned, totalEnrolled, totalProgress, progressRingStyle,
      selectedCourseCount, selectedTotalCredits,
      expandedCategories, toggleExpand, getCategoryRecommendations,
      showDialog, dialogType, currentCourse, conflictList, dialogTitle, dialogMessage, dialogConfirmText, capacityWarning,
      onEnrollClick, onDropClick, handleConfirm,
      enrollingCourses, droppingCourses, operationMessage, fetchData
    }
  }
}
</script>

<style scoped>
/* ========================================= */
/* 1. 悬浮入口卡片 (带拖拽交互)              */
/* ========================================= */
.assistant-widget-card {
  position: fixed;
  /* top 和 right 现在由 inline style 控制 */
  z-index: 1500;
  
  /* 透明度调整与毛玻璃 */
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  
  border: 1px solid rgba(240, 230, 245, 0.6);
  border-radius: 16px;
  padding: 16px;
  width: 210px;
  box-shadow: 0 10px 30px -5px rgba(124, 31, 137, 0.15), 
              0 4px 6px -2px rgba(124, 31, 137, 0.05);
  
  display: flex;
  flex-direction: column;
  gap: 14px;
  
  /* 交互效果 */
  user-select: none; /* 防止拖拽时选中文字 */
  transition: box-shadow 0.2s, background 0.2s; /* 移除 transform 动画，否则拖拽会有延迟感 */
}

/* 鼠标悬停或拖拽时的样式 */
.assistant-widget-card:hover,
.assistant-widget-card:active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 15px 35px -5px rgba(124, 31, 137, 0.25);
}

.widget-stats { display: flex; justify-content: space-between; align-items: flex-start; }
.stat-item { display: flex; flex-direction: column; }
.stat-label { font-size: 12px; color: #888; margin-bottom: 4px; }
.stat-value-group { display: flex; align-items: baseline; gap: 2px; flex-wrap: wrap; }
.stat-num { font-size: 20px; font-weight: 800; color: #7C1F89; line-height: 1; }
.stat-unit { font-size: 11px; color: #7C1F89; font-weight: 600; margin-right: 2px; }
.stat-divider { font-size: 14px; color: #ccc; margin: 0 3px; }

.progress-wrapper { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.mini-progress-ring { width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; position: relative; }
.ring-center { width: 30px; height: 30px; background: white; border-radius: 50%; font-size: 10px; display: flex; align-items: center; justify-content: center; font-weight: 700; color: #555; }
.progress-label { font-size: 10px; color: #999; transform: scale(0.9); }

.widget-actions { display: flex; flex-direction: column; gap: 8px; }
.btn-primary-gradient { background: linear-gradient(135deg, #9b59b6, #7C1F89); color: white; border: none; padding: 8px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 10px rgba(124, 31, 137, 0.2); }
.btn-primary-gradient:hover { box-shadow: 0 6px 15px rgba(124, 31, 137, 0.3); transform: translateY(-1px); }
.btn-secondary-outline { background: transparent; color: #7C1F89; border: 1px solid rgba(124, 31, 137, 0.2); padding: 7px 12px; border-radius: 8px; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.btn-secondary-outline:hover { background: rgba(248, 240, 252, 0.8); border-color: #7C1F89; }

/* 抽屉样式保持不变... */
.drawer-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0, 0, 0, 0.4); z-index: 2000; display: flex; justify-content: flex-end; backdrop-filter: blur(2px); }
.drawer-container { width: 440px; height: 100%; background: white; box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1); display: flex; flex-direction: column; }
.drawer-header { padding: 16px 20px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; background: #fff; }
.header-title { display: flex; align-items: center; gap: 10px; }
.drawer-header h3 { margin: 0; color: #333; font-size: 18px; font-weight: 700; }
.refresh-btn-mini { background: none; border: none; color: #7C1F89; cursor: pointer; font-size: 18px; padding: 4px; border-radius: 50%; }
.refresh-btn-mini:hover { background: #f3e5f5; }
.spinning { display: inline-block; animation: spin 1s linear infinite; }
.close-btn { background: #f5f5f5; border: none; width: 32px; height: 32px; border-radius: 50%; color: #666; cursor: pointer; transition: all 0.2s; }
.close-btn:hover { background: #eee; color: #333; }
.drawer-body { flex: 1; overflow-y: auto; padding: 20px; scrollbar-width: thin; }
.drawer-body::-webkit-scrollbar { width: 6px; }
.drawer-body::-webkit-scrollbar-thumb { background-color: #e0e0e0; border-radius: 10px; }
.drawer-tabs { display: flex; gap: 8px; margin-bottom: 20px; background: #f5f5f5; padding: 4px; border-radius: 10px; }
.drawer-tab { flex: 1; padding: 8px 10px; border-radius: 8px; border: none; background: transparent; color: #666; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.drawer-tab.active { background: #fff; color: #7C1F89; box-shadow: 0 2px 6px rgba(0,0,0,0.05); }
.summary-card { padding: 20px; border-radius: 16px; color: white; margin-bottom: 24px; position: relative; overflow: hidden; }
.selected-summary { background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%); box-shadow: 0 8px 16px rgba(25, 118, 210, 0.15); }
.summary-card:not(.selected-summary) { background: linear-gradient(135deg, #7C1F89 0%, #9C27B0 100%); box-shadow: 0 8px 16px rgba(124, 31, 137, 0.15); }
.summary-info { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 12px; }
.summary-value .current { font-size: 28px; font-weight: 800; }
.progress-bar-bg { height: 8px; background: rgba(255, 255, 255, 0.2); border-radius: 4px; overflow: hidden; }
.progress-bar-fill { height: 100%; background: #fff; border-radius: 4px; transition: width 0.8s cubic-bezier(0.34, 1.56, 0.64, 1); }
.category-header { margin-bottom: 14px; }
.category-title { margin: 0; font-size: 16px; color: #333; font-weight: 700; border-left: 4px solid #7C1F89; padding-left: 12px; }
.sub-item-container { background: #fff; border: 1px solid #f0f0f0; border-radius: 12px; overflow: hidden; margin-bottom: 16px; transition: all 0.3s; }
.sub-item-container:hover { border-color: #e1bee7; box-shadow: 0 4px 12px rgba(0,0,0,0.03); }
.waffle-item { padding: 14px; }
.waffle-header { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 10px; }
.btn-recommend-small { padding: 4px 10px; font-size: 11px; border: 1px solid #7C1F89; color: #7C1F89; background: white; border-radius: 6px; cursor: pointer; }
.waffle-cells { display: flex; flex-wrap: wrap; gap: 5px; }
.cell { width: 14px; height: 14px; border-radius: 3px; background-color: #f0f0f0; }
.cell.filled { background-color: #7C1F89; }
.cell.enrolled { background-color: #FFC107; }
.rec-course-card { display: flex; justify-content: space-between; align-items: flex-start; padding: 12px; margin-bottom: 10px; background: #fff; border: 1px solid #eee; border-radius: 10px; }
.rec-course-card:hover { border-color: #7C1F89; box-shadow: 0 4px 12px rgba(124, 31, 137, 0.08); }
.rec-course-main { flex: 1; }
.rec-course-name { font-size: 15px; font-weight: 700; color: #333; margin-bottom: 6px; }
.rec-course-info { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.rec-tag { font-size: 11px; padding: 2px 6px; border-radius: 4px; background: #f3e5f5; color: #7C1F89; }
.rec-tag.gray { background: #f5f5f5; color: #666; }
.selected-sessions { margin-top: 8px; font-size: 12px; color: #666; background: #f9f9f9; padding: 6px 10px; border-radius: 6px; }
.selected-session-item { display: flex; align-items: center; gap: 4px; }
.week-tag-mini { color: #7C1F89; font-weight: 700; font-size: 11px; }
.rec-course-action { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; margin-left: 12px; min-width: 70px; }
.btn-enroll-mini, .btn-drop-mini { padding: 6px 14px; border: none; border-radius: 6px; font-size: 12px; font-weight: 700; cursor: pointer; width: 100%; }
.btn-enroll-mini { background: #7C1F89; color: white; }
.btn-drop-mini { background: #ff4d4f; color: white; }
.msg-toast { font-size: 10px; padding: 4px 8px; border-radius: 4px; white-space: nowrap; max-width: 120px; overflow: hidden; text-overflow: ellipsis; }
.msg-toast.success { color: #52c41a; background: #f6ffed; border: 1px solid #b7eb8f; }
.msg-toast.error { color: #ff4d4f; background: #fff2f0; border: 1px solid #ffccc7; }
.no-rec-text { font-size: 13px; color: #999; padding: 40px 20px; text-align: center; }
.state-box { text-align: center; padding: 60px 20px; color: #999; }
.spinner { width: 20px; height: 20px; border: 2px solid #f3e5f5; border-top-color: #7C1F89; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 12px; }
.drawer-slide-enter-active, .drawer-slide-leave-active { transition: opacity 0.3s ease; }
.drawer-slide-enter-from, .drawer-slide-leave-to { opacity: 0; }
.drawer-slide-enter-active .drawer-container, .drawer-slide-leave-active .drawer-container { transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-slide-enter-from .drawer-container, .drawer-slide-leave-to .drawer-container { transform: translateX(100%); }
@keyframes spin { to { transform: rotate(360deg); } }
</style>