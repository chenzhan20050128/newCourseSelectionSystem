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

    private Long instructorId;

    private String campus;

    private String classroom;

    private Integer startWeek;

    private Integer endWeek;

    private List<CourseSessionDTO> sessions;
}
