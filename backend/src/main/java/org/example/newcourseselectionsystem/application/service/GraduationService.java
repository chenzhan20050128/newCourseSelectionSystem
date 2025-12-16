package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.CourseRecommendationResponse;
import org.example.newcourseselectionsystem.application.dto.GraduationStatusResponse;
import org.example.newcourseselectionsystem.application.request.GraduationQueryRequest;
import org.example.newcourseselectionsystem.application.request.GraduationRecommendationRequest;

public interface GraduationService {
    GraduationStatusResponse getStatus(GraduationQueryRequest request);
    CourseRecommendationResponse recommend(GraduationRecommendationRequest request);
}

