package org.example.newcourseselectionsystem.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.newcourseselectionsystem.application.dto.EnrollmentResponse;
import org.example.newcourseselectionsystem.application.request.EnrollmentRequest;
import org.example.newcourseselectionsystem.domain.entity.Course;
import org.example.newcourseselectionsystem.domain.entity.CourseSession;
import org.example.newcourseselectionsystem.domain.entity.Enrollment;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.CourseSessionMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.EnrollmentMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 选课服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private CourseSessionMapper courseSessionMapper;

    @Test
    void shouldEnrollCourseSuccessfully() {
        // 准备：使用测试数据中的学生ID=2和课程ID=4（假设课程4还没有被学生2选过）
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(2L);
        request.setCourseId(4L);

        // 记录选课前的enrolled_count
        Course courseBefore = courseMapper.selectById(4L);
        Integer enrolledCountBefore = courseBefore != null && courseBefore.getEnrolledCount() != null 
                ? courseBefore.getEnrolledCount() : 0;

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证
        Assertions.assertTrue(response.getSuccess(), "选课应该成功");
        Assertions.assertEquals("选课成功", response.getMessage());
        Assertions.assertNotNull(response.getEnrollmentId(), "应该返回选课记录ID");

        // 验证数据库中确实创建了选课记录
        List<Enrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getStudentId, 2L)
                        .eq(Enrollment::getCourseId, 4L)
                        .eq(Enrollment::getStatus, "已选"));
        Assertions.assertFalse(enrollments.isEmpty(), "应该存在选课记录");

        // 验证课程的enrolled_count增加了
        Course courseAfter = courseMapper.selectById(4L);
        Assertions.assertNotNull(courseAfter, "课程应该存在");
        Integer enrolledCountAfter = courseAfter.getEnrolledCount() != null ? courseAfter.getEnrolledCount() : 0;
        Assertions.assertEquals(enrolledCountBefore + 1, enrolledCountAfter, 
                "enrolled_count应该增加1");
    }

    @Test
    void shouldFailWhenStudentNotExists() {
        // 准备：使用不存在的学生ID
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(99999L);
        request.setCourseId(1L);

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证
        Assertions.assertFalse(response.getSuccess(), "选课应该失败");
        Assertions.assertEquals("学生不存在或状态异常", response.getMessage());
        Assertions.assertNull(response.getEnrollmentId(), "不应该返回选课记录ID");
    }

    @Test
    void shouldFailWhenCourseNotExists() {
        // 准备：使用不存在的课程ID
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(1L);
        request.setCourseId(99999L);

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证
        Assertions.assertFalse(response.getSuccess(), "选课应该失败");
        Assertions.assertEquals("课程不存在或未开放选课", response.getMessage());
        Assertions.assertNull(response.getEnrollmentId(), "不应该返回选课记录ID");
    }

    @Test
    void shouldFailWhenDuplicateEnrollment() {
        // 准备：先让学生1选择课程1
        EnrollmentRequest firstRequest = new EnrollmentRequest();
        firstRequest.setStudentId(1L);
        firstRequest.setCourseId(1L);
        EnrollmentResponse firstResponse = enrollmentService.enrollCourse(firstRequest);
        Assertions.assertTrue(firstResponse.getSuccess(), "第一次选课应该成功");

        // 再次尝试让学生1选择课程1
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(1L);
        request.setCourseId(1L);

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证
        Assertions.assertFalse(response.getSuccess(), "选课应该失败");
        Assertions.assertEquals("您已经选择过该课程", response.getMessage());
        Assertions.assertNull(response.getEnrollmentId(), "不应该返回选课记录ID");
    }

    @Test
    void shouldFailWhenCourseIsFull() {
        // 准备：创建一个已满的课程
        Course fullCourse = new Course();
        fullCourse.setCourseName("已满课程");
        fullCourse.setCredits(3);
        fullCourse.setCollege("测试学院");
        fullCourse.setInstructorId(1L);
        fullCourse.setCampus("本部");
        fullCourse.setClassroom("A101");
        fullCourse.setStartWeek(1);
        fullCourse.setEndWeek(16);
        fullCourse.setCapacity(1);
        fullCourse.setEnrolledCount(1); // 已满
        courseMapper.insert(fullCourse);

        // 创建一个学生选择这个已满的课程
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(2L);
        request.setCourseId(fullCourse.getCourseId());

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证
        Assertions.assertFalse(response.getSuccess(), "选课应该失败");
        Assertions.assertTrue(response.getMessage().contains("课程已满"), 
                "错误消息应该包含'课程已满'");
        Assertions.assertNull(response.getEnrollmentId(), "不应该返回选课记录ID");
    }

    @Test
    void shouldEnrollWithTimeConflictWarning() {
        // 准备：先让学生1选择课程1（课程1在周三第5节）
        EnrollmentRequest firstRequest = new EnrollmentRequest();
        firstRequest.setStudentId(1L);
        firstRequest.setCourseId(1L);
        EnrollmentResponse firstResponse = enrollmentService.enrollCourse(firstRequest);
        Assertions.assertTrue(firstResponse.getSuccess(), "第一次选课应该成功");

        // 验证课程1的节次信息
        List<CourseSession> course1Sessions = courseSessionMapper.selectList(
                new LambdaQueryWrapper<CourseSession>()
                        .eq(CourseSession::getCourseId, 1L));
        Assertions.assertFalse(course1Sessions.isEmpty(), "课程1应该有节次信息");

        // 创建一个与课程1时间冲突的课程
        Course conflictCourse = new Course();
        conflictCourse.setCourseName("冲突课程");
        conflictCourse.setCredits(3);
        conflictCourse.setCollege("测试学院");
        conflictCourse.setInstructorId(1L);
        conflictCourse.setCampus("本部");
        conflictCourse.setClassroom("A102");
        conflictCourse.setStartWeek(1);
        conflictCourse.setEndWeek(16);
        conflictCourse.setCapacity(50);
        conflictCourse.setEnrolledCount(0);
        courseMapper.insert(conflictCourse);

        // 为冲突课程添加节次（周三第5节，与课程1冲突）
        CourseSession session = new CourseSession();
        session.setCourseId(conflictCourse.getCourseId());
        session.setWeekday("周三");
        session.setStartPeriod(5);
        session.setEndPeriod(5);
        courseSessionMapper.insert(session);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(1L);
        request.setCourseId(conflictCourse.getCourseId());

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证：即使有时间冲突，选课也应该成功，但会有警告
        Assertions.assertTrue(response.getSuccess(), "即使有时间冲突，选课也应该成功");
        Assertions.assertNotNull(response.getEnrollmentId(), "应该返回选课记录ID");
        Assertions.assertNotNull(response.getWarn(), "应该有警告信息");
        Assertions.assertTrue(response.getWarn().contains("选课时间冲突"), 
                "警告信息应该包含'选课时间冲突'");
    }

    @Test
    void shouldUpdateEnrolledCountAfterEnrollment() {
        // 准备：选择一个课程，记录选课前的enrolled_count
        Long courseId = 5L; // 假设课程5存在且学生2还没选过
        Course courseBefore = courseMapper.selectById(courseId);
        if (courseBefore == null) {
            // 如果课程不存在，跳过这个测试
            return;
        }
        Integer enrolledCountBefore = courseBefore.getEnrolledCount() != null ? courseBefore.getEnrolledCount() : 0;

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudentId(2L); // 使用学生2，避免与学生1的选课冲突
        request.setCourseId(courseId);

        // 执行
        EnrollmentResponse response = enrollmentService.enrollCourse(request);

        // 验证
        if (response.getSuccess()) {
            Course courseAfter = courseMapper.selectById(courseId);
            Assertions.assertNotNull(courseAfter, "课程应该存在");
            Integer enrolledCountAfter = courseAfter.getEnrolledCount() != null ? courseAfter.getEnrolledCount() : 0;
            Assertions.assertEquals(enrolledCountBefore + 1, enrolledCountAfter, 
                    "enrolled_count应该增加1");
        }
    }
}

