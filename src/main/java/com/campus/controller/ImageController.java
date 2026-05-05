package com.campus.controller;

import com.campus.config.MinIOConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 图片代理控制器
 * 
 * 解决图片共享问题：
 * 所有图片都存储在 MinIO 中，通过此 Controller 统一读取和返回
 * 前端 JSP 页面中的 <img src="/uploads/xxx.jpg"> 会被此 Controller 处理
 * 
 * 工作原理：
 * 1. 前端请求 /uploads/xxx.jpg
 * 2. 此 Controller 从 MinIO 读取文件
 * 3. 将文件内容返回给前端
 * 
 * 这样所有 Tomcat 实例都通过同一个 MinIO 读取图片，实现图片共享
 */
@Controller
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOConfig minIOConfig;

    /**
     * 从 MinIO 获取图片
     * 映射 /uploads/{filename} 到 MinIO 存储
     */
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public void getImage(@PathVariable String filename, HttpServletResponse response) {
        try {
            String bucketName = minIOConfig.getBucketName();
            
            // 从 MinIO 获取文件
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .build();

            try (InputStream inputStream = minioClient.getObject(args);
                 OutputStream outputStream = response.getOutputStream()) {

                // 设置 Content-Type
                String contentType = getContentType(filename);
                response.setContentType(contentType);
                response.setHeader("Cache-Control", "max-age=3600");

                // 流式传输文件内容
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("图片读取失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 根据文件名获取 Content-Type
     */
    private String getContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
