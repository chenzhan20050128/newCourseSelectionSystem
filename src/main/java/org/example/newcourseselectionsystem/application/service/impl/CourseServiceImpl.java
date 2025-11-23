package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseSessionDTO;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseAttributeQueryRequest;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;
import org.example.newcourseselectionsystem.application.service.CourseService;
import org.example.newcourseselectionsystem.domain.entity.Course;
import org.example.newcourseselectionsystem.domain.entity.CourseSession;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseSessionMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程查询实现
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseSessionMapper courseSessionMapper;

    @Override
    public List<CourseWithSessionsDTO> listCoursesByCondition(CourseQueryRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        // 动态拼接课程字段筛选条件
        LambdaQueryWrapper<Course> wrapper = buildCourseWrapper(request);
        List<Course> courses = courseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptyList();
        }
        List<Long> courseIds = courses.stream()
                .map(Course::getCourseId)
                .collect(Collectors.toList());

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
        // 查询的节次范围应该被课程节次范围包含（包含关系）
        // 即：courseSession.startPeriod <= query.startPeriod && courseSession.endPeriod
        // >= query.endPeriod
        LambdaQueryWrapper<CourseSession> wrapper = new LambdaQueryWrapper<>();
        Integer start = request.getStartPeriod();
        Integer end = request.getEndPeriod();
        if (start != null && end != null && start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        wrapper.eq(StringUtils.hasText(request.getWeekday()), CourseSession::getWeekday, request.getWeekday())
                .le(start != null, CourseSession::getStartPeriod, start)
                .ge(end != null, CourseSession::getEndPeriod, end);

        // 获取符合条件的课程ID列表
        List<Long> courseIds = courseSessionMapper.selectList(wrapper).stream()
                .map(CourseSession::getCourseId)
                .distinct()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(courseIds)) {
            return Collections.emptyList();
        }

        // 根据课程ID查询完整的课程信息
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().in(Course::getCourseId, courseIds));

        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptyList();
        }

        // 加载节次信息
        Map<Long, List<CourseSessionDTO>> sessionGroup = loadSessions(courseIds);

        // 组装返回结果
        return courses.stream()
                .map(course -> assembleCourseDTO(course, sessionGroup.get(course.getCourseId()),
                        course.getEnrolledCount() != null ? course.getEnrolledCount() : 0))
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<Course> buildCourseWrapper(CourseQueryRequest request) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getCourseId() != null, Course::getCourseId, request.getCourseId())
                .like(StringUtils.hasText(request.getCourseName()), Course::getCourseName, request.getCourseName())
                .eq(request.getCredits() != null, Course::getCredits, request.getCredits())
                .like(StringUtils.hasText(request.getDescription()), Course::getDescription, request.getDescription())
                .eq(StringUtils.hasText(request.getCollege()), Course::getCollege, request.getCollege())
                .eq(request.getInstructorId() != null, Course::getInstructorId, request.getInstructorId())
                .eq(StringUtils.hasText(request.getCampus()), Course::getCampus, request.getCampus())
                .eq(StringUtils.hasText(request.getClassroom()), Course::getClassroom, request.getClassroom())
                .eq(request.getStartWeek() != null, Course::getStartWeek, request.getStartWeek())
                .eq(request.getEndWeek() != null, Course::getEndWeek, request.getEndWeek())
                .eq(request.getCapacity() != null, Course::getCapacity, request.getCapacity());
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

    private CourseWithSessionsDTO assembleCourseDTO(Course course, List<CourseSessionDTO> sessions,
            Integer enrolledCount) {
        CourseWithSessionsDTO dto = new CourseWithSessionsDTO();
        BeanUtils.copyProperties(course, dto);
        dto.setSessions(sessions == null ? Collections.emptyList() : sessions);
        dto.setEnrolledCount(enrolledCount);
        return dto;
    }

    @Override
    public List<Object> getAttributeValues(CourseAttributeQueryRequest request) {
        if (request == null || request.getCondition() == null ||
                !StringUtils.hasText(request.getAttributeName())) {
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

    private CourseSessionDTO convertSession(CourseSession session) {
        CourseSessionDTO dto = new CourseSessionDTO();
        dto.setSessionId(session.getSessionId());
        dto.setWeekday(session.getWeekday());
        dto.setStartPeriod(session.getStartPeriod());
        dto.setEndPeriod(session.getEndPeriod());
        return dto;
    }
}
