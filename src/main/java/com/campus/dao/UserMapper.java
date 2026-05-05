package com.campus.dao;

import com.campus.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
public interface UserMapper {
    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Integer id);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int update(User user);

    /**
     * 查询所有用户（管理员）
     */
    List<User> findAll();

    /**
     * 统计用户总数
     */
    int count();

    /**
     * 更新用户登录时间和在线状态
     */
    int updateLoginInfo(@Param("id") Integer id, @Param("lastLoginTime") java.util.Date lastLoginTime, @Param("onlineStatus") Integer onlineStatus);

    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
}



