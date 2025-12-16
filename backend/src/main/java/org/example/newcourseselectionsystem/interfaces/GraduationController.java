package org.example.newcourseselectionsystem.interfaces;

import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseRecommendationResponse;
import org.example.newcourseselectionsystem.application.dto.GraduationStatusResponse;
import org.example.newcourseselectionsystem.application.request.GraduationQueryRequest;
import org.example.newcourseselectionsystem.application.request.GraduationRecommendationRequest;
import org.example.newcourseselectionsystem.application.service.GraduationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/graduation")
@Validated
@RequiredArgsConstructor
public class GraduationController {

    private final GraduationService graduationService;

    @PostMapping("/status")
    public GraduationStatusResponse status(@Valid @RequestBody GraduationQueryRequest request) {
        return graduationService.getStatus(request);
    }

    @PostMapping("/recommendations")
    public CourseRecommendationResponse recommendations(@Valid @RequestBody GraduationRecommendationRequest request) {
        return graduationService.recommend(request);
    }
}

