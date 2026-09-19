package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.repository.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 发布商品 */
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody CreateProductRequest req) {
        Product product = new Product();
        product.setTitle(req.title());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setOriginalPrice(req.originalPrice());
        product.setCategory(req.category());
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 发布商品请求体
     */
    public record CreateProductRequest(
            @NotBlank(message = "标题不能为空")
            @Size(max = 100, message = "标题最长 100 字")
            String title,

            @Size(max = 2000, message = "描述最长 2000 字")
            String description,

            @NotNull(message = "价格不能为空")
            @Positive(message = "价格必须大于 0")
            BigDecimal price,

            @Positive(message = "原价必须大于 0")
            BigDecimal originalPrice,

            @Size(max = 50, message = "分类最长 50 字")
            String category
    ) {
    }
}
