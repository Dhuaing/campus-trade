package com.campus.trade.service;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.Payment;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付服务：管理支付单生命周期。
 * 关键约束（经验教训）：
 * - 支付回调只更新支付单状态（PENDING → PAID），不直接触发订单完成
 * - 支付单与订单一对一绑定，用 paymentNo（out_trade_no）唯一定位
 * - 回调幂等：仅 PENDING → PAID 可推进，重复回调不产生副作用
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final BalanceService balanceService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          PaymentGateway paymentGateway,
                          BalanceService balanceService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.balanceService = balanceService;
    }

    /**
     * 为订单创建支付单。若已存在待支付单则复用。
     */
    @Transactional
    public Payment createPayment(Long orderId) {
        Order order = orderRepository.findWithProductAndBuyerById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!Order.STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("订单状态非待支付，当前：" + order.getStatus());
        }
        // 已存在待支付单则复用
        return paymentRepository.findByOrderId(orderId)
                .filter(p -> Payment.STATUS_PENDING.equals(p.getStatus()))
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setPaymentNo("PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
                    p.setOrder(order);
                    p.setAmount(order.getAmount());
                    p.setPayMethod(paymentGateway.getCode());
                    p.setStatus(Payment.STATUS_PENDING);
                    return paymentRepository.save(p);
                });
    }

    /**
     * 支付回调（模拟/真实）：只更新支付单状态，不直接完成订单。
     * 幂等：仅 PENDING → PAID 可推进。
     */
    @Transactional
    public Payment handleCallback(String paymentNo, String transactionId) {
        Payment payment = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new IllegalArgumentException("支付单不存在：" + paymentNo));
        // 幂等：已支付则直接返回，不重复处理
        if (Payment.STATUS_PAID.equals(payment.getStatus())) {
            return payment;
        }
        if (!Payment.STATUS_PENDING.equals(payment.getStatus())) {
            throw new IllegalStateException("支付单状态非待支付，当前：" + payment.getStatus());
        }
        // 验签（模拟支付直接通过）
        if (!paymentGateway.verifyCallback(paymentNo, transactionId, payment.getAmount())) {
            payment.setStatus(Payment.STATUS_FAILED);
            return paymentRepository.save(payment);
        }
        // 仅更新支付单状态 + 订单推进到 PAID
        payment.setStatus(Payment.STATUS_PAID);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 扣买家余额（模拟支付从平台余额扣款，真实支付由第三方扣款）
        Order order = payment.getOrder();
        Long buyerId = order.getBuyer().getId();
        boolean deducted = balanceService.deduct(buyerId, payment.getAmount());
        if (!deducted) {
            // 余额不足，支付失败（不推进订单状态）
            payment.setStatus(Payment.STATUS_FAILED);
            return paymentRepository.save(payment);
        }

        // 订单推进到已付款（注意：不直接完成订单，等待卖家发货/买家收货）
        if (Order.STATUS_PENDING.equals(order.getStatus())) {
            order.setStatus(Order.STATUS_PAID);
            orderRepository.save(order);
        }
        return payment;
    }

    /** 查询支付单状态 */
    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("支付单不存在"));
    }

    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单无支付单"));
    }
}
