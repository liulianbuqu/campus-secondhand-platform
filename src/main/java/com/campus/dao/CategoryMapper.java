package com.campus.dao;

import com.campus.entity.Category;
import java.util.List;

/**
 * 分类Mapper接口
 */
public interface CategoryMapper {
    /**
     * 查询所有分类
     */
    List<Category> findAll();

    /**
     * 根据ID查询分类
     */
    Category findById(Integer id);
}



