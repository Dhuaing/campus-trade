package com.campus.trade.controller;

import com.campus.trade.entity.Product;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.security.Audited;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-商品审核：待审队列、通过/驳回、强制下架
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductRepository productRepository;

    public AdminProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** 商品分页列表：status 为空时查全部（含各状态） */
    @GetMapping
    @PreAuthorize("hasAuthority('product:audit')")
    public Map<String, Object> list(@RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<Product> result = (status == null || status.isBlank())
                ? productRepository.findAllWithCreator(pageable)
                : productRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        Map<String, Object> body = new HashMap<>();
        body.put("content", result.getContent());
        body.put("totalElements", result.getTotalElements());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("hasMore", result.hasNext());
        return body;
    }

    /** 审核通过：PENDING_REVIEW → ON_SALE */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('product:audit')")
    @Audited(action = "PRODUCT_APPROVE", targetType = "PRODUCT")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "商品不存在"));
        }
        if (!"PENDING_REVIEW".equals(product.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "仅待审核商品可通过"));
        }
        product.setStatus("ON_SALE");
        product.setAuditRemark(null);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of("id", id, "status", "ON_SALE"));
    }

    /** 审核驳回：PENDING_REVIEW → REJECTED（记录原因） */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('product:audit')")
    @Audited(action = "PRODUCT_REJECT", targetType = "PRODUCT")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody(required = false) ReasonRequest req) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "商品不存在"));
        }
        if (!"PENDING_REVIEW".equals(product.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "仅待审核商品可驳回"));
        }
        String reason = req == null || req.reason() == null || req.reason().isBlank()
                ? "不符合平台规范" : req.reason();
        product.setStatus("REJECTED");
        product.setAuditRemark(reason);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of("id", id, "status", "REJECTED", "reason", reason));
    }

    /** 强制下架：ON_SALE / PENDING_REVIEW → REMOVED（记录原因） */
    @PutMapping("/{id}/remove")
    @PreAuthorize("hasAuthority('product:remove')")
    @Audited(action = "PRODUCT_REMOVE", targetType = "PRODUCT")
    public ResponseEntity<?> remove(@PathVariable Long id,
                                    @RequestBody(required = false) ReasonRequest req) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "商品不存在"));
        }
        if (!"ON_SALE".equals(product.getStatus()) && !"PENDING_REVIEW".equals(product.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "仅在售或待审核商品可下架"));
        }
        String reason = req == null || req.reason() == null || req.reason().isBlank()
                ? "管理员强制下架" : req.reason();
        product.setStatus("REMOVED");
        product.setAuditRemark(reason);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of("id", id, "status", "REMOVED", "reason", reason));
    }

    public record ReasonRequest(String reason) {}
}
