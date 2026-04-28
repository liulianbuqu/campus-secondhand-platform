package com.campus.controller;

import com.campus.annotation.Log;
import com.campus.entity.User;
import com.campus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 验证码有效期（5分钟）
    private static final long CAPTCHA_EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 跳转到登录页
     */
    @RequestMapping("/loginPage")
    public String loginPage() {
        return "user/login";
    }

    /**
     * 跳转到注册页
     */
    @RequestMapping("/registerPage")
    public String registerPage() {
        return "user/register";
    }

    /**
     * 用户注册
     */
    @Log("用户注册")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> register(User user, String captcha, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 验证码校验（可选，注册时也可以不要验证码）
        // 如果需要注册验证码，取消下面的注释
        /*
        if (!validateCaptcha(captcha, session)) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }
        */

        boolean success = userService.register(user);
        result.put("success", success);
        if (!success) {
            result.put("message", "用户名已存在");
        }
        return result;
    }

    /**
     * 用户登录
     * 技术亮点：增加验证码校验，防止暴力破解
     */
    @Log("用户登录")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> login(String username, String password, String captcha, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 验证码校验
        if (!validateCaptcha(captcha, session)) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        User user = userService.login(username, password);
        if (user != null) {
            session.setAttribute("user", user);
            result.put("success", true);
            result.put("isAdmin", user.getRole() == 2);
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    /**
     * 校验验证码
     */
    private boolean validateCaptcha(String inputCaptcha, HttpSession session) {
        if (inputCaptcha == null || inputCaptcha.isEmpty()) {
            return false;
        }

        String sessionCaptcha = (String) session.getAttribute("captcha");
        Long captchaTime = (Long) session.getAttribute("captchaTime");

        // 验证码不存在
        if (sessionCaptcha == null || captchaTime == null) {
            return false;
        }

        // 验证码已过期
        if (System.currentTimeMillis() - captchaTime > CAPTCHA_EXPIRE_TIME) {
            session.removeAttribute("captcha");
            session.removeAttribute("captchaTime");
            return false;
        }

        // 校验验证码（忽略大小写）
        boolean valid = sessionCaptcha.equalsIgnoreCase(inputCaptcha.trim());

        // 验证成功后清除验证码（一次性使用）
        if (valid) {
            session.removeAttribute("captcha");
            session.removeAttribute("captchaTime");
        }

        return valid;
    }

    /**
     * 退出登录
     */
    @Log("退出登录")
    @RequestMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /**
     * 个人中心
     */
    @RequestMapping("/center")
    public String center() {
        return "user/center";
    }

    /**
     * 更新用户信息
     */
    @Log("更新个人信息")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> update(User user, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User sessionUser = (User) session.getAttribute("user");
        user.setId(sessionUser.getId());
        boolean success = userService.update(user);
        if (success) {
            // 更新session中的用户信息
            User updatedUser = userService.findById(user.getId());
            session.setAttribute("user", updatedUser);
        }
        result.put("success", success);
        return result;
    }
}
