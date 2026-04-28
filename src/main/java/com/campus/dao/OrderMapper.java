package com.campus.dao;

import com.campus.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper接口
 */
public interface OrderMapper {
    /**
     * 插入订单
     */
    int insert(Order order);

    /**
     * 根据订单号查询订单
     */
    Order findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据ID查询订单
     */
    Order findById(@Param("id") Integer id);

    /**
     * 查询用户的订单列表
     */
    List<Order> findByUserId(@Param("userId") Integer userId);

    /**
     * 查询卖家的订单列表
     */
    List<Order> findBySellerId(@Param("sellerId") Integer sellerId);

    /**
     * 查询所有订单（管理员）
     */
    List<Order> findAll();

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 统计总交易额
     */
    Double getTotalAmount();
}



