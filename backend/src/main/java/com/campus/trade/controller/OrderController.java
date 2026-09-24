package com.campus.trade.controller;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口：下单、我的订单（买/卖）、发货、收货、取消、退款。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /** 买家下单（商品暂保持 ON_SALE，支付成功后才置 SOLD） */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest req,
                                    Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.createOrder(userId, req.productId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(order));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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

    /** 卖家发货 */
    @PutMapping("/{id}/ship")
    public ResponseEntity<?> ship(@PathVariable Long id,
                                  @Valid @RequestBody ShipRequest req,
                                  Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.ship(id, userId, req.shippingCompany(), req.trackingNo());
            return ResponseEntity.ok(toDto(order));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 买家确认收货（卖家收款到余额） */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.complete(id, userId);
            return ResponseEntity.ok(toDto(order));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 取消订单（仅 PENDING 状态，商品恢复在售） */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.cancel(id, userId);
            return ResponseEntity.ok(toDto(order));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 买家申请退款（PAID/SHIPPED → REFUNDED，退买家余额） */
    @PutMapping("/{id}/refund")
    public ResponseEntity<?> refund(@PathVariable Long id,
                                    @Valid @RequestBody RefundRequest req,
                                    Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            Order order = orderService.refund(id, userId, req.reason());
            return ResponseEntity.ok(toDto(order));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", o.getId());
        dto.put("product", productDto);
        dto.put("buyer", buyerDto);
        dto.put("seller", sellerDto);
        dto.put("amount", o.getAmount());
        dto.put("status", o.getStatus());
        dto.put("shippingCompany", o.getShippingCompany());
        dto.put("trackingNo", o.getTrackingNo());
        dto.put("refundReason", o.getRefundReason());
        dto.put("createdAt", o.getCreatedAt());
        dto.put("shippedAt", o.getShippedAt());
        dto.put("completedAt", o.getCompletedAt());
        dto.put("refundedAt", o.getRefundedAt());
        return dto;
    }

    public record CreateOrderRequest(@NotNull(message = "商品不能为空") Long productId) {}

    public record ShipRequest(
            @NotBlank(message = "物流公司不能为空") String shippingCompany,
            @NotBlank(message = "物流单号不能为空") String trackingNo
    ) {}

    public record RefundRequest(@NotBlank(message = "退款原因不能为空") String reason) {}
}
