<template>
  <div class="app-container">
    <h1>课程查询系统 - 测试前端</h1>
    
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
    </div>

    <div class="content">
      <CourseSearch v-if="activeTab === 'course'" />
      <SessionSearch v-if="activeTab === 'session'" />
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import CourseSearch from './components/CourseSearch.vue'
import SessionSearch from './components/SessionSearch.vue'

export default {
  name: 'App',
  components: {
    CourseSearch,
    SessionSearch
  },
  setup() {
    const activeTab = ref('course')
    
    return {
      activeTab
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

.content {
  min-height: 400px;
}
</style>

