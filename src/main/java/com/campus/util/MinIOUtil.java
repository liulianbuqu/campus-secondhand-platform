package com.campus.util;

import com.campus.config.MinIOConfig;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储工具类
 * 
 * 替代原来的本地文件存储，实现多实例间图片共享
 * 
 * 使用方式：
 * 1. 上传：MinIOUtil.upload(file) → 返回可访问的图片URL
 * 2. 所有Tomcat实例都连接同一个MinIO，图片完全共享
 */
@Component
public class MinIOUtil {

    private static final Logger log = LoggerFactory.getLogger(MinIOUtil.class);

    @Autowired
    private MinIOConfig minIOConfig;

    @Autowired
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            // 检查 bucket 是否存在，不存在则创建
            String bucketName = minIOConfig.getBucketName();
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' 创建成功", bucketName);
            } else {
                log.info("MinIO bucket '{}' 已存在", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO 初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 上传文件到 MinIO
     * @param file 上传的文件
     * @return 文件访问路径（相对路径，格式：/uploads/文件名）
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // 上传到 MinIO
            String bucketName = minIOConfig.getBucketName();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(newFilename)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            log.info("文件上传到 MinIO 成功: {}/{}", bucketName, newFilename);

            // 返回相对路径（与原来格式一致，方便 JSP 页面使用）
            return "/uploads/" + newFilename;

        } catch (Exception e) {
            log.error("MinIO 文件上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的预签名 URL（临时可访问）
     * @param objectName 文件名
     * @param expiryMinutes 过期时间（分钟）
     * @return 临时访问 URL
     */
    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            String bucketName = minIOConfig.getBucketName();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 删除文件
     * @param objectName 文件名
     */
    public void delete(String objectName) {
        try {
            String bucketName = minIOConfig.getBucketName();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("文件从 MinIO 删除成功: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: {}", e.getMessage());
        }
    }

    /**
     * 从完整URL中提取文件名
     * 例如：/uploads/abc.jpg → abc.jpg
     */
    public static String extractObjectName(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        // 去掉前缀 /uploads/
        if (imageUrl.startsWith("/uploads/")) {
            return imageUrl.substring("/uploads/".length());
        }
        return imageUrl;
    }
}
