package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.EnrollmentRequest;
import org.example.newcourseselectionsystem.application.service.EnrollmentService;
import org.example.newcourseselectionsystem.domain.entity.Course;
import org.example.newcourseselectionsystem.domain.entity.CourseSession;
import org.example.newcourseselectionsystem.domain.entity.Enrollment;
import org.example.newcourseselectionsystem.domain.entity.Student;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseSessionMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.EnrollmentMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 选课服务实现
 */
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseSessionMapper courseSessionMapper;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public EnrollmentResponse enrollCourse(EnrollmentRequest request) {
        Long studentId = request.getStudentId();
        Long courseId = request.getCourseId();

        // 1. 学生身份验证
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return EnrollmentResponse.builder()
                    .success(false)
                    .message("学生不存在或状态异常")
                    .build();
        }

        // 2. 课程存在性验证
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return EnrollmentResponse.builder()
                    .success(false)
                    .message("课程不存在或未开放选课")
                    .build();
        }

        // 3. 重复选课检查
        LambdaQueryWrapper<Enrollment> enrollmentWrapper = new LambdaQueryWrapper<>();
        enrollmentWrapper.eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, "已选");
        Enrollment existingEnrollment = enrollmentMapper.selectOne(enrollmentWrapper);
        if (existingEnrollment != null) {
            return EnrollmentResponse.builder()
                    .success(false)
                    .message("您已经选择过该课程")
                    .build();
        }

        // 4. 课程容量限制（硬性条件）
        Integer enrolledCount = course.getEnrolledCount() != null ? course.getEnrolledCount() : 0;
        Integer capacity = course.getCapacity();
        if (enrolledCount >= capacity) {
            return EnrollmentResponse.builder()
                    .success(false)
                    .message(String.format("课程已满，当前选课人数：%d/%d", enrolledCount, capacity))
                    .build();
        }

        // 5. 时间冲突检查（返回警告，不阻止选课）
        String warnMessage = checkTimeConflict(studentId, courseId, course.getCourseName());

        // 6. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus("已选");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentMapper.insert(enrollment);

        // 7. 更新课程的已选人数
        course.setEnrolledCount(enrolledCount + 1);
        courseMapper.updateById(course);

        return EnrollmentResponse.builder()
                .success(true)
                .message("选课成功")
                .warn(warnMessage)
                .enrollmentId(enrollment.getEnrollmentId())
                .build();
    }

    /**
     * 检查时间冲突
     *
     * @param studentId  学生ID
     * @param courseId   新选课程ID
     * @param courseName 新选课程名称
     * @return 冲突警告信息，如果没有冲突返回null
     */
    private String checkTimeConflict(Long studentId, Long courseId, String courseName) {
        // 1. 查询学生所有状态为"已选"的选课记录，获取course_id列表
        LambdaQueryWrapper<Enrollment> enrollmentWrapper = new LambdaQueryWrapper<>();
        enrollmentWrapper.eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getStatus, "已选");
        List<Enrollment> enrolledCourses = enrollmentMapper.selectList(enrollmentWrapper);
        List<Long> enrolledCourseIds = enrolledCourses.stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        if (enrolledCourseIds.isEmpty()) {
            return null;
        }

        // 2. 查询这些课程的所有course_sessions记录
        LambdaQueryWrapper<CourseSession> enrolledSessionsWrapper = new LambdaQueryWrapper<>();
        enrolledSessionsWrapper.in(CourseSession::getCourseId, enrolledCourseIds);
        List<CourseSession> enrolledSessions = courseSessionMapper.selectList(enrolledSessionsWrapper);

        // 3. 查询新选课程的所有course_sessions记录
        LambdaQueryWrapper<CourseSession> newCourseSessionsWrapper = new LambdaQueryWrapper<>();
        newCourseSessionsWrapper.eq(CourseSession::getCourseId, courseId);
        List<CourseSession> newCourseSessions = courseSessionMapper.selectList(newCourseSessionsWrapper);

        // 4. 比较节次是否冲突
        for (CourseSession newSession : newCourseSessions) {
            for (CourseSession enrolledSession : enrolledSessions) {
                // 冲突条件：同一weekday（星期），且时间段有重叠
                if (newSession.getWeekday().equals(enrolledSession.getWeekday())) {
                    // 时间段重叠判断：(new_start <= old_end) && (new_end >= old_start)
                    if (newSession.getStartPeriod() <= enrolledSession.getEndPeriod() &&
                            newSession.getEndPeriod() >= enrolledSession.getStartPeriod()) {
                        // 需要获取冲突的课程名称
                        Course conflictCourse = courseMapper.selectById(enrolledSession.getCourseId());
                        String conflictCourseName = conflictCourse != null ? conflictCourse.getCourseName() : "未知课程";
                        return String.format("选课时间冲突：与课程[%s]在[%s]第%d-%d节冲突",
                                conflictCourseName,
                                enrolledSession.getWeekday(),
                                enrolledSession.getStartPeriod(),
                                enrolledSession.getEndPeriod());
                    }
                }
            }
        }

        return null;
    }
}

