package com.campus.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体类
 */
@Data
public class Order {
    private Integer id;
    private String orderNo;
    private Integer userId;
    private Integer productId;
    private BigDecimal totalPrice;
    private Integer status; // 0-待处理，1-已完成，2-已取消
    private Date createTime;
    
    // 关联对象
    private User buyer;
    private Product product;
}



