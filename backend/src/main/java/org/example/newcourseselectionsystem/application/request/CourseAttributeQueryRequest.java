package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 课程属性值查询请求：根据条件筛选课程，然后返回指定属性的所有唯一值
 */
@Data
public class CourseAttributeQueryRequest {

    /**
     * 课程查询条件（排除节次相关条件）
     */
    private CourseQueryRequest condition;

    /**
     * 需要聚合的属性名称（如：campus, classroom, college等）
     */
    @NotBlank(message = "属性名称不能为空")
    private String attributeName;
}

