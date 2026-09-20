package com.campus.trade.controller;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口：下单、我的订单（买/卖）、卖家确认完成、双方取消
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderController(OrderRepository orderRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /** 买家下单（商品置为 SOLD） */
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest req,
                                    Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        User buyer = userRepository.findById(userId).orElse(null);
        if (buyer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户不存在"));
        }
        Product product = productRepository.findWithCreatorById(req.productId()).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "商品不存在"));
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "商品当前不可购买"));
        }
        if (product.getCreator() != null && userId.equals(product.getCreator().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "不能购买自己发布的商品"));
        }
        Order order = new Order();
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setStatus("PENDING");
        Order saved = orderRepository.save(order);
        product.setStatus("SOLD");
        productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    /** 我的订单：bought = 我买到的，sold = 我卖出的 */
    @GetMapping("/mine")
    public ResponseEntity<?> mine(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        List<Map<String, Object>> bought = orderRepository.findByBuyer_IdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
        List<Map<String, Object>> sold = orderRepository.findByProduct_Creator_IdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(Map.of("bought", bought, "sold", sold));
    }

    /** 卖家确认完成订单 */
    @PutMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<?> complete(@PathVariable Long id, Authentication authentication) {
        return transition(id, authentication, "COMPLETED", true);
    }

    /** 买家或卖家取消订单（商品恢复在售） */
    @PutMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id, Authentication authentication) {
        return transition(id, authentication, "CANCELLED", false);
    }

    private ResponseEntity<?> transition(Long id, Authentication authentication,
                                         String targetStatus, boolean sellerOnly) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        Order order = orderRepository.findWithProductAndBuyerById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "订单不存在"));
        }
        Long buyerId = order.getBuyer().getId();
        Long sellerId = order.getProduct().getCreator() == null
                ? null : order.getProduct().getCreator().getId();
        boolean isBuyer = userId.equals(buyerId);
        boolean isSeller = sellerId != null && userId.equals(sellerId);
        if (sellerOnly && !isSeller) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "仅卖家可确认完成"));
        }
        if (!sellerOnly && !isBuyer && !isSeller) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "无权操作该订单"));
        }
        if (!"PENDING".equals(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "订单已处理，无法重复操作"));
        }
        order.setStatus(targetStatus);
        orderRepository.save(order);
        Product product = order.getProduct();
        if ("CANCELLED".equals(targetStatus) && "SOLD".equals(product.getStatus())) {
            product.setStatus("ON_SALE");
            productRepository.save(product);
        }
        return ResponseEntity.ok(toDto(order));
    }

    private Map<String, Object> toDto(Order o) {
        Product p = o.getProduct();
        Map<String, Object> productDto = Map.of(
                "id", p.getId(),
                "title", p.getTitle(),
                "price", p.getPrice(),
                "coverImage", p.getCoverImage() == null ? "" : p.getCoverImage()
        );
        Map<String, Object> buyerDto = Map.of(
                "id", o.getBuyer().getId(),
                "nickname", o.getBuyer().getNickname()
        );
        Map<String, Object> sellerDto = p.getCreator() == null
                ? Map.<String, Object>of("id", 0, "nickname", "未知卖家")
                : Map.<String, Object>of(
                        "id", p.getCreator().getId(),
                        "nickname", p.getCreator().getNickname());
        return Map.of(
                "id", o.getId(),
                "product", productDto,
                "buyer", buyerDto,
                "seller", sellerDto,
                "status", o.getStatus(),
                "createdAt", o.getCreatedAt()
        );
    }

    /**
     * 下单请求体
     */
    public record CreateOrderRequest(
            @NotNull(message = "商品不能为空")
            Long productId
    ) {
    }
}
