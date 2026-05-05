package com.campus.service;

import com.campus.entity.Order;
import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    /**
     * 创建订单
     */
    boolean createOrder(Order order);

    /**
     * 查询用户的订单列表
     */
    List<Order> findByUserId(Integer userId);

    /**
     * 查询卖家的订单列表
     */
    List<Order> findBySellerId(Integer sellerId);

    /**
     * 查询所有订单
     */
    List<Order> findAll();

    /**
     * 更新订单状态
     */
    boolean updateStatus(Integer id, Integer status);

    /**
     * 统计总交易额
     */
    Double getTotalAmount();

    /**
     * 统计订单总数
     */
    int count();

    /**
     * 根据ID查询订单
     */
    Order findById(Integer id);

    /**
     * 卖家确认完成订单
     */
    boolean confirmOrder(Integer orderId, Integer sellerId);

    /**
     * 卖家取消订单（商品恢复上架）
     */
    boolean cancelOrder(Integer orderId, Integer sellerId);
}



