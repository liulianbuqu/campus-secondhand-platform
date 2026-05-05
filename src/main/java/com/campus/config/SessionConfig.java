package com.campus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * 分布式 Session 配置
 * 成员1：使用 Redis 存储 Session，实现多实例间登录状态共享
 * 
 * 工作原理：
 * 1. 用户登录后，Session 数据存储在 Redis 中，而非单个 Tomcat 的内存
 * 2. 所有 Tomcat 实例都连接同一个 Redis，读取同一个 Session
 * 3. 用户在实例A登录 → Redis 存储 Session → 实例B也能读取到
 * 
 * 验收方法：
 * 1. 启动两个 Tomcat 实例（8080 和 8081）
 * 2. 在 8080 登录 → 访问 8081 的首页 → 依然显示已登录状态 ✅
 * 3. 关掉 8080 → 8081 的 Session 依然有效 ✅
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // Session 30分钟过期
public class SessionConfig {

    /**
     * 配置 Cookie 序列化
     * 设置 Cookie 的路径为 /，域名不设置（保持默认）
     * 这样所有实例都能读取到同一个 Session ID
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");     // Cookie 名称
        serializer.setCookiePath("/");              // 全路径共享
        serializer.setUseHttpOnlyCookie(true);      // 防止 XSS 攻击
        serializer.setUseSecureCookie(false);       // 开发环境不用 HTTPS
        return serializer;
    }
}
