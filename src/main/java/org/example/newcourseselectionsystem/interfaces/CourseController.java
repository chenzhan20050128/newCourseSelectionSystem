package org.example.newcourseselectionsystem.interfaces;

import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;
import org.example.newcourseselectionsystem.application.service.CourseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 课程查询接口
 */
@RestController
@RequestMapping("/api/courses")
@Validated
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 根据课程字段组合检索，返回携带节次信息的课程信息
     */
    @PostMapping("/search")
    public List<CourseWithSessionsDTO> searchCourses(@RequestBody CourseQueryRequest request) {
        return courseService.listCoursesByCondition(request);
    }

    /**
     * 根据节次条件倒查所有课程ID
     */
    @PostMapping("/search-by-session")
    public List<Long> searchCourseIdsBySession(@Valid @RequestBody SessionQueryRequest request) {
        return courseService.listCourseIdsBySession(request);
    }
}
