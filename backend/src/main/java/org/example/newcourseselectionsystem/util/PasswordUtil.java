package org.example.newcourseselectionsystem.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码工具类
 * 用于生成MD5加密密码
 */
public class PasswordUtil {

    /**
     * 使用MD5加密密码
     * 
     * @param password 原始密码
     * @return MD5加密后的密码
     */
    public static String encodePassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证密码
     * 
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encodePassword(rawPassword).equals(encodedPassword);
    }

    /**
     * 测试工具：生成常用密码的MD5值
     */
    public static void main(String[] args) {
        String[] passwords = { "123456", "password", "admin123", "12345678" };

        System.out.println("=== 密码MD5加密结果 ===");
        for (String password : passwords) {
            System.out.println("密码: " + password + " -> MD5: " + encodePassword(password));
        }
    }
}
