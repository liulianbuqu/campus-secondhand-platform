package com.campus.service.impl;

import com.campus.dao.ProductMapper;
import com.campus.entity.Product;
import com.campus.service.EnhancedRecommendService;
import com.campus.service.ProductFeatureService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 增强版商品推荐服务实现
 *
 * ==================== 多因子相似度算法 ====================
 *
 * 对每个同分类商品计算综合相似度：
 *
 *   score = 0.40 × isSameCategory
 *         + 0.25 × priceSimilarity
 *         + 0.20 × keywordSimilarity
 *         + 0.10 × popularityNorm
 *         + 0.05 × recencyNorm
 *
 * 约束条件：
 *   1. 排除已售/下架商品（status != 0）
 *   2. 排除当前商品自身
 *   3. 价格相似度低于 0.3 的降权处理
 *   4. 最低总分阈值 0.15，过低的不推荐
 *
 * ==================== 缓存策略 ====================
 *
 * Redis Key: "similar:enhanced:{productId}" (Hash)
 * 字段: "products" → JSON商品列表
 *       "details"  → JSON评分明细
 * TTL: 1 小时（缓存击穿时由互斥锁保护）
 * 淘汰：商品信息更新时主动清除
 */
@Service
public class EnhancedRecommendServiceImpl implements EnhancedRecommendService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedRecommendServiceImpl.class);

    // ==================== 评分权重 ====================
    private static final double W_CATEGORY = 0.40;
    private static final double W_PRICE = 0.25;
    private static final double W_KEYWORD = 0.20;
    private static final double W_POPULARITY = 0.10;
    private static final double W_RECENCY = 0.05;

    // ==================== 阈值常量 ====================
    /** 最低综合评分，低于此值不推荐 */
    private static final double MIN_SCORE_THRESHOLD = 0.15;
    /** 价格相似度低于此值降权 */
    private static final double PRICE_PENALTY_THRESHOLD = 0.3;

    // ==================== 缓存常量 ====================
    private static final String CACHE_KEY_PREFIX = "similar:enhanced:";
    private static final long CACHE_TTL_SECONDS = 3600; // 1 小时
    private static final long CACHE_LOCK_TTL_SECONDS = 10;
    private static final String CACHE_LOCK_KEY_PREFIX = "lock:similar:enhanced:";

    @Autowired
    private ProductMapper productMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductFeatureService productFeatureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ================================================================
    // 对外接口
    // ================================================================

    @Override
    public List<Product> getSimilarProductsEnhanced(Integer productId, Integer limit) {
        if (productId == null) return Collections.emptyList();
        int safeLimit = (limit == null || limit <= 0) ? 8 : limit;

        // 1. 尝试从缓存读取
        List<Product> cached = getFromCache(productId);
        if (cached != null) {
            log.debug("[增强推荐] 缓存命中 productId={}, 返回{}条", productId, Math.min(cached.size(), safeLimit));
            return cached.size() > safeLimit ? cached.subList(0, safeLimit) : cached;
        }

        // 2. 缓存未命中，加锁计算（防止缓存击穿）
        String lockKey = CACHE_LOCK_KEY_PREFIX + productId;
        String lockValue = UUID.randomUUID().toString();
        try {
            if (acquireLock(lockKey, lockValue, CACHE_LOCK_TTL_SECONDS)) {
                // 双重检查
                cached = getFromCache(productId);
                if (cached != null) {
                    return cached.size() > safeLimit ? cached.subList(0, safeLimit) : cached;
                }
                return computeAndCache(productId, safeLimit);
            }
            // 获取锁失败，等 50ms 后读缓存
            Thread.sleep(50);
            cached = getFromCache(productId);
            if (cached != null) {
                return cached.size() > safeLimit ? cached.subList(0, safeLimit) : cached;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            releaseLock(lockKey, lockValue);
        }

        // 4. 最终降级：走原始同分类推荐
        return fallbackSimilarProducts(productId, safeLimit);
    }

    @Override
    public List<Map<String, Object>> getSimilarProductsWithDetail(Integer productId, Integer limit) {
        if (productId == null) return Collections.emptyList();
        int safeLimit = (limit == null || limit <= 0) ? 8 : limit;

        // 先确保计算并缓存
        getSimilarProductsEnhanced(productId, safeLimit);

        // 从缓存读明细
        String detailKey = CACHE_KEY_PREFIX + productId;
        if (redisTemplate != null) {
            try {
                Object detailObj = redisTemplate.opsForHash().get(detailKey, "details");
                if (detailObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> details = (List<Map<String, Object>>) detailObj;
                    if (details.size() > safeLimit) {
                        return details.subList(0, safeLimit);
                    }
                    return details;
                }
                if (detailObj instanceof String) {
                    List<Map<String, Object>> details = objectMapper.readValue(
                            (String) detailObj,
                            new TypeReference<List<Map<String, Object>>>() {});
                    if (details.size() > safeLimit) {
                        return details.subList(0, safeLimit);
                    }
                    return details;
                }
            } catch (Exception e) {
                log.warn("[增强推荐] 读取评分明细缓存失败: {}", e.getMessage());
            }
        }

        // 降级返回无明细
        List<Product> products = getSimilarProductsEnhanced(productId, safeLimit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", p.getId());
            item.put("name", p.getName());
            item.put("price", p.getPrice());
            item.put("imageUrl", p.getImageUrl());
            item.put("viewCount", p.getViewCount());
            item.put("score", 0.0);
            result.add(item);
        }
        return result;
    }

    @Override
    public void clearSimilarCache(Integer productId) {
        if (productId == null) return;
        String key = CACHE_KEY_PREFIX + productId;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
                log.info("[增强推荐] 已清除商品{}的相似推荐缓存", productId);
            } catch (DataAccessException e) {
                log.warn("[增强推荐] 清除缓存失败: {}", e.getMessage());
            }
        }
    }

    // ================================================================
    // 核心计算
    // ================================================================

    /**
     * 计算并缓存相似推荐
     */
    private List<Product> computeAndCache(Integer productId, int limit) {
        long start = System.currentTimeMillis();

        Product current = productMapper.findById(productId);
        if (current == null) return Collections.emptyList();

        // 获取当前商品关键词
        List<String> currentKeywords = extractKeywords(current);

        // 获取同分类所有在售商品
        List<Product> candidates = productMapper.findList(null, current.getCategoryId(), 0);
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        // 计算每件候选商品的多因子评分
        List<ScoredProduct> scored = new ArrayList<>();
        Map<Integer, String[]> candidateKeywordsCache = new HashMap<>();

        // 预计算所有候选商品的关键词
        for (Product p : candidates) {
            if (p.getId().equals(productId)) continue; // 排除自身
            if (p.getStatus() != null && p.getStatus() != 0) continue; // 排除非在售

            String[] kwArray = getOrComputeKeywords(p, candidateKeywordsCache);
            double score = computeSimilarityScore(current, p, currentKeywords, kwArray);

            if (score >= MIN_SCORE_THRESHOLD) {
                scored.add(new ScoredProduct(p, score));
            }
        }

        // 按评分降序排列
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 取前 limit 个
        List<Product> resultProducts = new ArrayList<>();
        List<Map<String, Object>> resultDetails = new ArrayList<>();
        int rank = 1;
        for (ScoredProduct sp : scored) {
            if (resultProducts.size() >= limit) break;
            resultProducts.add(sp.product);

            // 构建评分明细
            Map<String, Object> detail = buildScoreDetail(sp, rank, current, currentKeywords,
                    candidateKeywordsCache.get(sp.product.getId()));
            resultDetails.add(detail);
            rank++;
        }

        // 写入缓存
        saveToCache(productId, resultProducts, resultDetails);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[增强推荐] 商品{}的相似推荐计算完成，候选{}个，推荐{}个，耗时{}ms",
                productId, scored.size(), resultProducts.size(), elapsed);

        return resultProducts;
    }

    /**
     * 多因子相似度计算
     */
    private double computeSimilarityScore(Product current, Product candidate,
                                          List<String> currentKeywords, String[] candidateKeywords) {
        // 1. 分类匹配度 (0.40)
        double categoryScore = current.getCategoryId() != null
                && current.getCategoryId().equals(candidate.getCategoryId()) ? 1.0 : 0.0;

        // 2. 价格相似度 (0.25)
        double priceScore = computePriceSimilarity(current.getPrice(), candidate.getPrice());

        // 3. 关键词相似度 (0.20) - Jaccard
        double keywordScore = computeKeywordSimilarity(currentKeywords, candidateKeywords);

        // 4. 热度归一化 (0.10)
        double popularityScore = computePopularityScore(candidate);

        // 5. 时效性 (0.05)
        double recencyScore = computeRecencyScore(candidate);

        // 综合评分
        double total = W_CATEGORY * categoryScore
                     + W_PRICE * priceScore
                     + W_KEYWORD * keywordScore
                     + W_POPULARITY * popularityScore
                     + W_RECENCY * recencyScore;

        // 价格严重不匹配时降权（价差超过2倍）
        if (priceScore < PRICE_PENALTY_THRESHOLD) {
            total *= 0.6;
        }

        return Math.round(total * 1000.0) / 1000.0;
    }

    /**
     * 价格相似度
     * 公式：1.0 - |p1 - p2| / max(p1, p2)
     * 价格越接近，得分越高
     */
    private double computePriceSimilarity(BigDecimal price1, BigDecimal price2) {
        if (price1 == null || price2 == null) return 0.0;
        if (price1.compareTo(BigDecimal.ZERO) <= 0 || price2.compareTo(BigDecimal.ZERO) <= 0) return 0.0;

        double p1 = price1.doubleValue();
        double p2 = price2.doubleValue();
        double diff = Math.abs(p1 - p2);
        double max = Math.max(p1, p2);

        double similarity = 1.0 - (diff / max);
        return Math.max(0.0, similarity);
    }

    /**
     * 关键词相似度 - Jaccard 系数
     * J(A, B) = |A ∩ B| / |A ∪ B|
     */
    private double computeKeywordSimilarity(List<String> keywords1, String[] keywords2) {
        if (keywords1 == null || keywords2 == null) return 0.0;
        if (keywords1.isEmpty() || keywords2.length == 0) return 0.0;

        Set<String> set1 = new HashSet<>(keywords1);
        Set<String> set2 = new HashSet<>(Arrays.asList(keywords2));

        // 计算交集
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // 计算并集
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / (double) union.size();
    }

    /**
     * 热度归一化
     * popularityNorm = min(viewCount / 1000.0, 1.0)
     */
    private double computePopularityScore(Product product) {
        int vc = product.getViewCount() == null ? 0 : product.getViewCount();
        return Math.min(vc / 1000.0, 1.0);
    }

    /**
     * 时效性评分
     * recencyNorm = 1.0 - min(daysSinceCreation / 365.0, 1.0)
     * 越新的商品得分越高
     */
    private double computeRecencyScore(Product product) {
        if (product.getCreateTime() == null) return 0.0;
        long now = System.currentTimeMillis();
        long create = product.getCreateTime().getTime();
        long diffMs = now - create;
        if (diffMs < 0) return 1.0; // 未来时间？
        double days = diffMs / (1000.0 * 60 * 60 * 24);
        double score = 1.0 - Math.min(days / 365.0, 1.0);
        return Math.max(0.0, score);
    }

    /**
     * 提取商品关键词（带缓存）
     */
    private String[] getOrComputeKeywords(Product product, Map<Integer, String[]> cache) {
        if (cache.containsKey(product.getId())) {
            return cache.get(product.getId());
        }
        List<String> kwList = extractKeywords(product);
        String[] kwArray = kwList.toArray(new String[0]);
        cache.put(product.getId(), kwArray);
        return kwArray;
    }

    /**
     * 从商品提取关键词
     */
    private List<String> extractKeywords(Product product) {
        String title = product.getName() != null ? product.getName() : "";
        return productFeatureService.extractKeywords(title);
    }

    // ================================================================
    // 缓存管理
    // ================================================================

    @SuppressWarnings("unchecked")
    private List<Product> getFromCache(Integer productId) {
        if (redisTemplate == null) return null;
        try {
            String key = CACHE_KEY_PREFIX + productId;
            Object obj = redisTemplate.opsForHash().get(key, "products");
            if (obj instanceof List) {
                return (List<Product>) obj;
            }
            if (obj instanceof String) {
                return objectMapper.readValue((String) obj,
                        new TypeReference<List<Product>>() {});
            }
        } catch (Exception e) {
            log.warn("[增强推荐] 缓存读取失败: {}", e.getMessage());
        }
        return null;
    }

    private void saveToCache(Integer productId, List<Product> products,
                             List<Map<String, Object>> details) {
        if (redisTemplate == null) return;
        try {
            String key = CACHE_KEY_PREFIX + productId;
            Map<String, Object> cacheMap = new HashMap<>();
            cacheMap.put("products", products);
            cacheMap.put("details", details);
            cacheMap.put("cachedAt", System.currentTimeMillis());
            redisTemplate.opsForHash().putAll(key, cacheMap);
            redisTemplate.expire(key, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
            log.warn("[增强推荐] 缓存写入失败: {}", e.getMessage());
        }
    }

    // ================================================================
    // 分布式锁
    // ================================================================

    private boolean acquireLock(String lockKey, String lockValue, long ttlSeconds) {
        if (redisTemplate == null) return false;
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (DataAccessException e) {
            log.warn("[增强推荐] 获取分布式锁失败: {}", e.getMessage());
            return false;
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        if (redisTemplate == null) return;
        try {
            String current = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(lockKey);
            }
        } catch (DataAccessException e) {
            log.warn("[增强推荐] 释放分布式锁失败: {}", e.getMessage());
        }
    }

    // ================================================================
    // 评分明细与降级
    // ================================================================

    private Map<String, Object> buildScoreDetail(ScoredProduct sp, int rank,
                                                  Product current, List<String> currentKeywords,
                                                  String[] candidateKeywords) {
        Product p = sp.product;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("rank", rank);
        detail.put("productId", p.getId());
        detail.put("name", p.getName());
        detail.put("price", p.getPrice());
        detail.put("imageUrl", p.getImageUrl());
        detail.put("viewCount", p.getViewCount());
        detail.put("totalScore", sp.score);

        // 各维度得分
        double priceScore = computePriceSimilarity(current.getPrice(), p.getPrice());
        double kwScore = computeKeywordSimilarity(currentKeywords,
                candidateKeywords != null ? candidateKeywords : new String[0]);
        double popScore = computePopularityScore(p);
        double recScore = computeRecencyScore(p);

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("category", 1.0); // 同分类
        breakdown.put("price", Math.round(priceScore * 1000.0) / 1000.0);
        breakdown.put("keyword", Math.round(kwScore * 1000.0) / 1000.0);
        breakdown.put("popularity", Math.round(popScore * 1000.0) / 1000.0);
        breakdown.put("recency", Math.round(recScore * 1000.0) / 1000.0);
        detail.put("scoreBreakdown", breakdown);

        return detail;
    }

    /**
     * 降级方案：原始同分类+浏览量排序
     */
    private List<Product> fallbackSimilarProducts(Integer productId, int limit) {
        Product currentProduct = productMapper.findById(productId);
        if (currentProduct == null) return Collections.emptyList();

        List<Product> sameCategory = productMapper.findList(null, currentProduct.getCategoryId(), 0);
        if (sameCategory == null) return Collections.emptyList();

        List<Product> result = new ArrayList<>();
        for (Product p : sameCategory) {
            if (!p.getId().equals(productId)) result.add(p);
        }
        result.sort((a, b) -> {
            int va = a.getViewCount() == null ? 0 : a.getViewCount();
            int vb = b.getViewCount() == null ? 0 : b.getViewCount();
            return Integer.compare(vb, va);
        });
        if (result.size() > limit) result = result.subList(0, limit);
        return result;
    }

    // ================================================================
    // 内部类
    // ================================================================

    private static class ScoredProduct {
        final Product product;
        final double score;

        ScoredProduct(Product product, double score) {
            this.product = product;
            this.score = score;
        }
    }
}
