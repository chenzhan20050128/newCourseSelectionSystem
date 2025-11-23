package org.example.newcourseselectionsystem.application.dto;

import lombok.Data;

/**
 * 课程节次 DTO
 */
@Data
public class CourseSessionDTO {

    private Long sessionId;

    private String weekday;

    private Integer startPeriod;

    private Integer endPeriod;
}
