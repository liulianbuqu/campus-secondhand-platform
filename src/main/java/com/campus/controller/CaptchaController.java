package com.campus.controller;

import com.campus.util.CaptchaUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 验证码控制器
 * 技术亮点：动态生成图形验证码，增强系统安全性
 */
@Controller
@RequestMapping("/captcha")
public class CaptchaController {

    /**
     * 生成验证码图片
     */
    @RequestMapping("/image")
    public void captcha(HttpServletResponse response, HttpSession session) throws IOException {
        // 生成验证码文本
        String code = CaptchaUtil.generateCode();
        
        // 存入 Session（用于登录时校验）
        session.setAttribute("captcha", code.toLowerCase());
        session.setAttribute("captchaTime", System.currentTimeMillis());
        
        // 生成并输出验证码图片
        CaptchaUtil.generateImage(code, response);
    }
}

