package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseSessionDTO;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                .map(course -> assembleCourseDTO(course, sessionGroup.get(course.getCourseId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listCourseIdsBySession(SessionQueryRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        // 只要课程节次与查询区间有重叠即视为命中
        LambdaQueryWrapper<CourseSession> wrapper = new LambdaQueryWrapper<>();
        Integer start = request.getStartPeriod();
        Integer end = request.getEndPeriod();
        if (start != null && end != null && start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        wrapper.eq(StringUtils.hasText(request.getWeekday()), CourseSession::getWeekday, request.getWeekday())
                .le(end != null, CourseSession::getStartPeriod, end)
                .ge(start != null, CourseSession::getEndPeriod, start);

        return courseSessionMapper.selectList(wrapper).stream()
                .map(CourseSession::getCourseId)
                .distinct()
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
                .ge(request.getStartWeek() != null, Course::getStartWeek, request.getStartWeek())
                .le(request.getEndWeek() != null, Course::getEndWeek, request.getEndWeek());
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

    private CourseWithSessionsDTO assembleCourseDTO(Course course, List<CourseSessionDTO> sessions) {
        CourseWithSessionsDTO dto = new CourseWithSessionsDTO();
        BeanUtils.copyProperties(course, dto);
        dto.setSessions(sessions == null ? Collections.emptyList() : sessions);
        return dto;
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
