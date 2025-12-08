package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles("test")
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Test
    void shouldQueryCoursesByCondition() {
        CourseQueryRequest request = new CourseQueryRequest();
        request.setCollege("计算机学院");
        request.setCampus("本部");

        List<CourseWithSessionsDTO> result = courseService.listCoursesByCondition(request);

        Assertions.assertFalse(result.isEmpty(), "应当查询到计算机学院的课程");
        CourseWithSessionsDTO dataStructure = result.stream()
                .filter(dto -> "数据结构".equals(dto.getCourseName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(dataStructure, "应当包含数据结构课程");
        Assertions.assertFalse(dataStructure.getSessions().isEmpty(), "课程应附带节次信息");
    }

    @Test
    void shouldQueryCoursesBySession() {
        SessionQueryRequest request = new SessionQueryRequest();
        request.setWeekdays(Arrays.asList("周三", "周四"));
        request.setStartPeriod(5);
        request.setEndPeriod(5);

        List<CourseWithSessionsDTO> courses = courseService.listCoursesBySession(request);

        Assertions.assertFalse(courses.isEmpty(), "周三/周四第5节应当有课程");
        List<Long> courseIds = courses.stream()
                .map(CourseWithSessionsDTO::getCourseId)
                .sorted()
                .collect(Collectors.toList());
        Assertions.assertTrue(courseIds.contains(1L), "数据结构应当匹配");
        Assertions.assertTrue(courseIds.contains(4L), "计算机网络应当匹配");

        // 验证返回的课程包含完整信息和节次
        CourseWithSessionsDTO course = courses.stream()
                .filter(c -> c.getCourseId().equals(1L))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(course, "应当包含数据结构课程");
        Assertions.assertNotNull(course.getCourseName(), "课程名称不应为空");
        Assertions.assertFalse(course.getSessions().isEmpty(), "课程应附带节次信息");
    }
}
