package com.campus.controller;

import com.campus.service.CategoryService;
import com.campus.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 首页控制器
 */
@Controller
public class IndexController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 首页
     */
    @RequestMapping("/")
    public String index(Model model) {
        // 热门商品
        model.addAttribute("hotProducts", productService.findHotProducts(12));
        // 分类列表
        model.addAttribute("categories", categoryService.findAll());
        return "index";
    }
}

