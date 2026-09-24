package com.campus.trade.repository;

import com.campus.trade.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 按支付单号查询（out_trade_no，回调入口），预取订单与买家 */
    @EntityGraph(attributePaths = {"order", "order.buyer", "order.product"})
    Optional<Payment> findByPaymentNo(String paymentNo);

    /** 按订单查询支付单 */
    Optional<Payment> findByOrderId(Long orderId);
}
