package org.example.newcourseselectionsystem.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.newcourseselectionsystem.application.dto.LoginResponse;
import org.example.newcourseselectionsystem.application.dto.RegisterResponse;
import org.example.newcourseselectionsystem.application.dto.UserDTO;
import org.example.newcourseselectionsystem.application.request.LoginRequest;
import org.example.newcourseselectionsystem.application.request.RegisterRequest;
import org.example.newcourseselectionsystem.application.service.AuthService;
import org.example.newcourseselectionsystem.domain.entity.Student;
import org.example.newcourseselectionsystem.infrastructure.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 认证服务实现（移除教师相关逻辑）
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentMapper studentMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 仅支持学生登录
        return loginStudent(request);
    }

    /**
     * 学生登录
     */
    private LoginResponse loginStudent(LoginRequest request) {
        Student student = findStudent(request.getUsername());
        if (student == null) {
            return LoginResponse.builder()
                    .success(false)
                    .message("学生不存在")
                    .build();
        }
        if (!validatePassword(request.getPassword(), student.getPassword())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("密码错误")
                    .build();
        }
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
     * 查找学生（支持按ID或姓名查询）
     */
    private Student findStudent(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        try {
            Long studentId = Long.parseLong(username);
            Student byId = studentMapper.selectById(studentId);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentName, username);
        return studentMapper.selectOne(wrapper);
    }

    /**
     * 验证密码
     */
    private boolean validatePassword(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        return md5.equals(encodedPassword);
    }

    /**
     * 简单的MD5编码（仅用于示例）
     */
    private String encodePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            return "";
        }
        return DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
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
        // 简化：仅学生注册或按现有实现保留
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("两次输入的密码不一致")
                    .build();
        }
        try {
            Long studentId = Long.parseLong(request.getStudentId());
            Student existingStudentById = studentMapper.selectById(studentId);
            if (existingStudentById != null) {
                return RegisterResponse.builder()
                        .success(false)
                        .message("该学号已注册")
                        .build();
            }
            Student student = new Student();
            student.setStudentId(studentId);
            student.setStudentName(request.getStudentName());
            student.setPassword(encodePassword(request.getPassword()));
            student.setCollege(request.getCollege());
            student.setBirthDate(request.getBirthDate() != null ? request.getBirthDate() : LocalDate.of(2000, 1, 1));
            int result = studentMapper.insert(student);
            if (result > 0) {
                return RegisterResponse.builder()
                        .success(true)
                        .message("注册成功")
                        .studentId(student.getStudentId())
                        .studentName(student.getStudentName())
                        .build();
            }
        } catch (NumberFormatException e) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("学号必须是数字")
                    .build();
        }
        return RegisterResponse.builder()
                .success(false)
                .message("注册失败，请稍后重试")
                .build();
    }
}
