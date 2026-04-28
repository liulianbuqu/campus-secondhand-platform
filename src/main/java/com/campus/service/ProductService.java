package com.campus.service;

import com.campus.entity.Product;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {
    /**
     * 分页查询商品列表
     */
    PageInfo<Product> findList(String keyword, Integer categoryId, Integer pageNum, Integer pageSize);

    /**
     * 分页查询商品列表（可指定状态）
     */
    PageInfo<Product> findListWithStatus(String keyword, Integer categoryId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 根据ID查询商品详情
     */
    Product findById(Integer id);

    /**
     * 发布商品
     */
    boolean publish(Product product);

    /**
     * 更新商品
     */
    boolean update(Product product);

    /**
     * 删除商品
     */
    boolean delete(Integer id);

    /**
     * 增加浏览量
     */
    void increaseViewCount(Integer id);

    /**
     * 更新商品状态
     */
    boolean updateStatus(Integer id, Integer status);

    /**
     * 查询用户发布的商品
     */
    List<Product> findByUserId(Integer userId);

    /**
     * 查询热门商品
     */
    List<Product> findHotProducts(Integer limit);
}

