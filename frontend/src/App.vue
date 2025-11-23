<template>
  <div class="app-container">
    <h1>课程查询系统 - 测试前端</h1>
    
    <div class="student-info">
      <label for="studentId">学生ID:</label>
      <input 
        id="studentId"
        type="number" 
        v-model.number="studentId" 
        placeholder="请输入学生ID"
        min="1"
      />
      <span v-if="studentId" class="student-id-display">当前学生: {{ studentId }}</span>
    </div>
    
    <div class="tabs">
      <button 
        :class="['tab', { active: activeTab === 'course' }]"
        @click="activeTab = 'course'"
      >
        课程查询
      </button>
      <button 
        :class="['tab', { active: activeTab === 'session' }]"
        @click="activeTab = 'session'"
      >
        节次查询
      </button>
      <button 
        :class="['tab', { active: activeTab === 'myCourses' }]"
        @click="activeTab = 'myCourses'"
      >
        我的课程
      </button>
    </div>

    <div class="content">
      <CourseSearch v-if="activeTab === 'course'" />
      <SessionSearch v-if="activeTab === 'session'" />
      <MyCourses v-if="activeTab === 'myCourses'" />
    </div>
  </div>
</template>

<script>
import { ref, provide } from 'vue'
import CourseSearch from './components/CourseSearch.vue'
import SessionSearch from './components/SessionSearch.vue'
import MyCourses from './components/MyCourses.vue'

export default {
  name: 'App',
  components: {
    CourseSearch,
    SessionSearch,
    MyCourses
  },
  setup() {
    const activeTab = ref('course')
    const studentId = ref(null)
    
    // 使用 provide 向子组件提供 studentId
    provide('studentId', studentId)
    
    return {
      activeTab,
      studentId
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
  background-color: #f5f5f5;
  padding: 20px;
}

.app-container {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

h1 {
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #eee;
}

.tab {
  padding: 10px 20px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab:hover {
  color: #333;
}

.tab.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f0f7ff;
  border-radius: 4px;
  border: 1px solid #d4e8ff;
}

.student-info label {
  font-weight: bold;
  color: #333;
}

.student-info input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  width: 200px;
}

.student-info input:focus {
  outline: none;
  border-color: #1890ff;
}

.student-id-display {
  color: #1890ff;
  font-weight: bold;
  margin-left: 10px;
}

.content {
  min-height: 400px;
}
</style>

