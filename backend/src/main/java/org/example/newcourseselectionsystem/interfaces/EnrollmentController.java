package org.example.newcourseselectionsystem.interfaces;

import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.DropCourseRequest;
import org.example.newcourseselectionsystem.application.request.EnrollmentRequest;
import org.example.newcourseselectionsystem.application.service.EnrollmentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 选课接口
 */
@RestController
@RequestMapping("/api/enrollments")
@Validated
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * 学生选课
     */
    @PostMapping("/enroll")
    public EnrollmentResponse enrollCourse(@Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.enrollCourse(request);
    }

    /**
     * 学生退课
     */
    @PostMapping("/drop")
    public EnrollmentResponse dropCourse(@Valid @RequestBody DropCourseRequest request) {
        return enrollmentService.dropCourse(request);
    }

    /**
     * 查询学生当前已选的所有课程
     *
     * @param studentId 学生ID
     * @return 学生已选的课程列表（包含节次信息）
     */
    @GetMapping("/student/{studentId}")
    public List<CourseWithSessionsDTO> getStudentCourses(@PathVariable Long studentId) {
        return enrollmentService.getStudentCourses(studentId);
    }
}
