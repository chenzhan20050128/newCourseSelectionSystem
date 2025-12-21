<template>
  <teleport to="body">
    <transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click="handleCancel">
        <div class="dialog-content" @click.stop>
          <div class="dialog-header" :class="type">
            <div class="header-center">
              <div class="header-icon" :class="type" aria-hidden="true">
                <span v-if="type === 'drop'">!</span>
                <span v-else>✓</span>
              </div>
              <h3 class="header-title">{{ title }}</h3>
            </div>
            <button class="close-btn" @click="handleCancel">✕</button>
          </div>
          
          <div class="dialog-body">
            <div class="course-info" v-if="course">
              <div class="course-name">{{ course.courseName }}</div>
              <div class="course-id">({{ paddedCourseId }})</div>
            </div>
            
            <p class="message">{{ message }}</p>

            <div v-if="capacityWarning" class="capacity-warning">
              <div class="warning-header">
                <span class="warning-icon" aria-hidden="true">!</span>
                <span class="warning-title">容量提醒</span>
              </div>
              <p class="warning-tip">{{ capacityWarning }}</p>
            </div>
            
            <div v-if="conflicts && conflicts.length > 0" class="conflict-warning">
              <div class="warning-header">
                <span class="warning-icon" aria-hidden="true">!</span>
                <span class="warning-title">检测到课程冲突</span>
              </div>
              <p class="warning-tip">该课程与以下课程存在冲突：</p>
              <ul class="conflict-list">
                <li v-for="(c, index) in conflicts" :key="index">
                  <span class="conflict-name">{{ c.courseName }}</span>
                  <span class="conflict-time">{{ c.timeStr }}</span>
                </li>
              </ul>
            </div>
          </div>
          
          <div class="dialog-footer">
            <button class="btn-cancel" @click="handleCancel">{{ cancelText }}</button>
            <button 
              :class="['btn-confirm', type]" 
              @click="handleConfirm"
              :disabled="loading"
            >
              {{ loading ? '处理中...' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'ConfirmDialog',
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '确认操作' },
    message: { type: String, default: '' },
    confirmText: { type: String, default: '确认' },
    cancelText: { type: String, default: '取消' },
    type: { type: String, default: 'enroll' }, // 'enroll' | 'drop'
    course: { type: Object, default: null },
    conflicts: { type: Array, default: () => [] },
    capacityWarning: { type: String, default: '' },
    loading: { type: Boolean, default: false }
  },
  emits: ['confirm', 'cancel'],
  setup(props, { emit }) {
    const handleConfirm = () => {
      emit('confirm')
    }
    
    const handleCancel = () => {
      emit('cancel')
    }
    
    const paddedCourseId = computed(() => {
      const raw = props.course?.courseId
      if (raw === null || raw === undefined) return ''
      return String(raw).padStart(8, '0')
    })

    return { handleConfirm, handleCancel, paddedCourseId }
  }
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1500;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.dialog-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: dialog-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.dialog-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.dialog-header.enroll {
  background: #f6ffed;
  border-bottom: 1px solid #b7eb8f;
}

.dialog-header.drop {
  background: #fff2f0;
  border-bottom: 1px solid #ffccc7;
}

.header-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #333;
  text-align: center;
}

.header-icon {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  line-height: 1;
}

.header-icon.enroll {
  background: #52c41a;
  color: #fff;
}

.header-icon.drop {
  background: #ff4d4f;
  color: #fff;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  line-height: 1;
}
.close-btn:hover { color: #666; }

.dialog-body {
  padding: 20px;
}

.course-info {
  margin-bottom: 12px;
  text-align: center;
}

.course-name {
  font-weight: 800;
  color: #333;
  font-size: 18px;
  line-height: 1.3;
}

.course-id {
  color: #999;
  font-weight: normal;
  font-size: 14px;
  margin-top: 4px;
}

.message {
  color: #666;
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.conflict-warning {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 8px;
  padding: 12px;
  margin-top: 16px;
}

.capacity-warning {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  padding: 12px;
  margin-top: 12px;
}

.warning-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-weight: 600;
}

.capacity-warning .warning-header {
  color: #faad14;
}

.conflict-warning .warning-header {
  color: #ff4d4f;
}

.warning-icon {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 12px;
  color: #fff;
}

.capacity-warning .warning-icon {
  background: #faad14;
}

.conflict-warning .warning-icon {
  background: #ff4d4f;
}

.conflict-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #555;
}

.conflict-list li {
  margin-bottom: 4px;
}

.conflict-time {
  color: #888;
  margin-left: 4px;
  font-size: 12px;
}

.warning-tip {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #666;
}

.dialog-footer {
  padding: 16px 20px;
  background: #f9f9f9;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}
.btn-cancel:hover {
  color: #40a9ff;
  border-color: #40a9ff;
}

.btn-confirm {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: white;
  font-weight: 600;
  transition: all 0.2s;
}
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-confirm.enroll {
  background: #52c41a;
}
.btn-confirm.enroll:hover:not(:disabled) {
  background: #52c41a;
}

.btn-confirm.drop {
  background: #ff4d4f;
}
.btn-confirm.drop:hover:not(:disabled) {
  background: #ff7875;
}

/* Animations */
.dialog-fade-enter-active, .dialog-fade-leave-active {
  transition: opacity 0.2s;
}
.dialog-fade-enter-from, .dialog-fade-leave-to {
  opacity: 0;
}

@keyframes dialog-pop {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
</style>
