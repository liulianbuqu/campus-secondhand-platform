package com.campus.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体类
 */
@Data
public class Product {
    private Integer id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private Integer categoryId;
    private Integer userId;
    private Integer status; // 0-在售，1-已售出，2-下架
    private Integer viewCount;
    private Date createTime;
    private Date updateTime;
    
    // 关联对象
    private Category category;
    private User seller;
}



