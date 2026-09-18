package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.repository.ProductRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** 在售商品列表（按发布时间倒序） */
    @GetMapping
    public List<Product> list() {
        return productRepository.findByStatusOrderByCreatedAtDesc("ON_SALE");
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public ResponseEntity<Product> detail(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
