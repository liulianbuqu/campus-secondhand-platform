package com.campus.controller;

import com.campus.util.CaptchaUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证码控制器
 * 技术亮点：动态生成图形验证码，增强系统安全性
 */
@Controller
@RequestMapping("/captcha")
public class CaptchaController {
    // 与 UserController 保持一致：验证码 5 分钟过期
    private static final long CAPTCHA_EXPIRE_TIME = 5 * 60 * 1000L;
    private static final int MAX_CAPTCHA_CACHE_SIZE = 6;

    /**
     * 生成验证码图片
     */
    @RequestMapping("/image")
    public void captcha(HttpServletResponse response, HttpSession session) throws IOException {
        // 生成验证码文本
        String code = CaptchaUtil.generateCode();
        String normalizedCode = code.toLowerCase();
        long now = System.currentTimeMillis();
        
        // 存入 Session（用于登录时校验）
        // 兼容旧逻辑：保留当前验证码字段
        session.setAttribute("captcha", normalizedCode);
        session.setAttribute("captchaTime", now);

        // 新逻辑：缓存最近几次验证码，避免浏览器额外请求导致用户看到的验证码失效
        LinkedHashMap<String, Long> captchaCache = readCaptchaCache(session.getAttribute("captchaCache"));

        // 清理过期项
        Iterator<Map.Entry<String, Long>> iterator = captchaCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long createTime = entry.getValue();
            if (createTime == null || now - createTime > CAPTCHA_EXPIRE_TIME) {
                iterator.remove();
            }
        }

        captchaCache.put(normalizedCode, now);
        while (captchaCache.size() > MAX_CAPTCHA_CACHE_SIZE) {
            String firstKey = captchaCache.keySet().iterator().next();
            captchaCache.remove(firstKey);
        }
        session.setAttribute("captchaCache", captchaCache);
        
        // 生成并输出验证码图片
        CaptchaUtil.generateImage(code, response);
    }

    private LinkedHashMap<String, Long> readCaptchaCache(Object cachedObj) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        if (!(cachedObj instanceof Map)) {
            return result;
        }
        Map<?, ?> rawMap = (Map<?, ?>) cachedObj;
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null || !(entry.getValue() instanceof Number)) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()).toLowerCase(), ((Number) entry.getValue()).longValue());
        }
        return result;
    }
}

