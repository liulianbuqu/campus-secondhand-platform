package com.campus.entity;

import lombok.Data;
import java.util.Date;

/**
 * 操作日志实体类
 * 用于记录用户的关键操作，便于审计和问题追踪
 */
@Data
public class OperationLog {
    private Integer id;
    private Integer userId;          // 操作用户ID
    private String username;         // 操作用户名
    private String operation;        // 操作类型（如：登录、发布商品、下单等）
    private String method;           // 请求方法
    private String params;           // 请求参数
    private String ip;               // 操作IP
    private Long executionTime;      // 执行时长（毫秒）
    private Date createTime;         // 操作时间
}

