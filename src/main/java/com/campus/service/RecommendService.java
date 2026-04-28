package com.campus.service;

import com.campus.entity.Product;
import java.util.List;

/**
 * 推荐服务接口
 * 技术亮点：实现简单的商品推荐算法
 */
public interface RecommendService {

    /**
     * 获取相似商品推荐
     * 推荐算法：基于分类的协同过滤
     * 1. 找到与当前商品同分类的其他商品
     * 2. 按浏览量排序，推荐热门商品
     * 
     * @param productId 当前商品ID
     * @param limit 推荐数量
     * @return 推荐商品列表
     */
    List<Product> getSimilarProducts(Integer productId, Integer limit);

    /**
     * 获取用户个性化推荐
     * 推荐算法：基于用户浏览历史
     * 1. 获取用户最近浏览的商品分类
     * 2. 推荐这些分类下的热门商品
     * 
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐商品列表
     */
    List<Product> getPersonalizedRecommendations(Integer userId, Integer limit);

    /**
     * 记录用户浏览历史
     * @param userId 用户ID
     * @param productId 商品ID
     */
    void recordBrowseHistory(Integer userId, Integer productId);

    /**
     * 获取用户浏览历史
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 浏览过的商品列表
     */
    List<Product> getBrowseHistory(Integer userId, Integer limit);
}

