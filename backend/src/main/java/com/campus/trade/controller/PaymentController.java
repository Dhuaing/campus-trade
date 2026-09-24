package com.campus.trade.controller;

import com.campus.trade.entity.Payment;
import com.campus.trade.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口：创建支付单、模拟支付成功、查询支付状态。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 为订单创建支付单 */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreatePaymentRequest req) {
        try {
            Payment p = paymentService.createPayment(req.orderId());
            return ResponseEntity.ok(Map.of(
                    "paymentId", p.getId(),
                    "paymentNo", p.getPaymentNo(),
                    "amount", p.getAmount(),
                    "payMethod", p.getPayMethod(),
                    "status", p.getStatus()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 模拟支付成功（校园场景无真实支付网关，前端调此接口触发回调） */
    @PostMapping("/{paymentNo}/mock-success")
    public ResponseEntity<?> mockSuccess(@PathVariable String paymentNo) {
        try {
            Payment p = paymentService.handleCallback(paymentNo, "MOCK-" + UUID.randomUUID());
            return ResponseEntity.ok(Map.of(
                    "paymentId", p.getId(),
                    "status", p.getStatus(),
                    "paidAt", p.getPaidAt()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 查询支付单状态 */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            Payment p = paymentService.getPayment(id);
            return ResponseEntity.ok(Map.of(
                    "id", p.getId(),
                    "paymentNo", p.getPaymentNo(),
                    "amount", p.getAmount(),
                    "status", p.getStatus(),
                    "paidAt", p.getPaidAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    public record CreatePaymentRequest(@NotNull(message = "订单不能为空") Long orderId) {}
}
