package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "用户类型不能为空")
    private String userType; // "student" 或 "instructor"

    private String captcha; // 验证码（可选）

    private String captchaToken; // 验证码token（可选）
}
