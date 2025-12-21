// Simple course list sorting helpers (frontend-only)

const WEEKDAY_ORDER = {
  '周一': 1,
  '周二': 2,
  '周三': 3,
  '周四': 4,
  '周五': 5,
  '周六': 6,
  '周日': 7
}

const toNumberOrNull = (value) => {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

const normalizeString = (value) => (value === null || value === undefined ? '' : String(value))

const compareNullable = (a, b) => {
  const aNull = a === null || a === undefined
  const bNull = b === null || b === undefined
  if (aNull && bNull) return 0
  if (aNull) return 1
  if (bNull) return -1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), 'zh-CN')
}

const compareTuple = (a, b) => {
  const len = Math.max(a.length, b.length)
  for (let i = 0; i < len; i++) {
    const c = compareNullable(a[i], b[i])
    if (c !== 0) return c
  }
  return 0
}

export const getCourseIdKey = (course) => {
  const raw = course?.courseId
  const n = toNumberOrNull(raw)
  if (n !== null) return n
  return normalizeString(raw)
}

export const getInstructorKey = (course) => normalizeString(course?.instructorName).trim()

export const getTimeKey = (course) => {
  const sessions = Array.isArray(course?.sessions) ? course.sessions : []
  if (sessions.length === 0) return [99, 99, 99]

  let min = [99, 99, 99]
  for (const s of sessions) {
    const weekday = WEEKDAY_ORDER[normalizeString(s?.weekday).trim()] ?? 99
    const start = toNumberOrNull(s?.startPeriod) ?? 99
    const end = toNumberOrNull(s?.endPeriod) ?? 99
    const key = [weekday, start, end]
    if (compareTuple(key, min) < 0) min = key
  }
  return min
}

export const getUtilizationKey = (course) => {
  const enrolled = toNumberOrNull(course?.enrolledCount) ?? 0
  const cap = toNumberOrNull(course?.capacity)
  if (!cap || cap <= 0) return enrolled
  return enrolled / cap
}

export const sortCourses = (courses, { key, order } = {}) => {
  const list = Array.isArray(courses) ? courses : []
  if (!key) return list

  const direction = order === 'desc' ? -1 : 1

  const keyed = list.map((c, idx) => ({ c, idx }))

  const getKey = (course) => {
    if (key === 'courseId') return getCourseIdKey(course)
    if (key === 'instructorName') return getInstructorKey(course)
    if (key === 'time') return getTimeKey(course)
    if (key === 'utilization') return getUtilizationKey(course)
    return null
  }

  keyed.sort((a, b) => {
    const ka = getKey(a.c)
    const kb = getKey(b.c)

    let cmp = 0
    if (Array.isArray(ka) || Array.isArray(kb)) {
      cmp = compareTuple(Array.isArray(ka) ? ka : [ka], Array.isArray(kb) ? kb : [kb])
    } else {
      cmp = compareNullable(ka, kb)
    }

    if (cmp !== 0) return cmp * direction
    // stable
    return a.idx - b.idx
  })

  return keyed.map(x => x.c)
}
