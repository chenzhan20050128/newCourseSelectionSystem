package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseSessionDTO;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseAttributeQueryRequest;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;
import org.example.newcourseselectionsystem.application.request.CombinedCourseQueryRequest;
import org.example.newcourseselectionsystem.application.service.CourseService;
import org.example.newcourseselectionsystem.domain.entity.Course;
import org.example.newcourseselectionsystem.domain.entity.CourseSession;
import org.example.newcourseselectionsystem.domain.entity.Enrollment;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseSessionMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.EnrollmentMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程查询实现（更新为按instructorName过滤）
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseSessionMapper courseSessionMapper;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    public List<CourseWithSessionsDTO> searchCourses(CombinedCourseQueryRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        CourseQueryRequest courseCond = request.getCourseCondition();
        SessionQueryRequest sessionCond = request.getSessionCondition();
        Long studentId = request.getStudentId();

        List<Long> candidateCourseIds = null;
        List<Course> candidateCourses = Collections.emptyList();

        // 1) 先处理课程字段条件（如果有）
        if (courseCond != null) {
            LambdaQueryWrapper<Course> courseWrapper = buildCourseWrapper(courseCond);
            candidateCourses = courseMapper.selectList(courseWrapper);
            if (CollectionUtils.isEmpty(candidateCourses)) {
                return Collections.emptyList();
            }
            candidateCourseIds = candidateCourses.stream().map(Course::getCourseId).collect(Collectors.toList());
        }

        // 2) 再处理节次条件（如果有）并与课程候选集合求交集或并集
        if (sessionCond != null) {
            LambdaQueryWrapper<CourseSession> sessionWrapper = new LambdaQueryWrapper<>();
            if (sessionCond.getWeekdays() != null && !sessionCond.getWeekdays().isEmpty()) {
                sessionWrapper.in(CourseSession::getWeekday, sessionCond.getWeekdays());
            }
            // 希望课程节次区间覆盖查询区间：[start,end] 被课程区间包含
            if (sessionCond.getStartPeriod() != null) {
                sessionWrapper.le(CourseSession::getStartPeriod, sessionCond.getStartPeriod());
            }
            if (sessionCond.getEndPeriod() != null) {
                sessionWrapper.ge(CourseSession::getEndPeriod, sessionCond.getEndPeriod());
            }
            List<CourseSession> matchedSessions = courseSessionMapper.selectList(sessionWrapper);
            if (CollectionUtils.isEmpty(matchedSessions)) {
                return Collections.emptyList();
            }
            List<Long> matchedIds = matchedSessions.stream().map(CourseSession::getCourseId).distinct().collect(Collectors.toList());

            if (candidateCourseIds == null) {
                // 只有节次条件：候选ID = 节次匹配ID
                candidateCourseIds = matchedIds;
                candidateCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getCourseId, candidateCourseIds));
            } else {
                // 两者交集
                candidateCourseIds.retainAll(matchedIds);
                if (CollectionUtils.isEmpty(candidateCourseIds)) {
                    return Collections.emptyList();
                }
                candidateCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getCourseId, candidateCourseIds));
            }
        }

        // 如果两类条件都为空，则返回全部课程
        if (courseCond == null && sessionCond == null) {
            candidateCourses = courseMapper.selectList(null);
            candidateCourseIds = candidateCourses.stream().map(Course::getCourseId).collect(Collectors.toList());
        }

        Map<Long, List<CourseSessionDTO>> sessionGroup = loadSessions(candidateCourseIds);

        // 预先查询该学生已选的课程ID集合（仅当提供了studentId）
        Set<Long> enrolledCourseIds = Collections.emptySet();
        if (studentId != null) {
            LambdaQueryWrapper<Enrollment> enrollmentWrapper = new LambdaQueryWrapper<>();
            enrollmentWrapper.eq(Enrollment::getStudentId, studentId)
                    .eq(Enrollment::getStatus, "已选");
            List<Enrollment> enrollments = enrollmentMapper.selectList(enrollmentWrapper);
            enrolledCourseIds = enrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toSet());
        }

        Set<Long> finalEnrolledCourseIds = enrolledCourseIds; // effectively final for lambda
        return candidateCourses.stream()
                .map(course -> {
                    CourseWithSessionsDTO dto = assembleCourseDTO(course, sessionGroup.get(course.getCourseId()),
                            course.getEnrolledCount() != null ? course.getEnrolledCount() : 0);
                    if (studentId != null) {
                        dto.setIsEnrolled(finalEnrolledCourseIds.contains(course.getCourseId()));
                    } else {
                        dto.setIsEnrolled(null);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseWithSessionsDTO> listCoursesByCondition(CourseQueryRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        // 动态拼接课程字段筛选条件（原直连逻辑）
        LambdaQueryWrapper<Course> wrapper = buildCourseWrapper(request);
        List<Course> courses = courseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptyList();
        }
        List<Long> courseIds = courses.stream().map(Course::getCourseId).collect(Collectors.toList());
        Map<Long, List<CourseSessionDTO>> sessionGroup = loadSessions(courseIds);
        return courses.stream()
                .map(course -> assembleCourseDTO(course, sessionGroup.get(course.getCourseId()),
                        course.getEnrolledCount() != null ? course.getEnrolledCount() : 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseWithSessionsDTO> listCoursesBySession(SessionQueryRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<CourseSession> sessionWrapper = new LambdaQueryWrapper<>();
        if (request.getWeekdays() != null && !request.getWeekdays().isEmpty()) {
            sessionWrapper.in(CourseSession::getWeekday, request.getWeekdays());
        }
        // 希望课程节次区间覆盖查询区间：[start,end] 被课程区间包含
        if (request.getStartPeriod() != null) {
            sessionWrapper.le(CourseSession::getStartPeriod, request.getStartPeriod());
        }
        if (request.getEndPeriod() != null) {
            sessionWrapper.ge(CourseSession::getEndPeriod, request.getEndPeriod());
        }
        List<CourseSession> sessions = courseSessionMapper.selectList(sessionWrapper);
        if (CollectionUtils.isEmpty(sessions)) {
            return Collections.emptyList();
        }
        List<Long> courseIds = sessions.stream().map(CourseSession::getCourseId).distinct().collect(Collectors.toList());
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getCourseId, courseIds));
        Map<Long, List<CourseSessionDTO>> sessionGroup = loadSessions(courseIds);
        return courses.stream()
                .map(course -> assembleCourseDTO(course, sessionGroup.get(course.getCourseId()),
                        course.getEnrolledCount() != null ? course.getEnrolledCount() : 0))
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<Course> buildCourseWrapper(CourseQueryRequest request) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (request == null) {
            return wrapper;
        }
        wrapper.eq(request.getCourseId() != null, Course::getCourseId, request.getCourseId());
        wrapper.like(StringUtils.hasText(request.getCourseName()), Course::getCourseName, request.getCourseName());
        wrapper.eq(request.getCredits() != null, Course::getCredits, request.getCredits());
        wrapper.like(StringUtils.hasText(request.getDescription()), Course::getDescription, request.getDescription());
        wrapper.eq(StringUtils.hasText(request.getCollege()), Course::getCollege, request.getCollege());
        // 更新为按instructorName过滤
        wrapper.like(StringUtils.hasText(request.getInstructorName()), Course::getInstructorName, request.getInstructorName());
        wrapper.eq(StringUtils.hasText(request.getCampus()), Course::getCampus, request.getCampus());
        wrapper.eq(StringUtils.hasText(request.getClassroom()), Course::getClassroom, request.getClassroom());
        wrapper.ge(request.getStartWeek() != null, Course::getStartWeek, request.getStartWeek());
        wrapper.le(request.getEndWeek() != null, Course::getEndWeek, request.getEndWeek());
        wrapper.eq(request.getCapacity() != null, Course::getCapacity, request.getCapacity());
        return wrapper;
    }

    private Map<Long, List<CourseSessionDTO>> loadSessions(List<Long> courseIds) {
        if (CollectionUtils.isEmpty(courseIds)) {
            return Collections.emptyMap();
        }
        // 一次性批量加载节次信息，避免 N+1 查询
        return courseSessionMapper.selectList(
                new LambdaQueryWrapper<CourseSession>().in(CourseSession::getCourseId, courseIds)).stream()
                .collect(Collectors.groupingBy(
                        CourseSession::getCourseId,
                        Collectors.mapping(this::convertSession, Collectors.toList())));
    }

    private CourseSessionDTO convertSession(CourseSession session) {
        CourseSessionDTO dto = new CourseSessionDTO();
        dto.setSessionId(session.getSessionId());
        dto.setWeekday(session.getWeekday());
        dto.setStartPeriod(session.getStartPeriod());
        dto.setEndPeriod(session.getEndPeriod());
        return dto;
    }

    private CourseWithSessionsDTO assembleCourseDTO(Course course, List<CourseSessionDTO> sessions,
            Integer enrolledCount) {
        CourseWithSessionsDTO dto = new CourseWithSessionsDTO();
        BeanUtils.copyProperties(course, dto);
        dto.setSessions(sessions == null ? Collections.emptyList() : sessions);
        dto.setEnrolledCount(enrolledCount);
        // 设置instructorName
        dto.setInstructorName(course.getInstructorName());
        return dto;
    }

    @Override
    public List<Object> getAttributeValues(CourseAttributeQueryRequest request) {
        if (request == null || request.getCondition() == null ||
                !org.springframework.util.StringUtils.hasText(request.getAttributeName())) {
            return Collections.emptyList();
        }

        // 复用现有的筛选逻辑
        LambdaQueryWrapper<Course> wrapper = buildCourseWrapper(request.getCondition());
        List<Course> courses = courseMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptyList();
        }

        // 提取指定属性的值并去重
        String attributeName = request.getAttributeName();
        Set<Object> uniqueValues = courses.stream()
                .map(course -> extractAttributeValue(course, attributeName))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return uniqueValues.stream()
                .sorted((a, b) -> {
                    // 对结果进行排序，数字类型按数值排序，字符串类型按字母排序
                    if (a instanceof Comparable && b instanceof Comparable) {
                        @SuppressWarnings("unchecked")
                        Comparable<Object> aComp = (Comparable<Object>) a;
                        return aComp.compareTo(b);
                    }
                    return a.toString().compareTo(b.toString());
                })
                .collect(Collectors.toList());
    }

    /**
     * 从Course对象中提取指定属性的值
     */
    private Object extractAttributeValue(Course course, String attributeName) {
        try {
            // 将属性名转换为getter方法名（首字母大写）
            String methodName = "get" +
                    attributeName.substring(0, 1).toUpperCase() +
                    attributeName.substring(1);

            Method method = Course.class.getMethod(methodName);
            return method.invoke(course);
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的属性名称: " + attributeName, e);
        }
    }
}
