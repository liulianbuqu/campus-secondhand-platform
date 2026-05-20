package com.campus.service;

import com.campus.entity.Product;
import java.util.List;
import java.util.Map;

/**
 * 增强版商品推荐服务
 * 
 * 技术升级：
 * 1. 多因子相似度评分（分类 + 价格 + 关键词 + 热度 + 时效）
 * 2. Redis 缓存相似结果，提升并发性能
 * 3. 分布式场景下缓存一致性保障
 * 4. 详细的相似度明细返回，便于前端展示"为什么推荐"
 */
public interface EnhancedRecommendService {

    /**
     * 获取增强版相似商品推荐
     * 
     * 评分公式：
     *   score = 0.40 × isSameCategory 
     *         + 0.25 × priceSimilarity 
     *         + 0.20 × keywordSimilarity 
     *         + 0.10 × popularityNorm 
     *         + 0.05 × recencyNorm
     *
     * @param productId 当前商品ID
     * @param limit     返回数量
     * @return 推荐商品列表（按综合评分降序）
     */
    List<Product> getSimilarProductsEnhanced(Integer productId, Integer limit);

    /**
     * 获取相似商品推荐（带详细评分明细）
     * 用于前端展示"为什么推荐这些商品"
     *
     * @param productId 当前商品ID
     * @param limit     返回数量
     * @return 包含评分明细的推荐结果
     */
    List<Map<String, Object>> getSimilarProductsWithDetail(Integer productId, Integer limit);

    /**
     * 清除指定商品的相似推荐缓存
     * 在商品信息更新后调用，保证缓存一致性
     *
     * @param productId 商品ID
     */
    void clearSimilarCache(Integer productId);
}
