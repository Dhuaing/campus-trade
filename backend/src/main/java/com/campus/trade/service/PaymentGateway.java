package com.campus.trade.service;

import java.math.BigDecimal;

/**
 * 支付网关抽象：真实支付（微信/支付宝）与模拟支付统一接口。
 * 校园场景先用 MOCK，后续可替换为真实实现。
 */
public interface PaymentGateway {

    /**
     * 创建支付单，返回支付参数（如二维码链接、支付链接等）。
     * @param paymentNo 业务支付单号（out_trade_no）
     * @param amount 支付金额
     * @param description 商品描述
     * @return 支付参数（模拟支付返回支付单号本身，前端直接调模拟支付成功接口）
     */
    String createPayment(String paymentNo, BigDecimal amount, String description);

    /**
     * 支付回调签名校验（真实支付需要，模拟支付直接通过）。
     */
    boolean verifyCallback(String paymentNo, String transactionId, BigDecimal amount);

    /** 网关标识：MOCK / WECHAT / ALIPAY */
    String getCode();
}
