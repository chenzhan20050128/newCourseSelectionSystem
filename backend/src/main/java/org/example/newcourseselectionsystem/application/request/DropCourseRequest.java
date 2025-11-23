package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 退课请求
 */
@Data
public class DropCourseRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 课程ID
     */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
}

