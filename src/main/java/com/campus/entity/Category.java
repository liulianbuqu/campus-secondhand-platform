package com.campus.entity;

import lombok.Data;
import java.util.Date;

/**
 * 商品分类实体类
 */
@Data
public class Category {
    private Integer id;
    private String categoryName;
    private Date createTime;
}



