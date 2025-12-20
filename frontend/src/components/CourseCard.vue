<template>
  <div class="course-card-row">
    <!-- 1. 课程基本信息 -->
    <div class="col col-info">
      <!-- 修改结构：使用 block 容器包裹 inline 元素，实现自然换行 -->
      <div class="title-container">
        <span class="course-name" :title="course.courseName">{{ course.courseName }}</span>
        <!-- 学分角标：inline-block 保持整体性 -->
        <span class="credit-tag">{{ course.credits }}分</span>
      </div>
      
      <div class="course-meta">
        <span class="meta-id">{{ String(course.courseId).padStart(8, '0') }}</span>
        <span class="divider">|</span>
        <span class="meta-college">{{ course.college }}</span>
      </div>
    </div>

    <!-- 2. 教师 -->
    <div class="col col-instructor" :title="course.instructorName">{{ course.instructorName }}</div>
    
    <!-- 3. 时间与地点 -->
    <div class="col col-schedule">
      <div v-if="course.sessions && course.sessions.length > 0">
        <div v-for="session in course.sessions" :key="session.sessionId" class="schedule-line">
          <span class="time-badge">{{ session.weekday }}{{ session.startPeriod }}-{{ session.endPeriod }}</span>
          <span class="weeks-text">({{ course.startWeek }}-{{ course.endWeek }}周)</span>
        </div>
      </div>
      <div v-else class="text-gray">-</div>
      
      <div class="location-line">
        {{ course.campus }} {{ course.classroom }}
      </div>
    </div>
    
    <!-- 4. 容量 -->
    <div class="col col-capacity">
      <div class="capacity-info">
        <span :class="{ 'text-red': isFull }">{{ course.enrolledCount || 0 }}</span>
        <span class="text-gray"> / {{ course.capacity }}</span>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: capacityPercent + '%', backgroundColor: capacityColor }"></div>
      </div>
    </div>

    <!-- 5. 操作 -->
    <div class="col col-actions">
      <transition name="fade">
        <div v-if="message" :class="['message-toast', message.type]">
          {{ message.message }}
        </div>
      </transition>
      <button class="btn btn-action btn-enroll" v-if="!isEnrolled" @click="handleEnrollClick" :disabled="!canEnroll">
        {{ isEnrolling ? '...' : '选课' }}
      </button>
      <button class="btn btn-action btn-drop" v-else @click="handleDropClick" :disabled="!canDrop">
        {{ isDropping ? '...' : '退课' }}
      </button>
    </div>
  </div>

  <ConfirmDialog
    :visible="showDialog"
    :title="dialogTitle"
    :message="dialogMessage"
    :confirm-text="dialogConfirmText"
    :type="dialogType"
    :course="course"
    :conflicts="conflictList"
    :capacity-warning="capacityWarning"
    :loading="isEnrolling || isDropping"
    @confirm="handleConfirm"
    @cancel="showDialog = false"
  />
</template>

<script>
import { computed, ref } from 'vue'
import ConfirmDialog from './ConfirmDialog.vue'
import { checkTimeConflict } from '../utils/conflictChecker'

export default {
  name: 'CourseCard',
  components: { ConfirmDialog },
  props: {
    course: { type: Object, required: true },
    studentId: { type: [String, Number], default: null },
    isEnrolled: { type: Boolean, default: false },
    isEnrolling: { type: Boolean, default: false },
    isDropping: { type: Boolean, default: false },
    message: { type: Object, default: null },
    enrolledCourses: { type: Array, default: () => [] }
  },
  emits: ['enroll', 'drop'],
  setup(props, { emit }) {
    // 逻辑保持完全一致
    const isFull = computed(() => (props.course.enrolledCount || 0) >= props.course.capacity)
    const capacityPercent = computed(() => {
      const count = props.course.enrolledCount || 0
      const cap = props.course.capacity || 1
      return Math.min((count / cap) * 100, 100)
    })
    const capacityColor = computed(() => {
      if (capacityPercent.value >= 100) return '#ff4d4f'
      if (capacityPercent.value >= 80) return '#faad14'
      return '#52c41a' // 绿色
    })
    // 取消“满员不可点”限制：满员/超容量仍可发起选课（在弹窗里提示抽取难度）
    const canEnroll = computed(() => props.studentId && !props.isEnrolling)
    const canDrop = computed(() => props.studentId && !props.isDropping)
    
    // 弹窗状态
    const showDialog = ref(false)
    const dialogType = ref('enroll') // 'enroll' | 'drop'
    const conflictList = ref([])

    const capacityWarning = computed(() => {
      if (dialogType.value !== 'enroll') return ''
      const enrolled = Number(props.course.enrolledCount || 0)
      const cap = Number(props.course.capacity || 0)
      if (cap > 0 && enrolled >= cap) {
        return '该课程选课人数已超过容量，抽签难度较大，请谨慎选择'
      }
      return ''
    })

    const dialogTitle = computed(() => dialogType.value === 'enroll' ? '确认选课' : '确认退课')
    const dialogMessage = computed(() => {
      if (dialogType.value === 'enroll') {
        if (conflictList.value.length > 0) {
          return `该课程与${conflictList.value[0].courseName}存在冲突。仍要继续选课吗？`
        }
        return `确定要选择课程“${props.course.courseName}”吗？`
      } else {
        return `确定要退选课程“${props.course.courseName}”吗？退课后可能无法重新选择。`
      }
    })
    // 确认按钮仅保留“选课/退课”两字
    const dialogConfirmText = computed(() => dialogType.value === 'enroll' ? '选课' : '退课')

    const handleEnrollClick = () => {
      dialogType.value = 'enroll'
      // 检查冲突
      conflictList.value = checkTimeConflict(props.course, props.enrolledCourses)
      showDialog.value = true
    }

    const handleDropClick = () => {
      dialogType.value = 'drop'
      conflictList.value = [] // 退课不需要检查冲突
      showDialog.value = true
    }

    const handleConfirm = () => {
      showDialog.value = false
      if (dialogType.value === 'enroll') {
        emit('enroll', props.course)
      } else {
        emit('drop', props.course)
      }
    }
    
    return { 
      isFull, capacityPercent, capacityColor, canEnroll, canDrop,
      showDialog, dialogType, conflictList, dialogTitle, dialogMessage, dialogConfirmText,
      capacityWarning,
      handleEnrollClick, handleDropClick, handleConfirm
    }
  }
}
</script>

<style scoped>
.course-card-row {
  display: flex;
  align-items: center; /* 保持垂直居中，虽然长文本会让行高变高，但居中看起来比较整齐 */
  border-bottom: 1px solid #f0e6f5;
  padding: 16px 16px;
  transition: all 0.2s;
  background: #fff;
}

.course-card-row:hover {
  background-color: #FDF5FF !important;
  transform: scale(1.002);
  box-shadow: 0 2px 12px rgba(124, 31, 137, 0.08);
  z-index: 1;
  position: relative;
  border-radius: 4px;
}

/* --- 列宽定义 (与 CourseSearch 必须一致) --- */
.col { padding: 0 12px; overflow: hidden; box-sizing: border-box; }
.col-info      { flex: 2.5; min-width: 200px; }
.col-instructor{ flex: 1;   min-width: 100px; color: #333; font-size: 14px; }
.col-schedule  { flex: 1.8; min-width: 180px; font-size: 13px; }
.col-capacity  { flex: 1.2; min-width: 120px; }
.col-actions   { flex: 0 0 100px; display: flex; justify-content: center; position: relative; }

/* 1. 课程信息样式优化 (解决长文本问题) */
.title-container {
  display: block; /* 改为 block 容器，允许内部 inline 元素自然换行 */
  margin-bottom: 6px;
  line-height: 1.5; /* 增加行高，防止两行文字挤在一起 */
}

.course-name {
  font-size: 15px; /* 稍微调小一点点字号以容纳更多文字 */
  font-weight: 700;
  color: #333;
  margin-right: 8px;
  /* 移除 flex 相关的属性，让它回归纯文本流 */
}

.credit-tag {
  display: inline-block; /* 保持块状特性（padding等），但像文本一样流动 */
  font-size: 12px;
  color: #7C1F89;
  background: #F3E5F5;
  padding: 0px 6px; /* 稍微减小高度 */
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
  border: 1px solid #E1BEE7;
  vertical-align: 1px; /* 微调垂直对齐 */
}

.course-meta {
  font-size: 12px;
  color: #888;
  display: flex;
  align-items: center;
  flex-wrap: wrap; /* 如果学院名太长也允许换行 */
}
.meta-id { font-family: 'Roboto Mono', monospace; color: #999; }
.divider { margin: 0 6px; color: #ddd; }
.meta-college { color: #666; }

/* 其他样式保持微调 */
.schedule-line { margin-bottom: 3px; display: flex; align-items: center; flex-wrap: wrap;}
.time-badge { font-weight: 500; color: #333; margin-right: 4px;}
.weeks-text { color: #999; font-size: 12px; white-space: nowrap; }
.location-line { color: #666; font-size: 12px; margin-top: 2px; display: flex; align-items: center; }
.icon { margin-right: 4px; color: #7C1F89; flex-shrink: 0; }
.text-gray { color: #ccc; }

.capacity-info { font-size: 13px; margin-bottom: 4px; text-align: left; }
.text-red { color: #ff4d4f; font-weight: bold; }
.progress-track { height: 6px; background: #f0f0f0; border-radius: 3px; width: 90%; overflow: hidden; }
.progress-fill { height: 100%; transition: width 0.3s; }

.btn-action { padding: 6px 16px; border-radius: 4px; font-size: 13px; cursor: pointer; border: 1px solid transparent; transition: all 0.2s; white-space: nowrap; }
.btn-enroll { background: #7C1F89; color: white; }
.btn-enroll:hover:not(:disabled) { background: #9027a0; box-shadow: 0 2px 8px rgba(124, 31, 137, 0.3); }
.btn-enroll:disabled { background: #dcdfe6; cursor: not-allowed; }
.btn-drop { background: #ff4d4f; border-color: #ff4d4f; color: #fff; }
.btn-drop:hover:not(:disabled) { background: #ff7875; border-color: #ff7875; }

.message-toast { position: absolute; top: -30px; right: 0; padding: 4px 8px; border-radius: 4px; font-size: 12px; white-space: nowrap; box-shadow: 0 2px 8px rgba(0,0,0,0.1); background: #fff; z-index: 10; }
.message-toast.success { color: #52c41a; border: 1px solid #b7eb8f; background: #f6ffed; }
.message-toast.error { color: #ff4d4f; border: 1px solid #ffccc7; background: #fff2f0; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>