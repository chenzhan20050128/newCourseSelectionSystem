package org.example.newcourseselectionsystem.application.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

/**
 * 验证码工具类
 */
public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 48;
    private static final int CODE_LENGTH = 4;
    private static final String CODE_CHARS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    private static final Random RANDOM = new Random();

    /**
     * 生成验证码及图片
     */
    public static CaptchaResult generateCaptcha() {
        // 创建图片缓冲区
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 启用抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置背景色
        g.setColor(new Color(245, 245, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 生成验证码文本
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            String ch = String.valueOf(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            code.append(ch);

            // 随机颜色
            g.setColor(new Color(RANDOM.nextInt(120), RANDOM.nextInt(120), RANDOM.nextInt(120)));

            // 固定字体大小，避免过大导致超出边界
            g.setFont(new Font("Arial", Font.BOLD, 22));

            // 固定间距和固定y坐标，确保不会超出边界
            int x = 12 + i * 26;
            int y = 32; // 固定y坐标在中心位置

            // 极小的旋转角度，避免超出边界
            double angle = Math.toRadians(RANDOM.nextInt(10) - 5); // ±5度
            g.rotate(angle, x, y);
            g.drawString(ch, x, y);
            g.rotate(-angle, x, y);
        }

        // 绘制干扰线（减少干扰线数量）
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(RANDOM.nextInt(220) + 35, RANDOM.nextInt(220) + 35, RANDOM.nextInt(220) + 35));
            int x1 = RANDOM.nextInt(WIDTH);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = RANDOM.nextInt(WIDTH);
            int y2 = RANDOM.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 绘制噪点（减少噪点数量）
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255)));
            g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 1, 1);
        }

        g.dispose();

        // 转换为Base64
        String base64Image = imageToBase64(image);

        return new CaptchaResult(code.toString(), base64Image);
    }

    /**
     * 将图片转为Base64字符串
     */
    private static String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    /**
     * 验证码结果类
     */
    public static class CaptchaResult {
        private final String code;
        private final String imageBase64;

        public CaptchaResult(String code, String imageBase64) {
            this.code = code;
            this.imageBase64 = imageBase64;
        }

        public String getCode() {
            return code;
        }

        public String getImageBase64() {
            return imageBase64;
        }
    }
}
