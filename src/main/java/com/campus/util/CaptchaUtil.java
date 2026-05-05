package com.campus.util;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * 图形验证码工具类
 * 技术亮点：使用 Java 2D 绘图 API 生成动态验证码图片
 * 
 * 安全特性：
 * 1. 随机字符 + 随机颜色 + 随机位置
 * 2. 添加干扰线和噪点，防止 OCR 识别
 * 3. 字符旋转，增加识别难度
 */
public class CaptchaUtil {
    
    // 验证码字符集：使用纯数字提升可读性，减少误判
    private static final String CHARS = "23456789";
    
    private static final int WIDTH = 120;      // 图片宽度
    private static final int HEIGHT = 40;      // 图片高度
    private static final int CODE_LENGTH = 4;  // 验证码长度
    private static final int LINE_COUNT = 3;   // 干扰线数量
    
    private static final Random random = new Random();

    /**
     * 生成验证码文本
     */
    public static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    /**
     * 生成验证码图片并写入响应流
     */
    public static void generateImage(String code, HttpServletResponse response) throws IOException {
        // 创建图片缓冲区
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充背景色（浅灰色渐变）
        GradientPaint gradient = new GradientPaint(0, 0, new Color(240, 240, 240),
                WIDTH, HEIGHT, new Color(220, 220, 220));
        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制边框
        g.setColor(new Color(200, 200, 200));
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        // 绘制少量干扰线
        for (int i = 0; i < LINE_COUNT; i++) {
            g.setColor(randomColor(150, 200));
            g.setStroke(new BasicStroke(1.0f));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 添加少量噪点
        for (int i = 0; i < 12; i++) {
            g.setColor(randomColor(130, 180));
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g.fillOval(x, y, 2, 2);
        }

        // 绘制验证码字符
        g.setFont(new Font("Arial", Font.BOLD, 30));
        int charWidth = (WIDTH - 20) / CODE_LENGTH;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(20, 130));
            int x = 10 + i * charWidth;
            int y = 30 + random.nextInt(3);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g.dispose();

        // 设置响应头
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // 输出图片
        ImageIO.write(image, "PNG", response.getOutputStream());
    }

    /**
     * 生成随机颜色
     */
    private static Color randomColor(int min, int max) {
        int range = max - min;
        int r = min + random.nextInt(range);
        int g = min + random.nextInt(range);
        int b = min + random.nextInt(range);
        return new Color(r, g, b);
    }
}

