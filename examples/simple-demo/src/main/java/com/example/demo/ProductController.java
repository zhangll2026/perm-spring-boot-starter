package com.example.demo;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 示例控制器 - 商品管理
 * 用于演示权限管理功能
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /**
     * 获取商品列表
     */
    @GetMapping
    public List<String> listProducts() {
        return Arrays.asList("商品1", "商品2", "商品3");
    }

    /**
     * 创建商品
     */
    @PostMapping
    public String createProduct() {
        return "商品创建成功";
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public String updateProduct(@PathVariable Long id) {
        return "商品更新成功: " + id;
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        return "商品删除成功: " + id;
    }
}
