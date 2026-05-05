package com.campus.service.impl;

import com.campus.dao.UserMapper;
import com.campus.entity.User;
import com.campus.service.UserService;
import com.campus.util.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean register(User user) {
        // 检查用户名是否已存在
        User existUser = userMapper.findByUsername(user.getUsername());
        if (existUser != null) {
            return false;
        }

        // 密码MD5加密
        user.setPassword(MD5Util.md5(user.getPassword()));
        user.setRole(1); // 普通用户
        user.setStatus(1); // 正常状态

        // 如果没有提供昵称，使用用户名作为昵称
        if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            user.setNickname(user.getUsername());
        }

        return userMapper.insert(user) > 0;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }

        // 验证密码
        String encryptedPassword = MD5Util.md5(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            return null;
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            return null;
        }

        // 更新登录时间和在线状态（非核心流程，失败不应阻断登录）
        Date now = new Date();
        try {
            userMapper.updateLoginInfo(user.getId(), now, 1);
            user.setLastLoginTime(now);
            user.setOnlineStatus(1);
        } catch (Exception ex) {
            logger.warn("更新登录时间失败，忽略并继续登录，userId={}", user.getId(), ex);
        }

        return user;
    }

    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    @Override
    public boolean update(User user) {
        return userMapper.update(user) > 0;
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public int count() {
        return userMapper.count();
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        return userMapper.updateStatus(id, status) > 0;
    }
}



