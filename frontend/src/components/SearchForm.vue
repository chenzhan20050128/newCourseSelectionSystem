<template>
  <form @submit.prevent="handleSearch" class="search-card">
    
    <!-- 第一行：核心搜索区 -->
    <div class="primary-search-area">
      <div class="input-wrapper main-input">
        <input 
          v-model="keyword" 
          type="text" 
          placeholder="请输入课程号 或 课程名进行搜索..." 
          class="hero-input" 
        />
      </div>
      <button type="submit" :disabled="loading" class="btn-search-hero">
        {{ loading ? '查询中...' : '查询' }}
      </button>
    </div>

    <!-- 第二行：辅助筛选区 -->
    <div class="filter-bar">
      <div class="filter-cell">
        <AutocompleteInput
          v-model="query.instructorName"
          placeholder="教师姓名"
          :fetch-suggestions="createFetch('instructorName')"
          class="full-width"
        />
      </div>

      <div class="filter-cell">
        <AutocompleteInput
          v-model="query.college"
          placeholder="开课学院"
          :fetch-suggestions="createFetch('college')"
          class="full-width"
        />
      </div>

      <div class="filter-cell">
        <AutocompleteInput
          v-model="query.campus"
          placeholder="校区"
          :fetch-suggestions="createFetch('campus')"
          class="full-width"
        />
      </div>

      <div class="filter-cell short">
        <AutocompleteInput
          v-model="query.credits"
          placeholder="学分"
          :fetch-suggestions="createFetch('credits')"
          class="full-width"
        />
      </div>

      <div class="time-group">
        <select v-model="query.weekday" class="std-select no-border">
          <option value="">礼拜</option>
          <option v-for="d in ['周一','周二','周三','周四','周五','周六','周日']" :key="d" :value="d">{{d}}</option>
        </select>
        <span class="divider">|</span>
        <input v-model.number="query.startPeriod" type="number" min="1" max="12" class="std-input tiny-input no-border" />
        <span class="sep">-</span>
        <input v-model.number="query.endPeriod" type="number" min="1" max="12" class="std-input tiny-input no-border" />
        <span class="unit">节</span>
      </div>

      <button type="button" @click="handleReset" class="btn-reset-text">
        重置筛选
      </button>
    </div>

    <!-- [新增] 第三部分：课程类型筛选 -->
    <div class="category-filter-section">
      
      <!-- 一级标签 -->
      <div class="level-1-tabs">
        <div 
          v-for="group in level1Options" 
          :key="group"
          class="tab-l1-item"
          :class="{ active: activeLevel1 === group }"
          @click="handleLevel1Change(group)"
        >
          {{ group }}
        </div>
      </div>

      <!-- 二级标签 (仅当一级不为'全部'时显示) -->
      <transition name="fade">
        <div class="level-2-tabs" v-if="activeLevel1 !== '全部'">
          <div 
            v-for="subType in categoryGroups[activeLevel1]" 
            :key="subType"
            class="tab-l2-pill"
            :class="{ active: query.type === subType }"
            @click="handleLevel2Change(subType)"
          >
            {{ subType }}
          </div>
        </div>
      </transition>
      
    </div>

  </form>
</template>

<script>
import { reactive, ref, onMounted, computed } from 'vue'
import AutocompleteInput from './AutocompleteInput.vue'
import { getAttributeValues } from '../api/courseApi'

export default {
  name: 'SearchForm',
  components: { AutocompleteInput },
  props: {
    loading: { type: Boolean, default: false }
  },
  emits: ['search', 'reset'],
  setup(props, { emit }) {
    const keyword = ref('')
    const activeLevel1 = ref('全部') // 当前激活的一级分类

    // 1. 初始化 query，确保 type 字段存在且为空字符串
    const query = reactive({
      courseId: null, 
      courseName: '', 
      type: '',      // 关键：用于存储二级标签内容 (如 '大学数学')
      credits: null, 
      college: '', 
      instructorName: '', 
      campus: '', 
      weekday: '', 
      startPeriod: null, 
      endPeriod: null
    })

    // 2. 映射表数据
    const CATEGORY_MAP = {
      '大学数学': '必修课程', '大学英语': '必修课程', '思政': '必修课程',
      '体育': '必修课程', '军事课': '必修课程', '军理课': '必修课程',
      '学科基础课程': '专业课程', '专业选修': '专业课程', '毕业论文': '专业课程',
      '通识课': '通识教育','科光': '通识教育', '悦读': '通识教育',
      '美育': '通识教育', 
    }

    const level1Options = ['全部', '必修课程', '专业课程', '通识教育']

    // 3. 计算属性：将映射表反转为 { '必修课程': ['大学数学', ...], ... }
    const categoryGroups = computed(() => {
      const groups = {
        '必修课程': [],
        '专业课程': [],
        '通识教育': []
      }
      Object.keys(CATEGORY_MAP).forEach(subType => {
        const parentType = CATEGORY_MAP[subType]
        if (groups[parentType]) {
          // 避免重复添加
          if (!groups[parentType].includes(subType)) {
            groups[parentType].push(subType)
          }
        }
      })
      return groups
    })

    // 4. 处理一级标签点击：默认选中该分类下的第一个二级标签
    const handleLevel1Change = (group) => {
      activeLevel1.value = group
      
      if (group === '全部') {
        query.type = ''
      } else {
        const subList = categoryGroups.value[group]
        // 如果该分类下有子标签，默认选中第一个
        if (subList && subList.length > 0) {
          query.type = subList[0]
        } else {
          query.type = ''
        }
      }
      handleSearch() // 立即查询
    }

    // 5. 处理二级标签点击
    const handleLevel2Change = (subType) => {
      query.type = subType
      handleSearch() // 立即查询
    }

    const buildCondition = (excludeField = null) => {
      const condition = {}
      Object.keys(query).forEach(key => {
        if (key !== excludeField) {
          const value = query[key]
          if (value !== null && value !== '' && value !== undefined) {
            condition[key] = value
          }
        }
      })
      return condition
    }

    const createFetch = (fieldName) => {
      return async (queryString) => {
        try {
          const condition = buildCondition(fieldName)
          if (queryString) condition[fieldName] = queryString
          const values = await getAttributeValues({ condition: condition, attributeName: fieldName })
          return values || []
        } catch (err) {
          console.error(`Fetch ${fieldName} error:`, err)
          return []
        }
      }
    }

    const handleSearch = () => {
      query.courseId = null
      query.courseName = ''

      if (keyword.value) {
        const trimmed = keyword.value.trim()
        if (/^\d+$/.test(trimmed)) {
          query.courseId = trimmed
        } else {
          query.courseName = trimmed
        }
      }

      // 确保 type 被包含在发送的对象中
      emit('search', { ...query })
    }

    const handleReset = () => {
      keyword.value = ''
      
      // 重置分类状态
      activeLevel1.value = '全部'
      query.type = ''

      Object.keys(query).forEach(key => {
        // type 已经在上面重置了，这里跳过
        if (key === 'type') return

        if (['courseId', 'credits', 'startPeriod', 'endPeriod'].includes(key)) {
          query[key] = null
          return
        }
        query[key] = ''
      })
      emit('reset')
    }

    onMounted(() => {
      handleSearch()
    })

    return { 
      keyword, 
      query, 
      createFetch, 
      handleSearch, 
      handleReset,
      // 导出给模板使用
      activeLevel1,
      level1Options,
      categoryGroups,
      handleLevel1Change,
      handleLevel2Change
    }
  }
}
</script>

<style scoped>
/* 原有基础样式 */
.search-card {
  background: white;
  padding: 24px 30px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(124, 31, 137, 0.06);
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.primary-search-area { display: flex; gap: 16px; align-items: center; }
.main-input { flex: 1; position: relative; display: flex; align-items: center; }
.hero-input {
  width: 100%; height: 52px; padding: 0 20px; border: 2px solid #ebeef5; border-radius: 10px; font-size: 16px; background: #fdfdfd; transition: all 0.2s; color: #333;
}
.hero-input:focus { outline: none; border-color: #7C1F89; background: white; box-shadow: 0 0 0 4px rgba(124, 31, 137, 0.1); }
.hero-input::placeholder { color: #a8abb2; font-weight: 400; }
.btn-search-hero {
  height: 52px; padding: 0 40px; background: #7C1F89; color: white; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 6px 16px rgba(124, 31, 137, 0.25); white-space: nowrap;
}
.btn-search-hero:hover:not(:disabled) { background: #9027a0; transform: translateY(-2px); box-shadow: 0 8px 20px rgba(124, 31, 137, 0.35); }
.filter-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 16px; padding-top: 4px; }
.filter-cell { width: 150px; flex-shrink: 0; }
.filter-cell.short { width: 100px; }
.full-width { width: 100%; }

/* 输入框统一样式 */
.std-input, 
.std-select,
:deep(.autocomplete-container) {
  height: 36px !important;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  font-size: 14px;
  padding: 0 10px;
  background: #fff;
  box-sizing: border-box;
  transition: all 0.2s;
  color: #333;
  width: 100%;
}

:deep(.autocomplete-input) { 
  border: none !important; 
  outline: none !important;
  height: 100% !important; 
  width: 100% !important;
  background: transparent !important; 
  padding: 0 !important; 
  margin: 0 !important;
  box-shadow: none !important; 
  border-radius: 0 !important;
}

.std-input:focus, 
.std-select:focus,
:deep(.autocomplete-container):focus-within {
  outline: none;
  border-color: #7C1F89;
  box-shadow: 0 0 0 2px rgba(124, 31, 137, 0.1);
  background: #fff;
}

.time-group { background: #f7f8fa; border-radius: 8px; padding: 0 10px; height: 36px; display: flex; align-items: center; gap: 4px; border: 1px solid #e0e0e0; }
.no-border { border: none !important; background: transparent !important; box-shadow: none !important; padding: 0 4px; }
.tiny-input { width: 36px !important; text-align: center; color: #333; display: flex; align-items: center; justify-content: center; }
.divider { color: #ddd; margin: 0 6px; }
.sep { color: #a8abb2; margin: 0 4px; font-size: 14px; transform: translateY(-1px); display: inline-block; }
.unit { font-size: 13px; color: #888; margin-left: 2px; }
.btn-reset-text { background: none; border: none; color: #909399; font-size: 14px; cursor: pointer; margin-left: auto; padding: 6px 12px; border-radius: 6px; transition: all 0.2s; }
.btn-reset-text:hover { color: #7C1F89; background: #f9f0ff; }

/* 课程分类筛选样式 */
.category-filter-section {
  border-top: 1px dashed #eee;
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.level-1-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.tab-l1-item {
  font-size: 15px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  padding: 4px 0;
  position: relative;
  transition: all 0.2s;
}
.tab-l1-item:hover { color: #7C1F89; }
.tab-l1-item.active { color: #7C1F89; font-weight: 700; }
.tab-l1-item.active::after {
  content: ''; position: absolute; bottom: -9px; left: 0; width: 100%; height: 3px; background: #7C1F89; border-radius: 2px 2px 0 0;
}

.level-2-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 4px 0;
}

.tab-l2-pill {
  font-size: 13px;
  color: #666;
  background: #f4f4f5;
  padding: 6px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.tab-l2-pill:hover { color: #7C1F89; background: #f8f0fc; }
.tab-l2-pill.active { background: #7C1F89; color: white; box-shadow: 0 2px 6px rgba(124, 31, 137, 0.2); }

/* 动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 1000px) { .filter-cell, .filter-cell.short { width: auto; flex: 1 1 120px; } }
@media (max-width: 768px) { .primary-search-area { flex-direction: column; } .btn-search-hero { width: 100%; } .filter-bar { gap: 10px; } .filter-cell { flex: 1 1 45%; max-width: 48%; } .time-group { flex: 1 1 100%; max-width: 100%; order: 10; } .btn-reset-text { width: 100%; text-align: center; order: 11; margin-top: 8px;} }
</style>