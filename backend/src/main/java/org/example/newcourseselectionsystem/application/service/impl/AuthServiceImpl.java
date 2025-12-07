package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.LoginResponse;
import org.example.newcourseselectionsystem.application.dto.RegisterResponse;
import org.example.newcourseselectionsystem.application.dto.UserDTO;
import org.example.newcourseselectionsystem.application.request.LoginRequest;
import org.example.newcourseselectionsystem.application.request.RegisterRequest;
import org.example.newcourseselectionsystem.application.service.AuthService;
import org.example.newcourseselectionsystem.domain.entity.Instructor;
import org.example.newcourseselectionsystem.domain.entity.Student;
import org.example.newcourseselectionsystem.infrastructure.mapper.InstructorMapper;
import org.example.newcourseselectionsystem.infrastructure.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentMapper studentMapper;
    private final InstructorMapper instructorMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        String userType = request.getUserType();

        if ("student".equalsIgnoreCase(userType)) {
            return loginStudent(request);
        } else if ("instructor".equalsIgnoreCase(userType)) {
            return loginInstructor(request);
        } else {
            return LoginResponse.builder()
                    .success(false)
                    .message("无效的用户类型")
                    .build();
        }
    }

    /**
     * 学生登录
     */
    private LoginResponse loginStudent(LoginRequest request) {
        // 查询学生（按学号查询）
        Student student = findStudent(request.getUsername());

        if (student == null) {
            return LoginResponse.builder()
                    .success(false)
                    .message("学生不存在")
                    .build();
        }

        // 验证密码
        if (!validatePassword(request.getPassword(), student.getPassword())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("密码错误")
                    .build();
        }

        // 登录成功，返回用户信息
        UserDTO userDTO = UserDTO.builder()
                .userId(student.getStudentId())
                .username(student.getStudentName())
                .userType("student")
                .college(student.getCollege())
                .build();

        return LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .user(userDTO)
                .token(generateToken(student.getStudentId(), "student"))
                .build();
    }

    /**
     * 教师登录
     */
    private LoginResponse loginInstructor(LoginRequest request) {
        // 查询教师（username可以是instructor_id或instructor_name）
        Instructor instructor = findInstructor(request.getUsername());

        if (instructor == null) {
            return LoginResponse.builder()
                    .success(false)
                    .message("教师不存在")
                    .build();
        }

        // 验证密码
        if (!validatePassword(request.getPassword(), instructor.getPassword())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("密码错误")
                    .build();
        }

        // 登录成功，返回用户信息
        UserDTO userDTO = UserDTO.builder()
                .userId(instructor.getInstructorId())
                .username(instructor.getInstructorName())
                .userType("instructor")
                .college(instructor.getCollege())
                .build();

        return LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .user(userDTO)
                .token(generateToken(instructor.getInstructorId(), "instructor"))
                .build();
    }

    /**
     * 查找学生（支持按ID或姓名查询）
     */
    private Student findStudent(String username) {
        if (StringUtils.isEmpty(username)) {
            return null;
        }

        // 尝试按ID查询
        try {
            Long studentId = Long.parseLong(username);
            Student student = studentMapper.selectById(studentId);
            if (student != null) {
                return student;
            }
        } catch (NumberFormatException ignored) {
            // 不是数字，继续尝试按姓名查询
        }

        // 按姓名查询
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentName, username);
        return studentMapper.selectOne(wrapper);
    }

    /**
     * 查找教师（支持按ID或姓名查询）
     */
    private Instructor findInstructor(String username) {
        if (StringUtils.isEmpty(username)) {
            return null;
        }

        // 尝试按ID查询
        try {
            Long instructorId = Long.parseLong(username);
            Instructor instructor = instructorMapper.selectById(instructorId);
            if (instructor != null) {
                return instructor;
            }
        } catch (NumberFormatException ignored) {
            // 不是数字，继续尝试按姓名查询
        }

        // 按姓名查询
        LambdaQueryWrapper<Instructor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Instructor::getInstructorName, username);
        return instructorMapper.selectOne(wrapper);
    }

    /**
     * 验证密码
     */
    private boolean validatePassword(String rawPassword, String encodedPassword) {
        if (StringUtils.isEmpty(rawPassword) || StringUtils.isEmpty(encodedPassword)) {
            return false;
        }
        String encoded = encodePassword(rawPassword);
        return encoded.equals(encodedPassword);
    }

    /**
     * 密码加密（使用MD5，实际生产环境应该使用更安全的算法如BCrypt）
     */
    private String encodePassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成简单的token（实际生产环境应该使用JWT）
     */
    private String generateToken(Long userId, String userType) {
        String rawToken = userId + ":" + userType + ":" + System.currentTimeMillis();
        return DigestUtils.md5DigestAsHex(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        // 验证两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("两次输入的密码不一致")
                    .build();
        }

        // 检查学号是否已存在
        try {
            Long studentId = Long.parseLong(request.getStudentId());
            Student existingStudentById = studentMapper.selectById(studentId);
            if (existingStudentById != null) {
                return RegisterResponse.builder()
                        .success(false)
                        .message("该学号已注册")
                        .build();
            }
        } catch (NumberFormatException e) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("学号必须是数字")
                    .build();
        }

        // 创建新学生
        Student student = new Student();
        student.setStudentId(Long.parseLong(request.getStudentId()));
        student.setStudentName(request.getStudentName());
        student.setPassword(encodePassword(request.getPassword()));
        student.setCollege(request.getCollege());
        student.setBirthDate(request.getBirthDate() != null ? request.getBirthDate() : LocalDate.of(2000, 1, 1));

        // 插入数据库
        int result = studentMapper.insert(student);

        if (result > 0) {
            return RegisterResponse.builder()
                    .success(true)
                    .message("注册成功")
                    .studentId(student.getStudentId())
                    .studentName(student.getStudentName())
                    .build();
        } else {
            return RegisterResponse.builder()
                    .success(false)
                    .message("注册失败，请稍后重试")
                    .build();
        }
    }
}
