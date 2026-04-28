package com.campus.service;

import com.campus.entity.Category;
import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {
    /**
     * 查询所有分类
     */
    List<Category> findAll();
}



