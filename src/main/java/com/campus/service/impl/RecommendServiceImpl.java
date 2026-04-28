package com.campus.service.impl;

import com.campus.dao.ProductMapper;
import com.campus.entity.Product;
import com.campus.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推荐服务实现类
 * 技术亮点：
 * 1. 基于内容的推荐算法（Content-Based Filtering）
 * 2. 使用内存缓存存储浏览历史（生产环境建议使用Redis）
 * 3. LRU策略限制历史记录数量，防止内存溢出
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private ProductMapper productMapper;

    // 用户浏览历史缓存（userId -> 浏览的商品ID列表，按时间倒序）
    // 技术说明：使用 ConcurrentHashMap 保证线程安全
    private static final Map<Integer, LinkedList<Integer>> browseHistoryCache = new ConcurrentHashMap<>();
    
    // 每个用户最多保存的浏览历史数量
    private static final int MAX_HISTORY_SIZE = 50;

    @Override
    public List<Product> getSimilarProducts(Integer productId, Integer limit) {
        // 获取当前商品信息
        Product currentProduct = productMapper.findById(productId);
        if (currentProduct == null) {
            return Collections.emptyList();
        }

        // 获取同分类的其他商品（排除当前商品）
        List<Product> sameCategory = productMapper.findList(null, currentProduct.getCategoryId(), 0);
        
        // 过滤掉当前商品，并按浏览量排序
        List<Product> result = new ArrayList<>();
        for (Product p : sameCategory) {
            if (!p.getId().equals(productId)) {
                result.add(p);
            }
        }

        // 按浏览量降序排序
        result.sort((a, b) -> b.getViewCount() - a.getViewCount());

        // 限制返回数量
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }

        return result;
    }

    @Override
    public List<Product> getPersonalizedRecommendations(Integer userId, Integer limit) {
        // 获取用户浏览历史
        LinkedList<Integer> history = browseHistoryCache.get(userId);
        if (history == null || history.isEmpty()) {
            // 无浏览历史，返回热门商品
            return productMapper.findHotProducts(limit);
        }

        // 统计用户浏览过的分类及其频次
        Map<Integer, Integer> categoryCount = new HashMap<>();
        for (Integer productId : history) {
            Product p = productMapper.findById(productId);
            if (p != null && p.getCategoryId() != null) {
                categoryCount.merge(p.getCategoryId(), 1, Integer::sum);
            }
        }

        // 按浏览频次排序，取用户最感兴趣的分类
        List<Map.Entry<Integer, Integer>> sortedCategories = new ArrayList<>(categoryCount.entrySet());
        sortedCategories.sort((a, b) -> b.getValue() - a.getValue());

        // 从用户感兴趣的分类中获取推荐商品
        Set<Integer> historySet = new HashSet<>(history);
        List<Product> recommendations = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : sortedCategories) {
            if (recommendations.size() >= limit) break;

            List<Product> categoryProducts = productMapper.findList(null, entry.getKey(), 0);
            for (Product p : categoryProducts) {
                // 排除已浏览过的商品
                if (!historySet.contains(p.getId())) {
                    recommendations.add(p);
                    if (recommendations.size() >= limit) break;
                }
            }
        }

        // 如果推荐不足，用热门商品补充
        if (recommendations.size() < limit) {
            List<Product> hotProducts = productMapper.findHotProducts(limit);
            for (Product p : hotProducts) {
                if (!historySet.contains(p.getId()) && !containsProduct(recommendations, p.getId())) {
                    recommendations.add(p);
                    if (recommendations.size() >= limit) break;
                }
            }
        }

        return recommendations;
    }

    @Override
    public void recordBrowseHistory(Integer userId, Integer productId) {
        if (userId == null || productId == null) return;

        browseHistoryCache.compute(userId, (key, history) -> {
            if (history == null) {
                history = new LinkedList<>();
            }

            // 如果已存在，先移除（实现 LRU）
            history.remove(productId);
            
            // 添加到头部（最新浏览）
            history.addFirst(productId);

            // 限制历史记录数量
            while (history.size() > MAX_HISTORY_SIZE) {
                history.removeLast();
            }

            return history;
        });
    }

    @Override
    public List<Product> getBrowseHistory(Integer userId, Integer limit) {
        LinkedList<Integer> history = browseHistoryCache.get(userId);
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> result = new ArrayList<>();
        int count = 0;
        for (Integer productId : history) {
            if (count >= limit) break;
            Product p = productMapper.findById(productId);
            if (p != null) {
                result.add(p);
                count++;
            }
        }

        return result;
    }

    private boolean containsProduct(List<Product> list, Integer productId) {
        for (Product p : list) {
            if (p.getId().equals(productId)) return true;
        }
        return false;
    }
}

