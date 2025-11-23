import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    if (error.response) {
      return Promise.reject({
        message: error.response.data?.message || '请求失败',
        status: error.response.status
      })
    }
    return Promise.reject({
      message: '网络错误，请检查后端服务是否启动',
      status: 0
    })
  }
)

/**
 * 根据课程条件查询课程
 * @param {Object} queryRequest - 课程查询请求对象
 * @returns {Promise<Array>} 课程列表
 */
export function searchCourses(queryRequest) {
  return api.post('/courses/search', queryRequest)
}

/**
 * 根据节次条件查询课程，返回完整的课程信息
 * @param {Object} sessionRequest - 节次查询请求对象
 * @returns {Promise<Array>} 课程列表（包含完整课程信息和节次信息）
 */
export function searchCoursesBySession(sessionRequest) {
  return api.post('/courses/search-by-session', sessionRequest)
}

/**
 * 根据条件筛选课程，返回指定属性的所有唯一值集合
 * @param {Object} attributeQueryRequest - 属性查询请求对象
 * @param {Object} attributeQueryRequest.condition - 课程查询条件
 * @param {String} attributeQueryRequest.attributeName - 需要聚合的属性名称
 * @returns {Promise<Array>} 属性值的唯一值集合
 */
export function getAttributeValues(attributeQueryRequest) {
  return api.post('/courses/attribute-values', attributeQueryRequest)
}

