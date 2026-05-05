package com.campus.service;

import com.campus.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 用户注册
     */
    boolean register(User user);

    /**
     * 用户登录
     */
    User login(String username, String password);

    /**
     * 根据ID查询用户
     */
    User findById(Integer id);

    /**
     * 更新用户信息
     */
    boolean update(User user);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 统计用户总数
     */
    int count();

    /**
     * 更新用户状态
     */
    boolean updateStatus(Integer id, Integer status);
}



