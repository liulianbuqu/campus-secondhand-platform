package com.campus.entity;

import lombok.Data;
import java.util.Date;

/**
 * 用户实体类
 */
@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;  // 邮箱（原微信号字段改为邮箱）
    private Integer role; // 1-普通用户，2-管理员
    private Integer status; // 1-正常，0-冻结
    private Date createTime;
}



