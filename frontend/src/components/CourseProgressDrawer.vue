<!-- src/components/CourseProgressDrawer.vue -->
<template>
  <teleport to="body">
    <transition name="drawer-slide">
      <div v-if="visible" class="drawer-overlay" @click="close">
        <div class="drawer-container" @click.stop>
          
          <!-- 头部 -->
          <div class="drawer-header">
            <h3>培养方案与选课建议</h3>
            <button class="close-btn" @click="close">✕</button>
          </div>

          <!-- 滚动内容区 -->
          <div class="drawer-body">
            
            <div v-if="loading" class="state-box">
              <div class="spinner"></div> 正在分析选课进度...
            </div>

            <div v-else-if="error" class="state-box error">
              {{ error }}
            </div>

            <template v-else>
              <!-- 总体进度大卡片 -->
              <div class="summary-card">
                <div class="summary-info">
                  <span class="summary-label">毕业总学分</span>
                  <span class="summary-value">
                    <span class="current">
                      {{ totalEarned }}
                      <span v-if="totalEnrolled > 0" class="enrolled-text">(+{{ totalEnrolled }})</span>
                    </span> / {{ totalRequired }}
                  </span>
                </div>
                <div class="progress-bar-bg">
                  <div class="progress-bar-fill" :style="{ width: totalProgress + '%' }"></div>
                </div>
              </div>

              <!-- 分类列表 -->
              <div class="modules-list">
                <div v-for="(category, index) in progressData" :key="index" class="module-group">
                  
                  <!-- 大分类标题 -->
                  <div class="category-header">
                    <h4 class="category-title">{{ category.name }}</h4>
                  </div>
                  
                  <!-- 子项列表 -->
                  <div class="sub-items-list">
                    <div v-for="item in category.items" :key="item.name" class="sub-item-container">
                      
                      <!-- 1. 进度概览 (华夫饼图) -->
                      <div class="waffle-item">
                        <div class="waffle-header">
                          <span class="course-label">{{ item.name }}</span>
                          <div class="header-right">
                            <span class="course-score">{{ item.earned }}/{{ item.required }}</span>
                            <!-- 推荐按钮：仅当未修满时显示 -->
                            <button 
                              v-if="item.earned < item.required"
                              class="btn-recommend-small"
                              @click="toggleExpand(item.name)"
                            >
                              {{ expandedCategories.has(item.name) ? '收起' : '推荐课程' }}
                            </button>
                          </div>
                        </div>
                        
                        <div class="waffle-cells">
                          <template v-if="item.required > 0">
                            <div 
                              v-for="n in item.required" 
                              :key="n"
                              class="cell"
                              :class="{ 
                                'filled': n <= item.earned, 
                                'enrolled': n > item.earned && n <= (item.earned + (item.enrolled || 0)),
                                'empty': n > (item.earned + (item.enrolled || 0)) 
                              }"
                            ></div>
                          </template>
                          <span v-else class="no-req-text">无硬性要求</span>
                        </div>
                      </div>

                      <!-- 2. 推荐课程列表 (展开后显示) -->
                      <div v-show="expandedCategories.has(item.name)" class="recommendation-list">
                        <div v-if="getCategoryRecommendations(item.name).length > 0">
                          
                          <div 
                            v-for="course in getCategoryRecommendations(item.name)" 
                            :key="course.courseId" 
                            class="rec-course-card"
                          >
                            <div class="rec-course-main">
                              <div class="rec-course-name">{{ course.courseName }}</div>
                              <div class="rec-course-info">
                                <span class="rec-tag">{{ course.credits }}分</span>
                                <span class="rec-tag gray">{{ course.campus }}</span>
                                <span class="rec-teacher">{{ course.instructorName?.split(',')[0] }}</span>
                              </div>
                            </div>
                            
                            <!-- 选课操作区 -->
                            <div class="rec-course-action">
                              <div v-if="operationMessage[course.courseId]" 
                                   :class="['msg-toast', operationMessage[course.courseId].type]">
                                {{ operationMessage[course.courseId].message }}
                              </div>
                              <button 
                                class="btn-enroll-mini"
                                :disabled="enrollingCourses.has(course.courseId)"
                                @click="handleEnroll(course)"
                              >
                                {{ enrollingCourses.has(course.courseId) ? '...' : '选课' }}
                              </button>
                            </div>
                          </div>
                        </div>
                        <div v-else class="no-rec-text">
                          暂无该分类的特定推荐课程
                        </div>
                      </div>

                    </div>
                  </div>

                </div>
              </div>
            </template>

          </div>
          
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script>
import { computed, ref, watch, inject, reactive } from 'vue'
import { getRecommendations as fetchRecommendationsApi, enrollCourse, getStudentCourses } from '../api/courseApi'

export default {
  name: 'CourseProgressDrawer',
  props: {
    visible: { type: Boolean, default: false }
  },
  emits: ['close', 'course-enrolled'],
  setup(props, { emit }) {
    const studentId = inject('studentId')
    
    // 进度数据 (对应华夫饼图)
    const progressData = ref([])
    // 推荐课程字典 (对应你提供的JSON: Key是分类名, Value是课程数组)
    const recommendationDict = ref({})
    
    const loading = ref(false)
    const error = ref('')
    
    // 选课相关状态
    const enrollingCourses = ref(new Set())
    const operationMessage = ref({})
    
    // 控制展开/收起的集合
    const expandedCategories = reactive(new Set())

    watch(() => props.visible, (newVal) => {
      if (newVal) fetchData()
    })

    // 分类映射表
    const CATEGORY_MAP = {
      // 必修课程
      '大学数学': '必修课程',
      '大学英语': '必修课程',
      '思政': '必修课程',
      '体育': '必修课程',
      '军事课': '必修课程',
      '军理课': '必修课程', // 兼容可能的数据返回

      // 专业课程
      '学科基础课程': '专业课程',
      '专业选修': '专业课程',
      '毕业论文': '专业课程',

      // 通识教育
      '科光': '通识教育',
      '悦读': '通识教育',
      '阅读': '通识教育', // 兼容可能的数据返回
      '美育': '通识教育',
      '通识课': '通识教育',
      '通识': '通识教育' // 兼容可能的数据返回
    }

    const fetchData = async () => {
      if (!studentId.value) return

      loading.value = true
      error.value = ''
      
      try {
        // 并行请求推荐数据和已选课程数据
        const [recData, enrolledData] = await Promise.all([
          fetchRecommendationsApi({ studentId: parseInt(studentId.value) }),
          getStudentCourses(parseInt(studentId.value))
        ])
        
        // 处理已选课程数据，统计各分类的已选学分
        const enrolledCreditsMap = {}
        if (enrolledData && Array.isArray(enrolledData)) {
          enrolledData.forEach(course => {
            // 修正：后端返回的字段是 type，不是 courseType
            const type = course.type || course.courseType || '其他'
            
            if (!enrolledCreditsMap[type]) {
              enrolledCreditsMap[type] = 0
            }
            enrolledCreditsMap[type] += (course.credits || 0)
          })
        }

        // 1. 处理进度数据 (creditProgress)
        if (recData && recData.creditProgress) {
          const groups = {
            '必修课程': [],
            '专业课程': [],
            '通识教育': []
          }

          recData.creditProgress.forEach(item => {
             const type = item.type
             // 简单分类逻辑
             let groupName = CATEGORY_MAP[type] || '专业课程'
             
             // 获取该分类的已选学分
             const enrolled = enrolledCreditsMap[type] || 0

             groups[groupName].push({
               name: item.type,
               required: item.required,
               earned: item.earned,
               enrolled: enrolled // 新增已选学分
             })
          })
          
          // 转换为数组，过滤掉空组
          // 按照 必修 -> 专业 -> 通识 的顺序排序
          const order = ['必修课程', '专业课程', '通识教育']
          progressData.value = order
            .filter(key => groups[key] && groups[key].length > 0)
            .map(key => ({
              name: key,
              items: groups[key]
            }))
        } else {
            progressData.value = []
        }

        // 2. 处理推荐课程数据 (recommendations)
        if (recData && recData.recommendations) {
          recommendationDict.value = recData.recommendations
        } else {
          recommendationDict.value = {}
        }

      } catch (err) {
        console.error("加载失败:", err)
        error.value = "无法获取选课建议"
      } finally {
        loading.value = false
      }
    }

    // 获取特定分类的推荐课程
    const getCategoryRecommendations = (categoryName) => {
      return recommendationDict.value[categoryName] || []
    }

    // 切换展开状态
    const toggleExpand = (categoryName) => {
      if (expandedCategories.has(categoryName)) {
        expandedCategories.delete(categoryName)
      } else {
        expandedCategories.add(categoryName)
      }
    }

    // 设置操作消息
    const setOperationMessage = (courseId, type, message) => {
      operationMessage.value[courseId] = { type, message }
      setTimeout(() => {
        if (operationMessage.value[courseId]) {
          delete operationMessage.value[courseId]
        }
      }, 5000)
    }

    // 选课逻辑
    const handleEnroll = async (course) => {
      if (!studentId.value) {
        setOperationMessage(course.courseId, 'error', '请先输入学生ID')
        return
      }

      enrollingCourses.value.add(course.courseId)
      
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
          // 更新本地状态
          course.enrolledCount = (course.enrolledCount || 0) + 1
          emit('course-enrolled') // 通知父组件刷新
        } else {
          setOperationMessage(course.courseId, 'error', response.message)
        }
      } catch (err) {
        setOperationMessage(course.courseId, 'error', err.message || '选课失败')
      } finally {
        enrollingCourses.value.delete(course.courseId)
      }
    }

    // 计算总学分
    const totalRequired = computed(() => {
      let sum = 0;
      progressData.value.forEach(cat => cat.items.forEach(item => sum += item.required));
      return sum;
    })

    const totalEarned = computed(() => {
      let sum = 0;
      progressData.value.forEach(cat => cat.items.forEach(item => sum += item.earned));
      return sum;
    })

    const totalEnrolled = computed(() => {
      let sum = 0;
      progressData.value.forEach(cat => cat.items.forEach(item => sum += (item.enrolled || 0)));
      return sum;
    })

    const totalProgress = computed(() => {
      if (totalRequired.value === 0) return 0;
      // 进度条可以包含已选学分，或者只显示已修学分，这里假设包含已选学分作为“预期进度”
      // 或者保持原样只显示已修。用户需求是“重新计算总学分”，可能指的是显示的数值。
      // 进度条本身通常反映已完成。如果想反映“已选”，可以用双色进度条，这里简单起见，
      // 让进度条只反映已修，或者已修+已选。
      // 鉴于华夫饼图区分了颜色，进度条也可以只反映已修，或者两者之和。
      // 这里暂且只计算已修的百分比，因为进度条背景通常是单一颜色。
      return Math.min(100, (totalEarned.value / totalRequired.value) * 100);
    })

    const close = () => emit('close')

    return {
      progressData,
      loading,
      error,
      totalRequired,
      totalEarned,
      totalEnrolled,
      totalProgress,
      expandedCategories,
      toggleExpand,
      getCategoryRecommendations,
      handleEnroll,
      enrollingCourses,
      operationMessage,
      close
    }
  }
}
</script>

<style scoped>
/* 保持原有基础布局样式 */
.drawer-overlay {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(0, 0, 0, 0.4); z-index: 2000;
  display: flex; justify-content: flex-end;
}
.drawer-container {
  width: 420px; /* 稍微加宽一点以容纳课程列表 */
  height: 100%; background: white;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  display: flex; flex-direction: column;
}

/* 头部 */
.drawer-header {
  padding: 20px; border-bottom: 1px solid #eee;
  display: flex; justify-content: space-between; align-items: center; background: #fafafa;
}
.drawer-header h3 { margin: 0; color: #333; font-size: 18px; }
.close-btn { background: none; border: none; font-size: 20px; color: #999; cursor: pointer; }

/* 内容区 */
.drawer-body { flex: 1; overflow-y: auto; padding: 20px; }

/* 状态提示 */
.state-box { text-align: center; padding: 40px; color: #999; }
.state-box.error { color: #ff4d4f; }
.spinner { display: inline-block; width: 12px; height: 12px; border: 2px solid #ccc; border-top-color: #7C1F89; border-radius: 50%; animation: spin 1s linear infinite; margin-right: 8px; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 总进度卡片 */
.summary-card {
  background: linear-gradient(135deg, #7C1F89 0%, #a25cb5 100%);
  padding: 20px; border-radius: 12px; color: white; margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(124, 31, 137, 0.2);
}
.summary-info { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 10px; }
.summary-value .current { font-size: 24px; font-weight: bold; }
.progress-bar-bg { height: 8px; background: rgba(255, 255, 255, 0.2); border-radius: 4px; overflow: hidden; }
.progress-bar-fill { height: 100%; background: #fff; border-radius: 4px; transition: width 0.5s ease; }

/* 分类列表 */
.module-group { margin-bottom: 20px; border-bottom: 1px solid #f0f0f0; padding-bottom: 20px; }
.module-group:last-child { border-bottom: none; }

.category-header {
  margin-bottom: 12px; padding: 4px 0;
}
.category-title {
  margin: 0; font-size: 18px; color: #333; font-weight: 700;
  border-left: 4px solid #7C1F89; padding-left: 10px;
}

/* 子项列表 */
.sub-items-list { display: flex; flex-direction: column; gap: 15px; }
.sub-item-container { background: #f9f9f9; border-radius: 8px; overflow: hidden; }

/* 华夫饼图样式调整 */
.waffle-item { padding: 12px; background: transparent; }
.waffle-header { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 8px; color: #555; }
.header-right { display: flex; align-items: center; gap: 10px; }
.course-label { font-weight: 600; color: #333; }
.course-score { font-weight: 600; color: #7C1F89; }

.btn-recommend-small {
  padding: 2px 8px; font-size: 11px; border: 1px solid #7C1F89; color: #7C1F89;
  background: white; border-radius: 4px; cursor: pointer; transition: all 0.2s;
}
.btn-recommend-small:hover { background: #fcf5ff; }

.waffle-cells { display: flex; flex-wrap: wrap; gap: 4px; }
.cell { width: 16px; height: 16px; border-radius: 3px; }
.cell.filled { background-color: #7C1F89; }
.cell.enrolled { background-color: #FFC107; } /* 黄色表示已选 */
.cell.empty { background-color: #e0e0e0; }
.no-req-text { font-size: 12px; color: #ccc; }

.enrolled-text { font-size: 16px; color: #FFC107; font-weight: bold; margin-left: 2px; }

/* 推荐课程列表样式 */
.recommendation-list {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px;
  animation: slideDown 0.3s ease;
}

.rec-course-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px; margin-bottom: 8px;
  border: 1px solid #eee; border-radius: 6px;
  transition: all 0.2s;
}
.rec-course-card:hover {
  border-color: #dcbadd; background: #fdf5ff;
}

.rec-course-name { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.rec-course-info { display: flex; gap: 6px; align-items: center; }
.rec-tag {
  font-size: 10px; padding: 1px 5px; border-radius: 3px;
  background: #f3e5f5; color: #7C1F89;
}
.rec-tag.gray { background: #f5f5f5; color: #666; }
.rec-teacher { font-size: 12px; color: #666; margin-left: 4px; }

.rec-course-action {
  display: flex; flex-direction: column; align-items: flex-end; gap: 4px;
  min-width: 80px;
}

.btn-enroll-mini {
  padding: 4px 12px; background: #7C1F89; color: white; border: none;
  border-radius: 4px; font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.btn-enroll-mini:hover:not(:disabled) { background: #9C27B0; }
.btn-enroll-mini:disabled { background: #ccc; cursor: not-allowed; }

.msg-toast { font-size: 10px; padding: 2px 4px; border-radius: 2px; }
.msg-toast.success { color: #52c41a; background: #f6ffed; }
.msg-toast.error { color: #ff4d4f; background: #fff2f0; }

.no-rec-text { font-size: 12px; color: #ccc; padding: 10px; text-align: center; }

/* 动画 */
.drawer-slide-enter-active, .drawer-slide-leave-active { transition: opacity 0.3s ease; }
.drawer-slide-enter-from, .drawer-slide-leave-to { opacity: 0; }
.drawer-slide-enter-active .drawer-container, .drawer-slide-leave-active .drawer-container { transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-slide-enter-from .drawer-container, .drawer-slide-leave-to .drawer-container { transform: translateX(100%); }
</style>