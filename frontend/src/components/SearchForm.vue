<template>
  <form @submit.prevent="handleSearch" class="search-form">
    <div class="search-container">
      <!-- 第一行：课程与开课信息 -->
      <div class="search-row">
        <div class="search-item">
          <div class="search-label">课程信息:</div>
          <div class="search-inputs">
            <input v-model.number="query.courseId" type="number" placeholder="课程号" class="std-input w-80" />
            <AutocompleteInput
              v-model="query.courseName"
              placeholder="课程名"
              :fetch-suggestions="createFetch('courseName')"
              class="w-120"
            />
            <AutocompleteInput
              v-model="query.credits"
              placeholder="学分"
              :fetch-suggestions="createFetch('credits')"
              class="w-60"
            />
          </div>
        </div>

        <div class="search-item">
          <div class="search-label">开课单位:</div>
          <div class="search-inputs">
            <input v-model.number="query.instructorId" type="number" placeholder="教师ID" class="std-input w-80" />
            <AutocompleteInput
              v-model="query.college"
              placeholder="学院"
              :fetch-suggestions="createFetch('college')"
              class="w-120"
            />
          </div>
        </div>
      </div>

      <!-- 第二行：地点与时间 -->
      <div class="search-row">
        <div class="search-item">
          <div class="search-label">上课地点:</div>
          <div class="search-inputs">
            <AutocompleteInput
              v-model="query.campus"
              placeholder="校区"
              :fetch-suggestions="createFetch('campus')"
              class="w-100"
            />
            <AutocompleteInput
              v-model="query.classroom"
              placeholder="教室"
              :fetch-suggestions="createFetch('classroom')"
              class="w-100"
            />
          </div>
        </div>

        <div class="search-item">
          <div class="search-label">上课时间:</div>
          <div class="search-inputs">
            <div class="range-group">
              <input v-model.number="query.startWeek" type="number" placeholder="始" class="std-input w-40 center-text" />
              <span class="separator">-</span>
              <input v-model.number="query.endWeek" type="number" placeholder="终" class="std-input w-40 center-text" />
            </div>
            <select v-model="query.weekday" class="std-select w-70">
              <option value="">周几</option>
              <option value="周一">周一</option>
              <option value="周二">周二</option>
              <option value="周三">周三</option>
              <option value="周四">周四</option>
              <option value="周五">周五</option>
              <option value="周六">周六</option>
              <option value="周日">周日</option>
            </select>
            
            <span class="inner-label">节次:</span>
            <div class="range-group">
              <input v-model.number="query.startPeriod" type="number" min="1" max="12" placeholder="始" class="std-input w-40 center-text" />
              <span class="separator">-</span>
              <input v-model.number="query.endPeriod" type="number" min="1" max="12" placeholder="终" class="std-input w-40 center-text" />
            </div>
          </div>
        </div>

        <div class="search-actions">
          <button type="submit" :disabled="loading" class="btn-search">查询</button>
          <button type="button" @click="handleReset" class="btn-reset">重置</button>
        </div>
      </div>
    </div>
  </form>
</template>

<script>
import { reactive, ref, onMounted } from 'vue'
import AutocompleteInput from './AutocompleteInput.vue'
import { getAttributeValues } from '../api/courseApi'

export default {
  name: 'SearchForm',
  components: {
    AutocompleteInput
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['search', 'reset'],
  setup(props, { emit }) {
    const query = reactive({
      courseId: null,
      courseName: '',
      credits: null,
      description: '',
      college: '',
      instructorId: null,
      campus: '',
      classroom: '',
      startWeek: null,
      endWeek: null,
      weekday: '',
      startPeriod: null,
      endPeriod: null
    })

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
          // If queryString is present, maybe we should add it to condition?
          // But getAttributeValues usually returns all values matching the condition.
          // If we want to filter by the input text, we can do it client side or server side.
          // The API getAttributeValues returns unique values for the attribute based on condition.
          // It doesn't seem to take a "like" query for the attribute itself in the standard way, 
          // or maybe it does if we add it to condition?
          // If I add { courseName: queryString } to condition, it might do an exact match or like match depending on backend.
          // Usually autocomplete wants "starts with" or "contains".
          // Let's assume the backend returns a list and we filter it, OR we pass the partial value.
          // If I pass the partial value in condition, the backend might filter it.
          
          // Let's try passing it in condition if it's not empty.
          if (queryString) {
             // condition[fieldName] = queryString // This might be too strict if backend does exact match
          }
          
          const values = await getAttributeValues({
            condition: condition,
            attributeName: fieldName
          })
          
          // Filter client side if backend returns all
          if (queryString && values) {
             return values.filter(v => String(v).toLowerCase().includes(String(queryString).toLowerCase()))
          }
          
          return values || []
        } catch (err) {
          console.error(`Fetch ${fieldName} error:`, err)
          return []
        }
      }
    }

    const handleSearch = () => {
      emit('search', { ...query })
    }

    const handleReset = () => {
      Object.keys(query).forEach(key => {
        if (typeof query[key] === 'number' || key === 'courseId' || key === 'instructorId' || key === 'credits' || key === 'startWeek' || key === 'endWeek' || key === 'startPeriod' || key === 'endPeriod') {
          query[key] = null
        } else {
          query[key] = ''
        }
      })
      emit('reset')
    }

    onMounted(() => {
      handleSearch()
    })

    return {
      query,
      createFetch,
      handleSearch,
      handleReset
    }
  }
}
</script>

<style scoped>
.search-form {
  background: transparent;
  padding: 0;
  margin-bottom: 10px;
}

.search-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px; /* 组间距 */
}

.search-item {
  display: flex;
  align-items: center;
}

.search-label {
  font-size: 12px;
  font-weight: bold;
  color: #555;
  margin-right: 8px;
  white-space: nowrap;
}

.search-inputs {
  display: flex;
  align-items: center;
  gap: 6px; /* 输入框间距 */
}

.inner-label {
  font-size: 12px;
  color: #555;
  margin: 0 4px;
  white-space: nowrap;
}

/* 输入框通用样式 */
.std-input,
.std-select,
:deep(.autocomplete-container) {
  height: 26px;
  box-sizing: border-box;
  font-size: 12px;
}

.std-input,
.std-select {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0 6px;
}

.std-input:focus,
.std-select:focus {
  outline: none;
  border-color: #7C1F89;
}

/* Autocomplete 内部样式覆盖 */
:deep(.autocomplete-input) {
  height: 26px !important;
  padding: 0 6px !important;
  font-size: 12px !important;
  border-color: #ddd !important;
}
:deep(.autocomplete-input:focus) {
  border-color: #7C1F89 !important;
}

/* 宽度工具类 */
.w-40 { width: 40px; }
.w-60 { width: 60px; }
.w-70 { width: 70px; }
.w-80 { width: 80px; }
.w-100 { width: 100px; }
.w-120 { width: 120px; }

.center-text { text-align: center; }

.range-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.separator {
  color: #999;
  font-size: 12px;
}

/* 按钮组 */
.search-actions {
  margin-left: auto; /* 右对齐 */
  display: flex;
  gap: 8px;
}

.btn-search,
.btn-reset {
  height: 26px;
  padding: 0 15px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-search {
  background: #7C1F89;
  color: white;
}

.btn-search:hover:not(:disabled) {
  background: #9c27b0;
}

.btn-reset {
  background: #f0f0f0;
  color: #333;
}

.btn-reset:hover {
  background: #e0e0e0;
}

/* 响应式：屏幕变窄时自动换行 */
@media (max-width: 1024px) {
  .search-row {
    gap: 10px;
  }
  .search-actions {
    margin-left: 0; /* 取消右对齐，跟随流 */
    margin-top: 4px;
  }
}
</style>
