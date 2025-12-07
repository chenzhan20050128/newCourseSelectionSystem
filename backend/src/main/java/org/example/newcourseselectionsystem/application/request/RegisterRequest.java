package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 学生注册请求
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotBlank(message = "学生姓名不能为空")
    private String studentName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "学院不能为空")
    private String college;

    private LocalDate birthDate;
}
