package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.DropCourseRequest;
import org.example.newcourseselectionsystem.application.request.EnrollmentRequest;

import java.util.List;

/**
 * 选课服务接口
 */
public interface EnrollmentService {

    /**
     * 学生选课
     *
     * @param request 选课请求
     * @return 选课响应
     */
    EnrollmentResponse enrollCourse(EnrollmentRequest request);

    /**
     * 学生退课
     *
     * @param request 退课请求
     * @return 退课响应
     */
    EnrollmentResponse dropCourse(DropCourseRequest request);

    /**
     * 查询学生当前已选的所有课程
     *
     * @param studentId 学生ID
     * @return 学生已选的课程列表（包含节次信息）
     */
    List<CourseWithSessionsDTO> getStudentCourses(Long studentId);
}

