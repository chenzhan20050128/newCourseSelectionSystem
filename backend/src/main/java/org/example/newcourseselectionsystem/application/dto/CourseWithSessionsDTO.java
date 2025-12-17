package org.example.newcourseselectionsystem.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 携带节次信息的课程 DTO
 */
@Data
public class CourseWithSessionsDTO {

    private Long courseId;

    private String courseName;

    private Integer credits;

    private String description;

    private String college;

    private String instructorName;

    private String campus;

    private String classroom;

    private Integer startWeek;

    private Integer endWeek;

    private Integer capacity;

    private Integer enrolledCount;

    private String type;

    private List<CourseSessionDTO> sessions;

    /**
     * 是否已被指定学生选中（仅在查询时提供了 studentId 时才有意义）
     */
    private Boolean isEnrolled;
}
