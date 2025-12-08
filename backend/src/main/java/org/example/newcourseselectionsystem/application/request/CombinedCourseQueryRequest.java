package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.Valid;

/**
 * 合并后的课程查询请求：包含课程字段条件 + 可选的节次条件
 */
@Data
public class CombinedCourseQueryRequest {

    /**
     * 课程字段过滤条件（全部可选）
     */
    private CourseQueryRequest courseCondition;

    /**
     * 节次过滤条件（可选）
     */
    @Valid
    private SessionQueryRequest sessionCondition;
}

