package org.example.newcourseselectionsystem.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CourseRecommendationResponse {
    private Long studentId;
    private Map<String, List<CourseWithSessionsDTO>> recommendations;
}

