package com.campus.service.impl;

import com.campus.dao.ProductMapper;
import com.campus.entity.Product;
import com.campus.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 用户浏览历史缓存（userId -> 浏览的商品ID列表，按时间倒序）
    // 技术说明：使用 ConcurrentHashMap 保证线程安全
    private static final Map<Integer, LinkedList<Integer>> browseHistoryCache = new ConcurrentHashMap<>();
    
    // 每个用户最多保存的浏览历史数量
    private static final int MAX_HISTORY_SIZE = 50;
    private static final int PROFILE_TTL_SECONDS = 12 * 60 * 60;
    private static final int HISTORY_TTL_SECONDS = 24 * 60 * 60;
    private static final int PROFILE_LOCK_TTL_SECONDS = 5;

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
        // 缓存穿透防护：先查 Redis 画像，空则自动重建
        Map<Integer, Integer> categoryCount = loadOrRebuildCategoryProfile(userId);
        LinkedList<Integer> history = loadHistory(userId);
        if (history == null || history.isEmpty()) {
            // 无浏览历史，返回热门商品
            return productMapper.findHotProducts(limit);
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

        // 分布式锁：避免同一用户画像并发更新冲突
        String lockKey = "lock:profile:update:" + userId;
        String lockValue = UUID.randomUUID().toString();
        if (!acquireLock(lockKey, lockValue, PROFILE_LOCK_TTL_SECONDS)) {
            return;
        }

        try {
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
            syncHistoryToRedis(userId);
            rebuildProfileCache(userId);
        } finally {
            releaseLock(lockKey, lockValue);
        }
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

    private String profileKey(Integer userId) {
        return "rec:profile:" + userId;
    }

    private String historyKey(Integer userId) {
        return "rec:history:" + userId;
    }

    private void syncHistoryToRedis(Integer userId) {
        if (redisTemplate == null) {
            return;
        }
        LinkedList<Integer> history = browseHistoryCache.get(userId);
        if (history == null || history.isEmpty()) {
            return;
        }
        String key = historyKey(userId);
        redisTemplate.delete(key);
        for (Integer productId : history) {
            redisTemplate.opsForList().rightPush(key, productId);
        }
        redisTemplate.expire(key, HISTORY_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private LinkedList<Integer> loadHistory(Integer userId) {
        LinkedList<Integer> local = browseHistoryCache.get(userId);
        if (local != null && !local.isEmpty()) {
            return local;
        }
        if (redisTemplate == null) {
            return local;
        }
        List<Object> values = redisTemplate.opsForList().range(historyKey(userId), 0, MAX_HISTORY_SIZE - 1);
        if (values == null || values.isEmpty()) {
            return local;
        }
        LinkedList<Integer> fromRedis = new LinkedList<>();
        for (Object value : values) {
            try {
                fromRedis.add(Integer.parseInt(String.valueOf(value)));
            } catch (Exception ignored) {
            }
        }
        if (!fromRedis.isEmpty()) {
            browseHistoryCache.put(userId, fromRedis);
        }
        return fromRedis;
    }

    private Map<Integer, Integer> loadOrRebuildCategoryProfile(Integer userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        if (redisTemplate != null) {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(profileKey(userId));
            Map<Integer, Integer> profile = convertProfile(cached);
            if (!profile.isEmpty()) {
                redisTemplate.expire(profileKey(userId), PROFILE_TTL_SECONDS, TimeUnit.SECONDS);
                return profile;
            }
        }
        return rebuildProfileCache(userId);
    }

    private Map<Integer, Integer> rebuildProfileCache(Integer userId) {
        LinkedList<Integer> history = loadHistory(userId);
        Map<Integer, Integer> categoryCount = new HashMap<>();
        if (history != null) {
            for (Integer productId : history) {
                Product p = productMapper.findById(productId);
                if (p != null && p.getCategoryId() != null) {
                    Integer categoryId = p.getCategoryId();
                    Integer current = categoryCount.get(categoryId);
                    categoryCount.put(categoryId, current == null ? 1 : current + 1);
                }
            }
        }
        if (redisTemplate != null) {
            String key = profileKey(userId);
            redisTemplate.delete(key);
            if (!categoryCount.isEmpty()) {
                Map<String, Integer> store = new HashMap<>();
                for (Map.Entry<Integer, Integer> e : categoryCount.entrySet()) {
                    store.put(String.valueOf(e.getKey()), e.getValue());
                }
                redisTemplate.opsForHash().putAll(key, store);
                redisTemplate.expire(key, PROFILE_TTL_SECONDS, TimeUnit.SECONDS);
            }
        }
        return categoryCount;
    }

    private Map<Integer, Integer> convertProfile(Map<Object, Object> cached) {
        Map<Integer, Integer> result = new HashMap<>();
        if (cached == null || cached.isEmpty()) {
            return result;
        }
        for (Map.Entry<Object, Object> entry : cached.entrySet()) {
            try {
                Integer categoryId = Integer.parseInt(String.valueOf(entry.getKey()));
                Integer weight = Integer.parseInt(String.valueOf(entry.getValue()));
                result.put(categoryId, weight);
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private boolean acquireLock(String lockKey, String lockValue, int ttlSeconds) {
        if (redisTemplate == null) {
            return true;
        }
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    private void releaseLock(String lockKey, String lockValue) {
        if (redisTemplate == null) {
            return;
        }
        Object current = redisTemplate.opsForValue().get(lockKey);
        if (current != null && lockValue.equals(String.valueOf(current))) {
            redisTemplate.delete(lockKey);
        }
    }
}

