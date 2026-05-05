package com.campus.controller;

import com.campus.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证工具 Controller
 * 成员1：用于验证 Redis 连接和分布式 Session 是否正常工作
 * 
 * 验收方法：
 * 1. 访问 /test/redis → 返回 Redis 连接状态 ✅
 * 2. 访问 /test/session/set?value=hello → 在 Session 中存入数据 ✅
 * 3. 访问 /test/session/get → 读取 Session 中的数据 ✅
 *    （在实例A存入，在实例B读取，验证分布式Session）
 * 4. 访问 /test/minio → 返回 MinIO 连接状态 ✅
 */
@Controller
@RequestMapping("/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Autowired(required = false)
    private MinIOConfig minIOConfig;

    /**
     * 测试 Redis 连接
     */
    @GetMapping("/redis")
    @ResponseBody
    public Map<String, Object> testRedis() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set("test:ping", "pong");
                String value = (String) redisTemplate.opsForValue().get("test:ping");
                result.put("success", true);
                result.put("message", "Redis 连接正常");
                result.put("data", "写入 test:ping = " + value);
            } else {
                result.put("success", false);
                result.put("message", "RedisTemplate 未注入，请检查 Redis 配置");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Redis 连接失败: " + e.getMessage());
            log.error("Redis 测试失败", e);
        }
        return result;
    }

    /**
     * 测试分布式 Session - 存入数据
     */
    @GetMapping("/session/set")
    @ResponseBody
    public Map<String, Object> setSession(String value, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (value == null) {
            value = "默认测试值";
        }
        session.setAttribute("testValue", value);
        session.setAttribute("testTime", System.currentTimeMillis());
        result.put("success", true);
        result.put("message", "Session 写入成功");
        result.put("sessionId", session.getId());
        result.put("value", value);
        return result;
    }

    /**
     * 测试分布式 Session - 读取数据
     */
    @GetMapping("/session/get")
    @ResponseBody
    public Map<String, Object> getSession(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Object testValue = session.getAttribute("testValue");
        Object testTime = session.getAttribute("testTime");
        result.put("success", true);
        result.put("sessionId", session.getId());
        result.put("testValue", testValue);
        result.put("testTime", testTime);
        result.put("message", testValue != null ? "Session 读取成功，数据跨实例共享！" : "Session 中无数据，请先访问 /test/session/set");
        return result;
    }

    /**
     * 测试 MinIO 连接
     */
    @GetMapping("/minio")
    @ResponseBody
    public Map<String, Object> testMinIO() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (minioClient != null && minIOConfig != null) {
                String bucketName = minIOConfig.getBucketName();
                boolean found = minioClient.bucketExists(
                        io.minio.BucketExistsArgs.builder().bucket(bucketName).build());
                result.put("success", true);
                result.put("message", "MinIO 连接正常");
                result.put("bucket", bucketName);
                result.put("bucketExists", found);
                result.put("endpoint", minIOConfig.getEndpoint());

                // 列出 bucket 中的文件
                if (found) {
                    Iterable<Result<Item>> objects = minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(bucketName).build());
                    int count = 0;
                    for (Result<Item> itemResult : objects) {
                        count++;
                    }
                    result.put("fileCount", count);
                }
            } else {
                result.put("success", false);
                result.put("message", "MinIO 客户端未注入，请检查 MinIO 配置");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "MinIO 连接失败: " + e.getMessage());
            log.error("MinIO 测试失败", e);
        }
        return result;
    }
}
