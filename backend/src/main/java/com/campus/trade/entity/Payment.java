package com.campus.trade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单：与订单一对一绑定。
 * 支付回调只更新本单状态，不直接触发订单完成（由订单服务在用户确认收货时结算）。
 */
@Entity
@Table(name = "payments")
public class Payment {

    /** 待支付 */
    public static final String STATUS_PENDING = "PENDING";
    /** 支付成功 */
    public static final String STATUS_PAID = "PAID";
    /** 支付失败 */
    public static final String STATUS_FAILED = "FAILED";
    /** 已退款 */
    public static final String STATUS_REFUNDED = "REFUNDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 支付单号（out_trade_no），全局唯一 */
    @Column(name = "payment_no", nullable = false, unique = true, length = 64)
    private String paymentNo;

    /** 关联订单 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** 支付金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 支付方式：MOCK / WECHAT / ALIPAY */
    @Column(name = "pay_method", length = 20)
    private String payMethod;

    /** 支付状态 */
    @Column(nullable = false, length = 20)
    private String status = STATUS_PENDING;

    /** 第三方交易号（真实支付时填入） */
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    /** 支付成功时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = STATUS_PENDING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
