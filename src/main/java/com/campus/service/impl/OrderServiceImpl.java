package com.campus.service.impl;

import com.campus.dao.OrderMapper;
import com.campus.dao.ProductMapper;
import com.campus.entity.Order;
import com.campus.entity.Product;
import com.campus.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createOrder(Order order) {
        // 检查商品状态，防止超卖
        Product product = productMapper.findById(order.getProductId());
        if (product == null || product.getStatus() != 0) {
            throw new RuntimeException("商品不存在或已售出");
        }

        // 生成订单号
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setStatus(0); // 待处理

        // 创建订单
        int result = orderMapper.insert(order);

        // 更新商品状态为已售出
        if (result > 0) {
            productMapper.updateStatus(order.getProductId(), 1);
        }

        return result > 0;
    }

    @Override
    public List<Order> findByUserId(Integer userId) {
        return orderMapper.findByUserId(userId);
    }

    @Override
    public List<Order> findBySellerId(Integer sellerId) {
        return orderMapper.findBySellerId(sellerId);
    }

    @Override
    public List<Order> findAll() {
        return orderMapper.findAll();
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        return orderMapper.updateStatus(id, status) > 0;
    }

    @Override
    public Double getTotalAmount() {
        return orderMapper.getTotalAmount();
    }

    @Override
    public Order findById(Integer id) {
        return orderMapper.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmOrder(Integer orderId, Integer sellerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 验证是否是该卖家的订单
        Product product = productMapper.findById(order.getProductId());
        if (product == null || !product.getUserId().equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 只有待处理的订单才能确认完成
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态为已完成
        return orderMapper.updateStatus(orderId, 1) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Integer orderId, Integer sellerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 验证是否是该卖家的订单
        Product product = productMapper.findById(order.getProductId());
        if (product == null || !product.getUserId().equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 只有待处理的订单才能取消
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态为已取消
        int result = orderMapper.updateStatus(orderId, 2);

        // 恢复商品状态为在售
        if (result > 0) {
            productMapper.updateStatus(order.getProductId(), 0);
        }

        return result > 0;
    }
}



