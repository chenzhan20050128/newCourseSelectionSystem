<template>
  <div class="home-container">
    <div class="header">
      <div class="header-left">
        <img src="../assets/校标-白底.png" alt="校徽" class="school-logo">
        <h1>课程选课系统</h1>
      </div>
      
      <!-- 选课轮次信息 - 移到header中间 -->
      <div class="batch-info-header" v-if="selectedBatch">
        <div class="batch-summary" @click="toggleBatchDetails">
          <span class="batch-name">{{ selectedBatch.batchName }} - {{ selectedBatch.roundName }}</span>
          <span :class="['batch-status', statusClass]">{{ selectedBatch.status }}</span>
          <span class="dropdown-icon">{{ showBatchDetails ? '▲' : '▼' }}</span>
        </div>
        <button @click="showBatchSelector = true" class="change-batch-btn-header">切换</button>
        
        <!-- 下拉详细信息 -->
        <div class="batch-details-dropdown" v-show="showBatchDetails">
          <div class="detail-item">
            <span class="detail-label">⏰ 选课时间:</span>
            <span class="detail-value">{{ formatDateTime(selectedBatch.startTime) }} ~ {{ formatDateTime(selectedBatch.endTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">📝 选课方式:</span>
            <span class="detail-value">{{ selectedBatch.selectionMode }}</span>
          </div>
          <div class="detail-item" v-if="timeRemaining">
            <span class="detail-label">⏳ 剩余时间:</span>
            <span class="detail-value countdown">{{ timeRemaining }}</span>
          </div>
        </div>
      </div>
      
      <div class="user-info">
        <span class="welcome">欢迎, {{ user?.username }} {{ user?.userId }}</span>
        <button @click="handleLogout" class="logout-btn">退出登录</button>
      </div>
    </div>

    <!-- 轮次选择弹窗 -->
    <div class="modal-overlay" v-if="showBatchSelector" @click="showBatchSelector = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>选择选课轮次</h3>
          <button @click="showBatchSelector = false" class="close-btn">✕</button>
        </div>
        <div class="modal-body">
          <div 
            v-for="batch in availableBatches" 
            :key="batch.batchId"
            :class="['batch-option', { selected: selectedBatch?.batchId === batch.batchId }]"
            @click="selectBatch(batch)"
          >
            <div class="batch-option-header">
              <span class="batch-option-name">{{ batch.batchName }} - {{ batch.roundName }}</span>
              <span :class="['batch-option-status', getStatusClass(batch.status)]">{{ batch.status }}</span>
            </div>
            <div class="batch-option-time">{{ formatDateTime(batch.startTime) }} ~ {{ formatDateTime(batch.endTime) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="tabs">
      <button 
        :class="['tab', { active: activeTab === 'course' }]"
        @click="activeTab = 'course'"
      >
        课程查询
      </button>
      <button 
        v-if="user?.userType === 'student'"
        :class="['tab', { active: activeTab === 'myCourses' }]"
        @click="activeTab = 'myCourses'"
      >
        我的课程
      </button>
      <button 
        v-if="user?.userType === 'student'"
        :class="['tab', { active: activeTab === 'smartSelection' }]"
        @click="activeTab = 'smartSelection'"
      >
        智能选课
      </button>
    </div>

  <!-- 内容区 -->
    <div class="content">
      <!-- 这里不需要再监听 openDrawer 了，因为按钮就在本页面 -->
      <CourseSearch v-if="activeTab === 'course'" />
      <MyCourses v-if="activeTab === 'myCourses' && user?.userType === 'student'" />
      <SmartCourseSelection v-if="activeTab === 'smartSelection' && user?.userType === 'student'" />
    </div>

    <!-- [新增] 全局悬浮选课进度挂件 -->
    <!-- 放在最外层，确保不受 overflow 影响 -->
    <div class="floating-progress-widget" @click="showProgressDrawer = true">
      <div class="widget-content">
        <div class="widget-title">选课进度</div>
        <div class="widget-subtitle">点击查看</div>
      </div>
    </div>

    <!-- 抽屉组件 -->
    <CourseProgressDrawer 
      :visible="showProgressDrawer" 
      @close="showProgressDrawer = false" 
    />
  </div>
</template>

<script>
import { ref, provide, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import CourseSearch from '../components/CourseSearch.vue';
import MyCourses from '../components/MyCourses.vue';
import SmartCourseSelection from '../components/SmartCourseSelection.vue';
import CourseProgressDrawer from '../components/CourseProgressDrawer.vue';
import { logout } from '../api/authApi';
import { getAllBatches } from '../api/electiveBatchApi';

export default {
  name: 'Home',
  components: {
    CourseSearch,
    MyCourses,
    SmartCourseSelection,
    CourseProgressDrawer
  },
  setup() {
    const router = useRouter();
    const activeTab = ref('course');
    const user = ref(null);
    const selectedBatch = ref(null);
    const availableBatches = ref([]);
    const showBatchSelector = ref(false);
    const showBatchDetails = ref(false);
    const timeRemaining = ref('');
    let countdownTimer = null;
    const showProgressDrawer = ref(false);
    // 切换详细信息显示
    const toggleBatchDetails = () => {
      showBatchDetails.value = !showBatchDetails.value;
    };
    
    // 从 localStorage 获取用户信息和选课轮次
    onMounted(() => {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        user.value = JSON.parse(userStr);
      } else {
        // 如果没有用户信息，跳转到登录页
        router.push('/login');
        return;
      }

      // 获取选中的选课轮次
      const batchStr = localStorage.getItem('selectedBatch');
      if (batchStr) {
        selectedBatch.value = JSON.parse(batchStr);
        startCountdown();
      }

      // 加载所有可用轮次
      loadAvailableBatches();
    });

    // 清除定时器
    onBeforeUnmount(() => {
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }
    });
    
    const userTypeText = computed(() => {
      return user.value?.userType === 'student' ? '学生' : '教师';
    });

    // 轮次状态样式
    const statusClass = computed(() => {
      if (!selectedBatch.value) return '';
      return getStatusClass(selectedBatch.value.status);
    });

    const getStatusClass = (status) => {
      if (status === '进行中') return 'status-active';
      if (status === '未开始') return 'status-pending';
      return 'status-ended';
    };
    
    // 提供 studentId 给子组件
    const studentId = computed(() => user.value?.userId);
    provide('studentId', studentId);

    // 加载可用轮次
    const loadAvailableBatches = async () => {
      try {
        const response = await getAllBatches();
        availableBatches.value = response.data.filter(
          batch => batch.status === '进行中' || batch.status === '未开始'
        );
      } catch (error) {
        console.error('加载选课轮次失败:', error);
      }
    };

    // 选择轮次
    const selectBatch = (batch) => {
      selectedBatch.value = batch;
      localStorage.setItem('selectedBatch', JSON.stringify(batch));
      localStorage.setItem('selectedBatchId', batch.batchId);
      showBatchSelector.value = false;
      startCountdown();
    };

    // 格式化日期时间
    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '';
      const date = new Date(dateTimeStr);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    };

    // 开始倒计时
    const startCountdown = () => {
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }

      const updateCountdown = () => {
        if (!selectedBatch.value) {
          timeRemaining.value = '';
          return;
        }

        const now = new Date().getTime();
        const startTime = new Date(selectedBatch.value.startTime).getTime();
        const endTime = new Date(selectedBatch.value.endTime).getTime();

        // 如果还未开始，显示距离开始的时间
        if (now < startTime) {
          const diff = startTime - now;
          const days = Math.floor(diff / (1000 * 60 * 60 * 24));
          const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
          const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
          const seconds = Math.floor((diff % (1000 * 60)) / 1000);

          if (days > 0) {
            timeRemaining.value = `距离开始: ${days}天 ${hours}小时 ${minutes}分钟`;
          } else if (hours > 0) {
            timeRemaining.value = `距离开始: ${hours}小时 ${minutes}分钟 ${seconds}秒`;
          } else {
            timeRemaining.value = `距离开始: ${minutes}分钟 ${seconds}秒`;
          }
          return;
        }

        // 如果已经开始，显示距离结束的时间
        const diff = endTime - now;

        if (diff <= 0) {
          timeRemaining.value = '已结束';
          if (countdownTimer) {
            clearInterval(countdownTimer);
          }
          return;
        }

        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);

        if (days > 0) {
          timeRemaining.value = `${days}天 ${hours}小时 ${minutes}分钟`;
        } else if (hours > 0) {
          timeRemaining.value = `${hours}小时 ${minutes}分钟 ${seconds}秒`;
        } else {
          timeRemaining.value = `${minutes}分钟 ${seconds}秒`;
        }
      };

      updateCountdown();
      countdownTimer = setInterval(updateCountdown, 1000);
    };
    
    const handleLogout = async () => {
      try {
        await logout();
      } catch (error) {
        console.error('登出错误:', error);
      } finally {
        // 清除本地存储
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('selectedBatch');
        localStorage.removeItem('selectedBatchId');
        // 跳转到登录页
        router.push('/login');
      }
    };
    
    return {
      activeTab,
      user,
      userTypeText,
      selectedBatch,
      availableBatches,
      showBatchSelector,
      showBatchDetails,
      timeRemaining,
      statusClass,
      getStatusClass,
      selectBatch,
      formatDateTime,
      handleLogout,
      toggleBatchDetails,
      showProgressDrawer
    };
  }
};
</script>

<style scoped>
.home-container {
  max-width: 100%;
  margin: 0 auto;
  background: white;
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-height: calc(100vh - 20px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -10px -10px 15px -10px;
  padding: 10px 15px;
  background: linear-gradient(135deg, #7C1F89 0%, #5A1566 100%);
  border-radius: 8px 8px 0 0;
  position: relative;
  flex-wrap: wrap;
  gap: 15px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.school-logo {
  width: 35px;
  height: 35px;
  object-fit: contain;
}

.header h1 {
  color: white;
  font-size: 22px;
  margin: 0;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome {
  color: white;
  font-size: 14px;
}

.logout-btn {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 5px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 0;
}

.tab {
  padding: 10px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 15px;
  color: #666;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
  position: relative;
  bottom: -2px;
}

.tab:hover {
  color: #667eea;
}

.tab.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
}

.content {
  margin-top: 10px;
}

/* Header中的选课轮次信息样式 */
.batch-info-header {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  min-width: 300px;
}

.batch-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 15px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  flex: 1;
}

.batch-summary:hover {
  background: rgba(255, 255, 255, 0.25);
}

.batch-name {
  font-size: 15px;
  font-weight: 600;
  color: white;
}

.batch-status {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.status-active {
  background: #4caf50;
  color: white;
}

.status-pending {
  background: #ff9800;
  color: white;
}

.status-ended {
  background: #9e9e9e;
  color: white;
}

.dropdown-icon {
  color: white;
  font-size: 12px;
  margin-left: auto;
}

.change-batch-btn-header {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 5px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
  white-space: nowrap;
}

.change-batch-btn-header:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
}

.batch-details-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 10px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #333;
  margin-bottom: 8px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-label {
  font-weight: 600;
  color: #7C1F89;
}

.detail-value {
  color: #555;
}

.countdown {
  font-weight: 600;
  color: #4caf50;
}

/* 弹窗样式 */
.modal-overlay {
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
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.batch-option {
  padding: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.batch-option:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.batch-option.selected {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
}

.batch-option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.batch-option-name {
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.batch-option-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.batch-option-time {
  color: #666;
  font-size: 14px;
}

/* [修改] 悬浮挂件样式 - 精致、现代、显眼 */
.floating-progress-widget {
  position: fixed;
  top: 220px;
  right: 15px; /* 稍微留一点边距，不要死贴边 */
  z-index: 999;
  
  /* 容器尺寸和背景 */
  background: white;
  padding: 16px 12px;
  border-radius: 50px; /* 变成两头圆的胶囊形状 */
  
  /* 关键：用投影代替粗边框，更高级 */
  box-shadow: 0 8px 24px rgba(124, 31, 137, 0.2); 
  /* 留一个极细的边框增加锐度 */
  border: 1px solid rgba(124, 31, 137, 0.1);
  
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); /* 弹性过渡 */
  
  /* 布局 */
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 鼠标悬停动画：整体上浮 + 变色 */
.floating-progress-widget:hover {
  transform: translateY(-6px); /* 明显上浮 */
  box-shadow: 0 15px 35px rgba(124, 31, 137, 0.35); /* 投影加深扩散 */
  background: #7C1F89; /* [关键改变] 悬停时背景变紫 */
  border-color: #7C1F89;
}

/* 内部内容容器 */
.widget-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

/* 图标：用 emoji 或者 svg */
.widget-icon {
  font-size: 24px;
  line-height: 1;
  margin-bottom: 4px;
  transition: all 0.3s;
}

/* 标题：竖排 */
.widget-title {
  font-size: 16px;
  font-weight: 700;
  color: #7C1F89; /* 默认紫色字 */
  writing-mode: vertical-lr; /* 竖排 */
  letter-spacing: 4px; /* 字间距 */
  font-family: sans-serif;
  transition: all 0.3s;
}

/* [关键交互] 悬停时文字和图标变白 */
.floating-progress-widget:hover .widget-title,
.floating-progress-widget:hover .widget-icon {
  color: white;
}

/* 删掉之前的 subtitle 相关样式，完全不需要它 */
.widget-subtitle {
  display: none;
}
</style>
