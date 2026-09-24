package com.campus.trade.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 模拟支付网关：不调真实第三方，直接返回支付单号。
 * 前端通过 /api/payments/{no}/mock-success 触发支付成功回调。
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String createPayment(String paymentNo, BigDecimal amount, String description) {
        // 模拟支付直接返回支付单号，前端拿到后调模拟成功接口
        return paymentNo;
    }

    @Override
    public boolean verifyCallback(String paymentNo, String transactionId, BigDecimal amount) {
        // 模拟支付跳过验签
        return true;
    }

    @Override
    public String getCode() {
        return "MOCK";
    }
}
