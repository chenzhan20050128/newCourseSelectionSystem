/**
 * 检查课程时间冲突
 * @param {Object} newCourse - 待选课程
 * @param {Array} enrolledCourses - 已选课程列表
 * @returns {Array} - 冲突的课程列表
 */
export function checkTimeConflict(newCourse, enrolledCourses) {
  if (!newCourse || !enrolledCourses || enrolledCourses.length === 0) {
    return [];
  }

  const conflicts = [];

  // 待选课程的时间段
  const newSessions = Array.isArray(newCourse.sessions) ? newCourse.sessions : [];
  const newStartWeek = normalizeWeek(newCourse.startWeek, 1);
  const newEndWeek = normalizeWeek(newCourse.endWeek, 16);

  for (const enrolled of enrolledCourses) {
    // 跳过自身（如果是修改课程的情况，虽然这里主要是选课）
    if (enrolled.courseId === newCourse.courseId) continue;

    const enrolledSessions = Array.isArray(enrolled.sessions) ? enrolled.sessions : [];
    const enrolledStartWeek = normalizeWeek(enrolled.startWeek, 1);
    const enrolledEndWeek = normalizeWeek(enrolled.endWeek, 16);

    // 1. 检查周次是否有重叠（并考虑单双周）
    const overlapStart = Math.max(newStartWeek, enrolledStartWeek);
    const overlapEnd = Math.min(newEndWeek, enrolledEndWeek);
    if (overlapStart > overlapEnd) continue;

    // 2. 检查具体上课时间是否有重叠
    let isConflict = false;
    for (const ns of newSessions) {
      for (const es of enrolledSessions) {
        // 检查星期几（兼容：1/"1"/"周一"/"星期一" 等）
        const nw = normalizeWeekday(ns.weekday);
        const ew = normalizeWeekday(es.weekday);
        if (nw === null || ew === null) continue;
        if (nw !== ew) continue;

        // 检查节次
        // [start, end] 重叠条件: ns.start <= es.end && es.start <= ns.end
        const nsStart = normalizePeriod(ns.startPeriod);
        const nsEnd = normalizePeriod(ns.endPeriod);
        const esStart = normalizePeriod(es.startPeriod);
        const esEnd = normalizePeriod(es.endPeriod);
        if (nsStart === null || nsEnd === null || esStart === null || esEnd === null) continue;

        if (!(nsStart <= esEnd && esStart <= nsEnd)) continue;

        // 检查单双周是否存在交集
        const nsWeekType = normalizeWeekType(ns.weekType);
        const esWeekType = normalizeWeekType(es.weekType);
        if (!hasWeekTypeIntersection(overlapStart, overlapEnd, nsWeekType, esWeekType)) continue;

        isConflict = true;
        break;
      }
      if (isConflict) break;
    }

    if (isConflict) {
      conflicts.push({
        courseId: enrolled.courseId,
        courseName: enrolled.courseName,
        // 构造一个简单的冲突时间描述
        timeStr: formatSessions(enrolledSessions)
      });
    }
  }

  return conflicts;
}

function formatSessions(sessions) {
  if (!sessions || sessions.length === 0) return '';
  // 简单拼接，例如 "周一 1-2节(单), 周三 3-4节"
  return sessions
    .map(s => {
      const w = weekdayLabel(normalizeWeekday(s.weekday), s.weekday);
      const startP = normalizePeriod(s.startPeriod);
      const endP = normalizePeriod(s.endPeriod);
      const wt = normalizeWeekType(s.weekType);
      const wtLabel = wt === 1 ? '(单)' : wt === 2 ? '(双)' : '';
      if (startP === null || endP === null) return `${w}${wtLabel}`;
      return `${w} ${startP}-${endP}节${wtLabel}`;
    })
    .join(', ');
}

function normalizeWeek(value, fallback) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

function normalizePeriod(value) {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

function normalizeWeekday(value) {
  if (value === null || value === undefined || value === '') return null;

  // 数字或数字字符串
  const n = Number(value);
  if (Number.isFinite(n) && n >= 1 && n <= 7) return n;

  const raw = String(value).trim();
  if (!raw) return null;

  // 处理：周一/星期一/礼拜一
  const m = raw.match(/(周|星期|礼拜)\s*([一二三四五六日天])/);
  if (m && m[2]) return chineseWeekdayToNumber(m[2]);

  // 处理：一/二/三...
  if (raw.length === 1) {
    const c = chineseWeekdayToNumber(raw);
    return c;
  }

  return null;
}

function chineseWeekdayToNumber(ch) {
  const map = { '一': 1, '二': 2, '三': 3, '四': 4, '五': 5, '六': 6, '日': 7, '天': 7 };
  return map[ch] ?? null;
}

function weekdayLabel(normalized, original) {
  const map = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' };
  if (normalized && map[normalized]) return map[normalized];
  if (original === null || original === undefined) return '周?';
  return String(original);
}

// weekType: 0/undefined=全周, 1=单周, 2=双周
function normalizeWeekType(value) {
  const n = Number(value);
  if (n === 1 || n === 2) return n;
  return 0;
}

function matchesWeekType(week, weekType) {
  if (weekType === 1) return week % 2 === 1;
  if (weekType === 2) return week % 2 === 0;
  return true;
}

function hasWeekTypeIntersection(startWeek, endWeek, aType, bType) {
  // 快速路径：两者全周
  if ((aType ?? 0) === 0 && (bType ?? 0) === 0) return true;
  for (let w = startWeek; w <= endWeek; w += 1) {
    if (matchesWeekType(w, aType ?? 0) && matchesWeekType(w, bType ?? 0)) return true;
  }
  return false;
}
