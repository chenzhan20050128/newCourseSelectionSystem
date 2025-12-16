package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseRecommendationResponse;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.dto.CreditProgressDTO;
import org.example.newcourseselectionsystem.application.dto.GraduationStatusResponse;
import org.example.newcourseselectionsystem.application.dto.TypeCreditDeficitDTO;
import org.example.newcourseselectionsystem.application.request.GraduationQueryRequest;
import org.example.newcourseselectionsystem.application.request.GraduationRecommendationRequest;
import org.example.newcourseselectionsystem.application.service.CourseService;
import org.example.newcourseselectionsystem.application.service.EnrollmentService;
import org.example.newcourseselectionsystem.application.service.GraduationService;
import org.example.newcourseselectionsystem.application.util.GraduationRequirements;
import org.example.newcourseselectionsystem.domain.entity.Course;
import org.example.newcourseselectionsystem.domain.entity.Enrollment;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.EnrollmentMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GraduationServiceImpl implements GraduationService {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentService enrollmentService; // reuse to assemble CourseWithSessionsDTO
    private final CourseService courseService;

    @Override
    public GraduationStatusResponse getStatus(GraduationQueryRequest request) {
        Long studentId = request.getStudentId();
        List<Enrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getStatus, "已修"));
        // Treat all entries in enrollments as earned credits per user instruction
        Set<Long> courseIds = enrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            List<TypeCreditDeficitDTO> deficits = GraduationRequirements.CREDIT_REQUIREMENTS.entrySet().stream()
                    .map(e -> new TypeCreditDeficitDTO(e.getKey(), e.getValue(), 0, e.getValue()))
                    .collect(Collectors.toList());
            int totalRequired = GraduationRequirements.CREDIT_REQUIREMENTS.values().stream().mapToInt(Integer::intValue).sum();
            return new GraduationStatusResponse(studentId, deficits, totalRequired, 0);
        }
        List<Course> courses = courseMapper.selectBatchIds(courseIds);
        Map<String, Integer> earnedByType = new HashMap<>();
        for (Course c : courses) {
            String type = c.getType();
            Integer credits = c.getCredits();
            if (type == null || credits == null) continue; // skip uncategorized
            earnedByType.merge(type, credits, Integer::sum);
        }
        List<TypeCreditDeficitDTO> deficits = new ArrayList<>();
        int totalEarned = 0;
        for (Map.Entry<String, Integer> e : GraduationRequirements.CREDIT_REQUIREMENTS.entrySet()) {
            String type = e.getKey();
            int required = e.getValue();
            int earned = earnedByType.getOrDefault(type, 0);
            int remaining = Math.max(required - earned, 0);
            deficits.add(new TypeCreditDeficitDTO(type, required, earned, remaining));
            totalEarned += earned;
        }
        int totalRequired = GraduationRequirements.CREDIT_REQUIREMENTS.values().stream().mapToInt(Integer::intValue).sum();
        return new GraduationStatusResponse(studentId, deficits, totalRequired, totalEarned);
    }

    @Override
    public CourseRecommendationResponse recommend(GraduationRecommendationRequest request) {
        GraduationStatusResponse status = getStatus(new GraduationQueryRequest(){ { setStudentId(request.getStudentId()); } });
        // Sort deficits: biggest remaining first
        List<TypeCreditDeficitDTO> sorted = new ArrayList<>(status.getDeficits());
        sorted.sort(Comparator.comparingInt(TypeCreditDeficitDTO::getRemainingCredits).reversed());

        Map<String, List<CourseWithSessionsDTO>> recs = new LinkedHashMap<>();
        Set<Long> alreadyCourseIds = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, request.getStudentId())
                .in(Enrollment::getStatus, Arrays.asList("已修", "已选")))
                .stream().map(Enrollment::getCourseId).collect(Collectors.toSet());

        Random rand = new Random();
        for (TypeCreditDeficitDTO d : sorted) {
            if (d.getRemainingCredits() <= 0) continue;
            // pull candidate IDs of this type
            List<Course> candidates = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getType, d.getType()));
            List<Long> candidateIds = candidates.stream()
                    .map(Course::getCourseId)
                    .filter(id -> !alreadyCourseIds.contains(id))
                    .collect(Collectors.toList());
            // introduce slight randomness: shuffle IDs within type
            Collections.shuffle(candidateIds, rand);
            // limit to top N IDs (e.g., 5)
            if (candidateIds.size() > 5) {
                candidateIds = candidateIds.subList(0, 5);
            }
            // convert to DTOs via CourseService
            List<CourseWithSessionsDTO> dtos = courseService.listCoursesByIds(candidateIds);
            recs.put(d.getType(), dtos);
        }

        // 构建学分进度列表
        List<CreditProgressDTO> creditProgress = new ArrayList<>();
        for (TypeCreditDeficitDTO d : status.getDeficits()) {
            creditProgress.add(new CreditProgressDTO(d.getType(), d.getEarnedCredits(), d.getRequiredCredits()));
        }

        return new CourseRecommendationResponse(request.getStudentId(), recs, creditProgress);
    }
}
