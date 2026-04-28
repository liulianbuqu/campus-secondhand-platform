package com.campus.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传工具类
 */
public class FileUploadUtil {
    /**
     * 上传文件
     * @param file 上传的文件
     * @param uploadPath 上传路径
     * @return 文件访问路径
     */
    public static String upload(MultipartFile file, String uploadPath) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 创建上传目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;

        // 保存文件
        File destFile = new File(dir, newFilename);
        file.transferTo(destFile);

        // 返回相对路径
        return "/upload/" + newFilename;
    }
}



