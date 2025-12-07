package org.example.newcourseselectionsystem.interfaces;

import org.example.newcourseselectionsystem.application.dto.LoginResponse;
import org.example.newcourseselectionsystem.application.dto.RegisterResponse;
import org.example.newcourseselectionsystem.application.request.LoginRequest;
import org.example.newcourseselectionsystem.application.request.RegisterRequest;
import org.example.newcourseselectionsystem.application.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        // 验证验证码（如果提供了验证码）
        if (request.getCaptcha() != null && !request.getCaptcha().isEmpty()
                && request.getCaptchaToken() != null && !request.getCaptchaToken().isEmpty()) {

            String cachedCode = CaptchaCache.getAndRemove(request.getCaptchaToken());

            if (cachedCode == null) {
                LoginResponse errorResponse = LoginResponse.builder()
                        .success(false)
                        .message("验证码已过期，请刷新")
                        .build();
                return ResponseEntity.ok(errorResponse);
            }

            if (!cachedCode.equalsIgnoreCase(request.getCaptcha().trim())) {
                LoginResponse errorResponse = LoginResponse.builder()
                        .success(false)
                        .message("验证码错误")
                        .build();
                return ResponseEntity.ok(errorResponse);
            }
        }

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 学生注册
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Validated @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    public ResponseEntity<java.util.Map<String, String>> getCaptcha() {
        org.example.newcourseselectionsystem.application.util.CaptchaUtil.CaptchaResult captcha = org.example.newcourseselectionsystem.application.util.CaptchaUtil
                .generateCaptcha();

        // 生成验证码token（使用MD5加密验证码+时间戳）
        String code = captcha.getCode().toLowerCase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String token = org.springframework.util.DigestUtils.md5DigestAsHex(
                (code + ":" + timestamp).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 存储到内存缓存中（实际生产环境应该使用Redis）
        CaptchaCache.put(token, code, timestamp);

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("image", captcha.getImageBase64());
        result.put("token", token);

        return ResponseEntity.ok(result);
    }

    // 简单的内存缓存（生产环境应使用Redis）
    private static class CaptchaCache {
        private static final java.util.Map<String, CaptchaData> cache = new java.util.concurrent.ConcurrentHashMap<>();
        private static final long EXPIRE_TIME = 5 * 60 * 1000; // 5分钟过期

        static void put(String token, String code, String timestamp) {
            cache.put(token, new CaptchaData(code, Long.parseLong(timestamp)));
            // 清理过期数据
            cache.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue().timestamp > EXPIRE_TIME);
        }

        static String getAndRemove(String token) {
            CaptchaData data = cache.remove(token);
            if (data == null) {
                return null;
            }
            // 检查是否过期
            if (System.currentTimeMillis() - data.timestamp > EXPIRE_TIME) {
                return null;
            }
            return data.code;
        }

        static class CaptchaData {
            String code;
            long timestamp;

            CaptchaData(String code, long timestamp) {
                this.code = code;
                this.timestamp = timestamp;
            }
        }
    }

    /**
     * 登出（可选）
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("登出成功");
    }
}
