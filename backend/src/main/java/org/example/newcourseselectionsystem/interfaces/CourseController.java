package org.example.newcourseselectionsystem.interfaces;

import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseAttributeQueryRequest;
import org.example.newcourseselectionsystem.application.request.CourseQueryRequest;
import org.example.newcourseselectionsystem.application.request.SessionQueryRequest;
import org.example.newcourseselectionsystem.application.request.CombinedCourseQueryRequest;
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
     * 新的合并查询接口：课程字段 + 可选节次条件
     */
    @PostMapping("/search/combined")
    public List<CourseWithSessionsDTO> searchCombined(@Valid @RequestBody CombinedCourseQueryRequest request) {
        return courseService.searchCourses(request);
    }

    /**
     * 根据课程字段组合检索，返回携带节次信息的课程信息
     */
    @PostMapping("/search")
    public List<CourseWithSessionsDTO> searchCourses(@RequestBody CourseQueryRequest request) {
        return courseService.listCoursesByCondition(request);
    }

    /**
     * 根据节次条件倒查所有课程，返回携带节次信息的完整课程信息
     */
    @PostMapping("/search-by-session")
    public List<CourseWithSessionsDTO> searchCoursesBySession(@Valid @RequestBody SessionQueryRequest request) {
        return courseService.listCoursesBySession(request);
    }

    /**
     * 根据条件筛选课程，返回指定属性的所有唯一值集合
     * 例如：查询所有教师为刘钦且classroom为A101的课程的campus集合
     */
    @PostMapping("/attribute-values")
    public List<Object> getAttributeValues(@Valid @RequestBody CourseAttributeQueryRequest request) {
        return courseService.getAttributeValues(request);
    }
}
