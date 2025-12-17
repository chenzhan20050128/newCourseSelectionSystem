<template>
  <div class="course-card-row">
    <div class="col col-id" title="课程号">{{ String(course.courseId).padStart(8, '0') }}</div>
    <div class="col col-name" :title="course.courseName">{{ course.courseName }}</div>
    <div class="col col-credit" title="学分">{{ course.credits }}</div>
    <div class="col col-instructor" :title="course.instructorName">{{ course.instructorName }}</div>
    
    <div class="col col-time">
      <div v-if="course.sessions && course.sessions.length > 0" class="sessions-list">
        <div v-for="session in course.sessions" :key="session.sessionId" class="session-item">
          {{ session.weekday }}{{ session.startPeriod }}-{{ session.endPeriod }}
        </div>
      </div>
      <div v-else class="no-time">-</div>
    </div>

    <div class="col col-weeks">{{ course.startWeek }}-{{ course.endWeek }}周</div>
    <div class="col col-location" :title="course.classroom">{{ course.classroom }}</div>
    <div class="col col-campus" :title="course.campus">{{ course.campus }}</div>
    <div class="col col-college" :title="course.college">{{ course.college }}</div>
    
    <div class="col col-capacity">
      <div class="capacity-text">
        <span :class="{ full: isFull }">{{ course.enrolledCount || 0 }}</span>/{{ course.capacity }}
      </div>
      <div class="capacity-bar">
        <div class="capacity-fill" :style="{ width: capacityPercent + '%', backgroundColor: capacityColor }"></div>
      </div>
    </div>

    <div class="col col-actions">
      <div v-if="message" :class="['message-toast', message.type]">
        {{ message.message }}
      </div>
      <div class="action-buttons">
        <button
          v-if="!course.isEnrolled"
          class="btn btn-enroll"
          @click="$emit('enroll', course)"
          :disabled="!canEnroll"
          :title="isEnrolling ? '选课中' : '选课'"
        >
          {{ isEnrolling ? '...' : '选课' }}
        </button>
        <button
          v-else
          class="btn btn-drop"
          @click="$emit('drop', course)"
          :disabled="!canDrop"
          :title="isDropping ? '退课中' : '退课'"
        >
          {{ isDropping ? '...' : '退课' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'CourseCard',
  props: {
    course: {
      type: Object,
      required: true
    },
    studentId: {
      type: [String, Number],
      default: null
    },
    isEnrolling: {
      type: Boolean,
      default: false
    },
    isDropping: {
      type: Boolean,
      default: false
    },
    message: {
      type: Object,
      default: null
    }
  },
  emits: ['enroll', 'drop'],
  setup(props) {
    const isFull = computed(() => {
      return (props.course.enrolledCount || 0) >= props.course.capacity
    })

    const capacityPercent = computed(() => {
      const count = props.course.enrolledCount || 0
      const cap = props.course.capacity || 1
      return Math.min((count / cap) * 100, 100)
    })

    const capacityColor = computed(() => {
      if (capacityPercent.value >= 100) return '#ff4d4f'
      if (capacityPercent.value >= 80) return '#faad14'
      return '#52c41a'
    })

    const canEnroll = computed(() => {
      return props.studentId && !props.course?.isEnrolled && !props.isEnrolling && !isFull.value
    })

    const canDrop = computed(() => {
      return props.studentId && !!props.course?.isEnrolled && !props.isDropping
    })

    return {
      isFull,
      capacityPercent,
      capacityColor,
      canEnroll,
      canDrop
    }
  }
}
</script>

<style scoped>
.course-card-row {
  display: flex;
  align-items: center;
  /* background 由父组件控制斑马纹 */
  border-bottom: 1px solid #f0f0f0;
  padding: 12px 5px; /* 稍微增加一点高度让大字体不拥挤 */
  gap: 8px;
  transition: background-color 0.2s;
  font-size: 13px;
  color: #333;
}

.course-card-row:hover {
  filter: brightness(0.98); /* hover 效果改为变暗一点点，适应不同背景色 */
}

.col {
  overflow: visible;
  white-space: normal;
  padding: 0 4px;
  word-break: break-word;
}

/* Column Widths - Must match header in parent component */
.col-id { flex: 0 0 80px; color: #888; }
.col-name { flex: 1 1 140px; font-weight: 600; color: #1f1f1f; font-size: 15px; }
.col-credit { flex: 0 0 40px; text-align: left; padding-left: 8px; }
.col-instructor { flex: 0 0 150px; }
.col-time { flex: 1 1 100px; font-size: 12px; }
.col-weeks { flex: 0 0 60px; text-align: center; }
.col-location { flex: 0 0 180px; }
.col-campus { flex: 0 0 70px; }
.col-college { flex: 0 0 150px; }
.col-capacity { flex: 0 0 70px; text-align: center; }
.col-actions { flex: 0 0 100px; display: flex; justify-content: center; position: relative; }

.sessions-list {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 6px;
}

.session-item {
  line-height: 1.2;
}

.no-time {
  color: #ccc;
}

/* Capacity Bar */
.capacity-bar-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.capacity-text {
  font-size: 12px;
  margin-bottom: 2px;
  line-height: 1;
}

.capacity-text .full {
  color: #ff4d4f;
  font-weight: bold;
}

.capacity-bar {
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
  width: 100%;
}

.capacity-fill {
  height: 100%;
  transition: width 0.3s ease, background-color 0.3s ease;
}

/* Buttons */
.action-buttons {
  display: flex;
  gap: 6px;
}

.btn {
  border: none;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
  font-weight: 500;
  min-width: 40px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-enroll {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
.btn-enroll:hover:not(:disabled) {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.btn-drop {
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffa39e;
}
.btn-drop:hover:not(:disabled) {
  background: #ff4d4f;
  color: white;
  border-color: #ff4d4f;
}

.message-toast {
  position: absolute;
  top: -25px;
  right: 0;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  animation: fadeIn 0.3s;
  z-index: 10;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.message-toast.success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.message-toast.error { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.message-toast.warning { background: #fffbe6; color: #faad14; border: 1px solid #ffe58f; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
