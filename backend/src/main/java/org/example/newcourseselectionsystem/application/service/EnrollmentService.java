package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.EnrollmentRequest;

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
}

