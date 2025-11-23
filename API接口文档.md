# 选课系统 API 接口文档

## 基础信息

### 基础URL
```
http://localhost:8080/api
```

### 请求格式
- Content-Type: `application/json`
- 所有接口均使用 POST 方法
- 请求体为 JSON 格式

### 响应格式
- Content-Type: `application/json`
- 响应体为 JSON 格式

### 跨域配置
后端已配置 CORS，支持跨域请求。

---

## 一、课程查询接口

### 1.1 根据课程字段组合检索课程

**接口路径：** `/api/courses/search`

**请求方法：** `POST`

**接口描述：** 根据课程表的任意字段进行组合过滤，返回携带节次信息的课程列表。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 否 | 课程ID（精确匹配） |
| courseName | String | 否 | 课程名称（模糊匹配） |
| credits | Integer | 否 | 学分（精确匹配） |
| description | String | 否 | 课程描述（模糊匹配） |
| college | String | 否 | 学院（精确匹配） |
| instructorId | Long | 否 | 教师ID（精确匹配） |
| campus | String | 否 | 校区（精确匹配） |
| classroom | String | 否 | 教室（精确匹配） |
| startWeek | Integer | 否 | 开始周次（精确匹配） |
| endWeek | Integer | 否 | 结束周次（精确匹配） |
| capacity | Integer | 否 | 课程容量（精确匹配） |

**请求示例：**
```json
{
  "courseName": "数据结构",
  "college": "计算机学院",
  "campus": "主校区"
}
```

**响应数据：**

成功响应（HTTP 200）：
```json
[
  {
    "courseId": 1,
    "courseName": "数据结构",
    "credits": 3,
    "description": "数据结构与算法基础",
    "college": "计算机学院",
    "instructorId": 1001,
    "campus": "主校区",
    "classroom": "A101",
    "startWeek": 1,
    "endWeek": 16,
    "capacity": 50,
    "enrolledCount": 30,
    "sessions": [
      {
        "sessionId": 1,
        "weekday": "周一",
        "startPeriod": 1,
        "endPeriod": 2
      },
      {
        "sessionId": 2,
        "weekday": "周三",
        "startPeriod": 3,
        "endPeriod": 4
      }
    ]
  }
]
```

**响应字段说明：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| courseId | Long | 课程ID |
| courseName | String | 课程名称 |
| credits | Integer | 学分 |
| description | String | 课程描述 |
| college | String | 学院 |
| instructorId | Long | 教师ID |
| campus | String | 校区 |
| classroom | String | 教室 |
| startWeek | Integer | 开始周次 |
| endWeek | Integer | 结束周次 |
| capacity | Integer | 课程容量 |
| enrolledCount | Integer | 已选人数 |
| sessions | Array | 课程节次列表 |
| sessions[].sessionId | Long | 节次ID |
| sessions[].weekday | String | 星期几（如：周一、周二） |
| sessions[].startPeriod | Integer | 开始节次（1-12） |
| sessions[].endPeriod | Integer | 结束节次（1-12） |

**注意事项：**
- 所有查询条件均为可选，多个条件之间为 AND 关系
- `courseName` 和 `description` 使用模糊匹配（LIKE），其他字段为精确匹配
- 如果未匹配到任何课程，返回空数组 `[]`
- 如果请求体为 `null`，返回空数组

---

### 1.2 根据节次条件倒查课程

**接口路径：** `/api/courses/search-by-session`

**请求方法：** `POST`

**接口描述：** 根据节次条件（星期、开始节次、结束节次）查找所有符合条件的课程，返回携带节次信息的完整课程信息。查询逻辑：查找所有课程节次范围包含查询范围的课程。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| weekday | String | 是 | 星期几（如：周一、周二、周三等） |
| startPeriod | Integer | 是 | 开始节次，范围：1-12 |
| endPeriod | Integer | 是 | 结束节次，范围：1-12 |

**请求示例：**
```json
{
  "weekday": "周一",
  "startPeriod": 1,
  "endPeriod": 2
}
```

**响应数据：**

成功响应（HTTP 200）：
```json
[
  {
    "courseId": 1,
    "courseName": "数据结构",
    "credits": 3,
    "description": "数据结构与算法基础",
    "college": "计算机学院",
    "instructorId": 1001,
    "campus": "主校区",
    "classroom": "A101",
    "startWeek": 1,
    "endWeek": 16,
    "capacity": 50,
    "enrolledCount": 30,
    "sessions": [
      {
        "sessionId": 1,
        "weekday": "周一",
        "startPeriod": 1,
        "endPeriod": 2
      }
    ]
  }
]
```

**响应字段说明：** 同接口 1.1

**注意事项：**
- `weekday`、`startPeriod`、`endPeriod` 为必填字段
- `startPeriod` 和 `endPeriod` 必须在 1-12 之间
- 如果 `startPeriod > endPeriod`，系统会自动交换两个值
- 查询逻辑：返回所有课程节次范围包含查询范围的课程（即：`courseSession.startPeriod <= query.startPeriod && courseSession.endPeriod >= query.endPeriod`）
- 如果未匹配到任何课程，返回空数组 `[]`

**错误响应示例：**
```json
{
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "周几不能为空",
  "path": "/api/courses/search-by-session"
}
```

---

### 1.3 获取课程属性值集合

**接口路径：** `/api/courses/attribute-values`

**请求方法：** `POST`

**接口描述：** 根据条件筛选课程，然后返回指定属性的所有唯一值集合。例如：查询所有教师为刘钦且教室为A101的课程的校区集合。

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| condition | Object | 否 | 课程查询条件（同接口1.1的请求参数，但不包含节次相关条件） |
| attributeName | String | 是 | 需要聚合的属性名称（如：campus、classroom、college、courseName等） |

**请求示例：**
```json
{
  "condition": {
    "instructorId": 1001,
    "classroom": "A101"
  },
  "attributeName": "campus"
}
```

**响应数据：**

成功响应（HTTP 200）：
```json
[
  "主校区",
  "东校区",
  "西校区"
]
```

**注意事项：**
- `attributeName` 为必填字段
- `condition` 为可选，如果为空则查询所有课程
- 返回的属性值已去重并排序（数字按数值排序，字符串按字母排序）
- 属性名称必须对应 Course 实体类的字段名（使用驼峰命名）
- 如果未匹配到任何课程或属性值，返回空数组 `[]`

**错误响应示例：**
```json
{
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "属性名称不能为空",
  "path": "/api/courses/attribute-values"
}
```

---

## 二、选课接口

### 2.1 学生选课

**接口路径：** `/api/enrollments/enroll`

**请求方法：** `POST`

**接口描述：** 学生选择课程。系统会进行以下验证：
1. 学生身份验证
2. 课程存在性验证
3. 重复选课检查
4. 课程容量限制检查
5. 时间冲突检查（仅警告，不阻止选课）

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | Long | 是 | 学生ID |
| courseId | Long | 是 | 课程ID |

**请求示例：**
```json
{
  "studentId": 2001,
  "courseId": 1
}
```

**响应数据：**

成功响应（HTTP 200）：
```json
{
  "success": true,
  "message": "选课成功",
  "warn": null,
  "enrollmentId": 10001
}
```

**响应字段说明：**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| message | String | 响应消息（成功或错误信息） |
| warn | String | 警告信息（如时间冲突等，不影响选课成功） |
| enrollmentId | Long | 选课记录ID（仅成功时返回） |

**成功场景示例：**

1. 选课成功，无冲突：
```json
{
  "success": true,
  "message": "选课成功",
  "warn": null,
  "enrollmentId": 10001
}
```

2. 选课成功，但有时间冲突警告：
```json
{
  "success": true,
  "message": "选课成功",
  "warn": "选课时间冲突：与课程[高等数学]在[周一]第1-2节冲突",
  "enrollmentId": 10001
}
```

**失败场景示例：**

1. 学生不存在：
```json
{
  "success": false,
  "message": "学生不存在或状态异常",
  "warn": null,
  "enrollmentId": null
}
```

2. 课程不存在：
```json
{
  "success": false,
  "message": "课程不存在或未开放选课",
  "warn": null,
  "enrollmentId": null
}
```

3. 重复选课：
```json
{
  "success": false,
  "message": "您已经选择过该课程",
  "warn": null,
  "enrollmentId": null
}
```

4. 课程已满：
```json
{
  "success": false,
  "message": "课程已满，当前选课人数：50/50",
  "warn": null,
  "enrollmentId": null
}
```

**错误响应示例（参数验证失败）：**
```json
{
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "学生ID不能为空",
  "path": "/api/enrollments/enroll"
}
```

**注意事项：**
- `studentId` 和 `courseId` 为必填字段
- 时间冲突检查逻辑：检查新选课程与已选课程是否在同一星期且时间段有重叠
- 时间冲突不会阻止选课，但会在 `warn` 字段中返回警告信息
- 选课成功后，课程的 `enrolledCount` 会自动加1
- 选课记录的状态为 "已选"

---

### 2.2 学生退课

**接口路径：** `/api/enrollments/drop`

**请求方法：** `POST`

**接口描述：** 学生退选已选择的课程。系统会进行以下验证：
1. 学生身份验证
2. 课程存在性验证
3. 选课记录验证（必须存在且状态为"已选"）

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | Long | 是 | 学生ID |
| courseId | Long | 是 | 课程ID |

**请求示例：**
```json
{
  "studentId": 2001,
  "courseId": 1
}
```

**响应数据：**

成功响应（HTTP 200）：
```json
{
  "success": true,
  "message": "退课成功",
  "warn": null,
  "enrollmentId": 10001
}
```

**失败场景示例：**

1. 学生不存在：
```json
{
  "success": false,
  "message": "学生不存在或状态异常",
  "warn": null,
  "enrollmentId": null
}
```

2. 课程不存在：
```json
{
  "success": false,
  "message": "课程不存在",
  "warn": null,
  "enrollmentId": null
}
```

3. 未选课或已退选：
```json
{
  "success": false,
  "message": "您尚未选择该课程或该课程已退选",
  "warn": null,
  "enrollmentId": null
}
```

**注意事项：**
- `studentId` 和 `courseId` 为必填字段
- 只能退选状态为"已选"的课程
- 退课成功后，课程的 `enrolledCount` 会自动减1
- 选课记录的状态会更新为 "已退选"

---

## 三、错误码说明

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误（如必填字段缺失、参数格式错误等） |
| 500 | 服务器内部错误 |

### 业务错误码

业务错误通过响应体中的 `success` 和 `message` 字段返回，常见错误信息：

| 错误信息 | 说明 |
|----------|------|
| 学生不存在或状态异常 | 学生ID不存在或学生状态异常 |
| 课程不存在或未开放选课 | 课程ID不存在或课程未开放 |
| 您已经选择过该课程 | 该学生已经选择过该课程 |
| 课程已满，当前选课人数：X/Y | 课程容量已满，无法继续选课 |
| 您尚未选择该课程或该课程已退选 | 退课时，该学生未选择该课程或已退选 |
| 周几不能为空 | 节次查询接口的 weekday 字段为空 |
| 开始节次不能为空 | 节次查询接口的 startPeriod 字段为空 |
| 结束节次不能为空 | 节次查询接口的 endPeriod 字段为空 |
| 节次最小为1 | 节次值小于1 |
| 节次最大为12 | 节次值大于12 |
| 属性名称不能为空 | 属性值查询接口的 attributeName 字段为空 |

---

## 四、数据字典

### 星期字段（weekday）取值
- 周一
- 周二
- 周三
- 周四
- 周五
- 周六
- 周日

### 节次范围
- 最小节次：1
- 最大节次：11
- 节次范围：1-11

### 选课状态
- 已选：表示学生已成功选择该课程
- 已退选：表示学生已退选该课程

---

## 六、注意事项

1. **请求格式**：所有接口均使用 POST 方法，请求体为 JSON 格式
2. **参数验证**：后端会对必填字段和参数格式进行验证，不符合要求会返回 400 错误
3. **空值处理**：可选字段如果不需要查询，可以不传或传 `null`
4. **模糊匹配**：`courseName` 和 `description` 字段使用模糊匹配（LIKE），其他字段为精确匹配
5. **时间冲突**：选课接口会检查时间冲突，但不会阻止选课，只会在 `warn` 字段中返回警告
6. **事务处理**：选课操作使用事务处理，确保数据一致性
7. **并发控制**：选课接口使用 `REPEATABLE_READ` 隔离级别，防止并发选课导致的数据不一致

---

## 七、更新日志

- 2024-01-01：初始版本，包含课程查询和选课功能
- 2024-01-02：新增学生退课功能

