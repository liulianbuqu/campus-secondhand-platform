package com.campus.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * MinIO 对象存储配置类
 * 
 * 解决图片共享问题：
 * 所有 Tomcat 实例都连接同一个 MinIO 服务，上传和读取同一份图片文件
 * 不再依赖本地磁盘，多实例间图片完全共享
 * 
 * MinIO 启动命令：
 * docker run -d -p 9000:9000 -p 9001:9001 \
 *   --name minio \
 *   -e MINIO_ROOT_USER=minioadmin \
 *   -e MINIO_ROOT_PASSWORD=minioadmin \
 *   minio/minio server /data --console-address ":9001"
 */
@Configuration
@PropertySource("classpath:redis-config.properties")
public class MinIOConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
