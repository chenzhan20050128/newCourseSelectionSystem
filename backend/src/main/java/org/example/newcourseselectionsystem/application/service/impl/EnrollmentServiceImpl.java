package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.CourseSessionDTO;
import org.example.newcourseselectionsystem.application.dto.CourseWithSessionsDTO;
import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.DropCourseRequest;
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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

        // 3. 重复选课检查（在同一轮次内）
        LambdaQueryWrapper<Enrollment> enrollmentWrapper = new LambdaQueryWrapper<>();
        enrollmentWrapper.eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getBatchId, request.getBatchId())
                .eq(Enrollment::getStatus, "已选");
        Enrollment existingEnrollment = enrollmentMapper.selectOne(enrollmentWrapper);
        if (existingEnrollment != null) {
            return EnrollmentResponse.builder()
                    .success(false)
                    .message("您在当前轮次已经选择过该课程")
                    .build();
        }

        // 4. 课程容量限制（预选：允许超容量，返回提醒）
        Integer enrolledCount = course.getEnrolledCount() != null ? course.getEnrolledCount() : 0;
        Integer capacity = course.getCapacity();
        String warnMessage = null;
        if (capacity != null && capacity > 0 && enrolledCount >= capacity) {
            warnMessage = String.format("该课程选课人数已超过容量，抽签难度较大，请谨慎选择（当前：%d/%d）", enrolledCount, capacity);
        }

        // 5. 时间冲突检查（返回警告，不阻止选课）
        String timeConflictWarn = checkTimeConflict(studentId, courseId, course.getCourseName());
        if (warnMessage == null || warnMessage.trim().isEmpty()) {
            warnMessage = timeConflictWarn;
        } else if (timeConflictWarn != null && !timeConflictWarn.trim().isEmpty()) {
            warnMessage = warnMessage + "；" + timeConflictWarn;
        }

        // 6. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setBatchId(request.getBatchId());
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
                        String newCourseIdStr = courseId == null ? "" : String.format("%08d", courseId);
                        String conflictCourseIdStr = enrolledSession.getCourseId() == null ? "" : String.format("%08d", enrolledSession.getCourseId());
                        String safeNewCourseName = (courseName == null || courseName.trim().isEmpty()) ? "未知课程" : courseName;
                        return String.format(
                                "选课时间冲突：新选课程《%s》(课程号:%s) 与已选课程《%s》(课程号:%s) 在[%s]第%d-%d节冲突",
                                safeNewCourseName,
                                newCourseIdStr,
                                conflictCourseName,
                                conflictCourseIdStr,
                                enrolledSession.getWeekday(),
                                enrolledSession.getStartPeriod(),
                                enrolledSession.getEndPeriod()
                        );
                    }
                }
            }
        }

        return null;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public EnrollmentResponse dropCourse(DropCourseRequest request) {
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
                    .message("课程不存在")
                    .build();
        }

        // 3. 查询选课记录（必须存在且状态为"已选"）
        // 注意：一个课程可能有多个session，学生可能选了多个时间段
        LambdaQueryWrapper<Enrollment> enrollmentWrapper = new LambdaQueryWrapper<>();
        enrollmentWrapper.eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, "已选");
        List<Enrollment> enrollments = enrollmentMapper.selectList(enrollmentWrapper);
        
        if (CollectionUtils.isEmpty(enrollments)) {
            return EnrollmentResponse.builder()
                    .success(false)
                    .message("您尚未选择该课程或该课程已退选")
                    .build();
        }

        // 4. 批量更新所有选课记录状态为"已退选"
        int droppedCount = 0;
        for (Enrollment enrollment : enrollments) {
            enrollment.setStatus("已退选");
            enrollmentMapper.updateById(enrollment);
            droppedCount++;
        }

        // 5. 更新课程的已选人数（减去退选的数量，确保不会小于0）
        Integer enrolledCount = course.getEnrolledCount() != null ? course.getEnrolledCount() : 0;
        if (enrolledCount >= droppedCount) {
            course.setEnrolledCount(enrolledCount - droppedCount);
            courseMapper.updateById(course);
        } else if (enrolledCount > 0) {
            course.setEnrolledCount(0);
            courseMapper.updateById(course);
        }

        return EnrollmentResponse.builder()
                .success(true)
                .message("退课成功")
                .warn(null)
                .enrollmentId(enrollments.get(0).getEnrollmentId())
                .build();
    }

    @Override
    public List<CourseWithSessionsDTO> getStudentCourses(Long studentId) {
        if (studentId == null) {
            return Collections.emptyList();
        }

        // 1. 查询学生所有状态为"已选"的选课记录
        LambdaQueryWrapper<Enrollment> enrollmentWrapper = new LambdaQueryWrapper<>();
        enrollmentWrapper.eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getStatus, "已选");
        List<Enrollment> enrollments = enrollmentMapper.selectList(enrollmentWrapper);

        if (CollectionUtils.isEmpty(enrollments)) {
            return Collections.emptyList();
        }

        // 2. 获取课程ID列表
        List<Long> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查询课程信息
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().in(Course::getCourseId, courseIds));

        if (CollectionUtils.isEmpty(courses)) {
            return Collections.emptyList();
        }

        // 4. 加载节次信息
        Map<Long, List<CourseSessionDTO>> sessionGroup = loadSessions(courseIds);

        // 5. 组装返回结果
        return courses.stream()
                .map(course -> assembleCourseDTO(course, sessionGroup.get(course.getCourseId()),
                        course.getEnrolledCount() != null ? course.getEnrolledCount() : 0))
                .collect(Collectors.toList());
    }

    /**
     * 加载课程的节次信息
     */
    private Map<Long, List<CourseSessionDTO>> loadSessions(List<Long> courseIds) {
        if (CollectionUtils.isEmpty(courseIds)) {
            return Collections.emptyMap();
        }
        // 一次性批量加载节次信息，避免 N+1 查询
        return courseSessionMapper.selectList(
                new LambdaQueryWrapper<CourseSession>().in(CourseSession::getCourseId, courseIds)).stream()
                .collect(Collectors.groupingBy(
                        CourseSession::getCourseId,
                        Collectors.mapping(this::convertSession, Collectors.toList())));
    }

    /**
     * 转换节次实体为DTO
     */
    private CourseSessionDTO convertSession(CourseSession session) {
        CourseSessionDTO dto = new CourseSessionDTO();
        dto.setSessionId(session.getSessionId());
        dto.setWeekday(session.getWeekday());
        dto.setStartPeriod(session.getStartPeriod());
        dto.setEndPeriod(session.getEndPeriod());
        dto.setWeekType(session.getWeekType());
        return dto;
    }

    /**
     * 组装课程DTO
     */
    private CourseWithSessionsDTO assembleCourseDTO(Course course, List<CourseSessionDTO> sessions,
            Integer enrolledCount) {
        CourseWithSessionsDTO dto = new CourseWithSessionsDTO();
        BeanUtils.copyProperties(course, dto);
        dto.setSessions(sessions == null ? Collections.emptyList() : sessions);
        dto.setEnrolledCount(enrolledCount);
        return dto;
    }
}

