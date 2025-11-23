package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

/**
 * 课程查询请求：允许根据课程表的任一字段过滤
 */
@Data
public class CourseQueryRequest {

    private Long courseId;

    private String courseName;

    private Integer credits;

    private String description;

    private String college;

    private Long instructorId;

    private String campus;

    private String classroom;

    private Integer startWeek;

    private Integer endWeek;
}

