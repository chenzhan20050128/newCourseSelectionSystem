package org.example.newcourseselectionsystem.interfaces;

import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.EnrollmentRequest;
import org.example.newcourseselectionsystem.application.service.EnrollmentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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
}

