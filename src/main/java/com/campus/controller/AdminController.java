package com.campus.controller;

import com.campus.entity.Order;
import com.campus.entity.Product;
import com.campus.entity.User;
import com.campus.service.OrderService;
import com.campus.service.ProductService;
import com.campus.service.UserService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员控制器
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    /**
     * 管理员首页
     */
    @RequestMapping("/index")
    public String index(Model model) {
        // 统计数据
        model.addAttribute("userCount", userService.findAll().size());
        model.addAttribute("totalAmount", orderService.getTotalAmount());
        model.addAttribute("orderCount", orderService.findAll().size());
        return "admin/index";
    }

    /**
     * 用户管理
     */
    @RequestMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    /**
     * 更新用户状态
     */
    @RequestMapping(value = "/user/updateStatus", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateUserStatus(Integer id, Integer status) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userService.updateStatus(id, status);
        result.put("success", success);
        return result;
    }

    /**
     * 商品管理
     */
    @RequestMapping("/products")
    public String products(Model model) {
        // 查询所有商品（包括已售出和下架的），status=null表示查询所有状态
        PageInfo<Product> pageInfo = productService.findListWithStatus(null, null, null, 1, 1000);
        model.addAttribute("products", pageInfo.getList());
        return "admin/products";
    }

    /**
     * 下架商品
     */
    @RequestMapping(value = "/product/offline", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> offlineProduct(Integer id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = productService.updateStatus(id, 2);
        result.put("success", success);
        return result;
    }

    /**
     * 订单管理
     */
    @RequestMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "admin/orders";
    }
}

