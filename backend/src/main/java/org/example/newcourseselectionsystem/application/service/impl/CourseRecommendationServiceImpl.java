package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.request.CourseRecommendationRequest;
import org.example.newcourseselectionsystem.application.service.CourseRecommendationService;
import org.example.newcourseselectionsystem.application.service.EnrollmentService;
import org.example.newcourseselectionsystem.domain.entity.Course;
import org.example.newcourseselectionsystem.domain.entity.Student;
import org.example.newcourseselectionsystem.infrastructure.client.DeepSeekApiClient;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 课程推荐服务实现
 */
@Service
@RequiredArgsConstructor
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentService enrollmentService;
    private final DeepSeekApiClient deepSeekApiClient;

    @Override
    public Flux<String> getRecommendationStream(CourseRecommendationRequest request) {
        try {
            Long studentId = request.getStudentId();
            String userPrompt = request.getPrompt();

            // 1. 获取学生基本信息
            Student student = studentMapper.selectById(studentId);
            if (student == null) {
                return Flux.just("错误: 学生不存在");
            }

            // 2. 获取全量课程
            List<Course> allCourses = courseMapper.selectList(new LambdaQueryWrapper<>());

            // 3. 获取学生已选课程
            List<CourseWithSessionsDTO> enrolledCourses = enrollmentService.getStudentCourses(studentId);

            // 4. 构建背景信息字符串
            String backgroundInfo = buildBackgroundInfo(student, allCourses, enrolledCourses);

            // 5. 构建消息列表
            List<DeepSeekApiClient.Message> messages = new java.util.ArrayList<>();
            messages.add(new DeepSeekApiClient.Message("system",
                    "你是一个专业的课程推荐助手，能够根据学生的需求和背景信息推荐合适的课程。"));
            messages.add(new DeepSeekApiClient.Message("user", backgroundInfo + "\n\n" + userPrompt));

            // 6. 调用 DeepSeek API 流式接口
            return deepSeekApiClient.chatStream(messages);

        } catch (Exception e) {
            return Flux.just("错误: " + e.getMessage());
        }
    }

    /**
     * 构建背景信息字符串
     */
    private String buildBackgroundInfo(Student student, List<Course> allCourses,
            List<CourseWithSessionsDTO> enrolledCourses) {
        StringBuilder sb = new StringBuilder();

        // 学生基本信息
        sb.append("学生基本信息：\n");
        sb.append("姓名：").append(student.getStudentName()).append("\n");
        sb.append("学号：").append(student.getStudentId()).append("\n");
        sb.append("学院：").append(student.getCollege()).append("\n");
        sb.append("\n");

        // 已选课程信息
        sb.append("已选课程列表：\n");
        if (CollectionUtils.isEmpty(enrolledCourses)) {
            sb.append("暂无已选课程\n");
        } else {
            for (CourseWithSessionsDTO course : enrolledCourses) {
                sb.append("- 课程ID：").append(course.getCourseId())
                        .append("，课程名称：").append(course.getCourseName())
                        .append("，学分：").append(course.getCredits())
                        .append("，描述：").append(course.getDescription() != null ? course.getDescription() : "无")
                        .append("，学院：").append(course.getCollege())
                        .append("\n");
            }
        }
        sb.append("\n");

        // 全量课程信息
        sb.append("所有可选课程列表：\n");
        if (CollectionUtils.isEmpty(allCourses)) {
            sb.append("暂无可选课程\n");
        } else {
            for (Course course : allCourses) {
                sb.append("- 课程ID：").append(course.getCourseId())
                        .append("，课程名称：").append(course.getCourseName())
                        .append("，学分：").append(course.getCredits())
                        .append("，描述：").append(course.getDescription() != null ? course.getDescription() : "无")
                        .append("，学院：").append(course.getCollege())
                        .append("，容量：").append(course.getCapacity())
                        .append("，已选人数：").append(course.getEnrolledCount() != null ? course.getEnrolledCount() : 0)
                        .append("\n");
            }
        }

        return sb.toString();
    }
}
