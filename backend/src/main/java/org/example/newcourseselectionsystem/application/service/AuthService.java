package org.example.newcourseselectionsystem.application.service;

import org.example.newcourseselectionsystem.application.dto.LoginResponse;
import org.example.newcourseselectionsystem.application.dto.RegisterResponse;
import org.example.newcourseselectionsystem.application.request.LoginRequest;
import org.example.newcourseselectionsystem.application.request.RegisterRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * 
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    LoginResponse login(LoginRequest request);

    /**
     * 学生注册
     * 
     * @param request 注册请求参数
     * @return 注册响应结果
     */
    RegisterResponse register(RegisterRequest request);
}
