package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductController(ProductRepository productRepository,
                             UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /** 在售商品列表（按发布时间倒序），q 非空时按标题/描述模糊搜索 */
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            return productRepository.search("ON_SALE", q.trim());
        }
        return productRepository.findByStatusOrderByCreatedAtDesc("ON_SALE");
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public ResponseEntity<Product> detail(@PathVariable Long id) {
        return productRepository.findWithCreatorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 发布商品（需登录） */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProductRequest req,
                                    Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        User creator = userRepository.findById(userId).orElse(null);
        if (creator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "用户不存在"));
        }
        Product product = new Product();
        product.setTitle(req.title());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setOriginalPrice(req.originalPrice());
        product.setCategory(req.category());
        product.setCreator(creator);
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
