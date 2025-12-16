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
 * 获取选课建议
 * @param {Object} data - 请求数据 { studentId: number }
 * @returns {Promise<Array>} 推荐课程列表
 */
export function getRecommendations(data) {
  return api.post('/graduation/recommendations', data)
}

/**
 * 综合查询课程（课程条件 + 节次条件）
 * @param {Object} combinedRequest - 综合查询请求对象
 * @returns {Promise<Array>} 课程列表
 */
export function searchCombinedCourses(combinedRequest) {
  return api.post('/courses/search/combined', combinedRequest)
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

/**
 * 学生选课
 * @param {Object} enrollmentRequest - 选课请求对象
 * @param {Number} enrollmentRequest.studentId - 学生ID
 * @param {Number} enrollmentRequest.courseId - 课程ID
 * @returns {Promise<Object>} 选课响应对象 { success, message, warn, enrollmentId }
 */
export function enrollCourse(enrollmentRequest) {
  return api.post('/enrollments/enroll', enrollmentRequest)
}

/**
 * 学生退课
 * @param {Object} dropRequest - 退课请求对象
 * @param {Number} dropRequest.studentId - 学生ID
 * @param {Number} dropRequest.courseId - 课程ID
 * @returns {Promise<Object>} 退课响应对象 { success, message, warn, enrollmentId }
 */
export function dropCourse(dropRequest) {
  return api.post('/enrollments/drop', dropRequest)
}

/**
 * 查询学生当前已选的所有课程
 * @param {Number} studentId - 学生ID
 * @returns {Promise<Array>} 学生已选的课程列表（包含节次信息）
 */
export function getStudentCourses(studentId) {
  return api.get(`/enrollments/student/${studentId}`)
}

/**
 * 获取智能课程推荐（流式响应）
 * @param {Object} request - 推荐请求对象
 * @param {Number} request.studentId - 学生ID
 * @param {String} request.prompt - 用户输入的推荐请求文本
 * @param {Function} onMessage - 接收到消息时的回调函数 (content: string) => void
 * @param {Function} onError - 错误回调函数 (error: Error) => void
 * @param {Function} onComplete - 完成回调函数 () => void
 * @returns {Function} 取消函数，调用可取消请求
 */
export function getCourseRecommendationStream(request, onMessage, onError, onComplete) {
  const url = '/api/recommendations/recommend'
  let cancelled = false
  let reader = null
  
  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request)
  })
  .then(async response => {
    if (!response.ok) {
      const errorText = await response.text().catch(() => '')
      throw new Error(errorText || `HTTP error! status: ${response.status}`)
    }
    
    reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    
    try {
      while (true) {
        if (cancelled) {
          await reader.cancel()
          break
        }
        
        const { done, value } = await reader.read()
        
        if (done) {
          // 处理缓冲区中剩余的数据
          if (buffer.trim()) {
            const lines = buffer.split('\n')
            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.substring(6).trim()
                if (data && data !== '[DONE]' && onMessage) {
                  onMessage(data)
                }
              }
            }
          }
          if (onComplete) onComplete()
          break
        }
        
        // 解码数据
        buffer += decoder.decode(value, { stream: true })
        
        // 处理 SSE 格式的数据（按双换行符分割事件）
        const events = buffer.split('\n\n')
        buffer = events.pop() || '' // 保留最后一个不完整的事件
        
        for (const event of events) {
          if (cancelled) break
          
          const lines = event.split('\n')
          let eventData = ''
          let eventType = 'message'
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              eventData = line.substring(6).trim()
            } else if (line.startsWith('event: ')) {
              eventType = line.substring(7).trim()
            }
          }
          
          if (eventData) {
            if (eventType === 'error') {
              if (onError) {
                onError(new Error(eventData))
              }
            } else if (eventData !== '[DONE]') {
              if (onMessage) onMessage(eventData)
            }
          }
        }
      }
    } catch (err) {
      if (!cancelled && onError) {
        onError(err)
      }
    }
  })
  .catch(error => {
    if (!cancelled && onError) {
      onError(error)
    }
  })
  
  // 返回取消函数
  return () => {
    cancelled = true
    if (reader) {
      reader.cancel().catch(() => {})
    }
  }
}

