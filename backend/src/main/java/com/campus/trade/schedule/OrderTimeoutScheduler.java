package com.campus.trade.schedule;

import com.campus.trade.service.OrderService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时定时任务：每分钟扫描超过指定时间未支付的订单，自动取消并恢复商品在售。
 * 超时时间可通过 order.timeout-minutes 配置（默认 30 分钟）。
 */
@Component
public class OrderTimeoutScheduler {

    /** 超时阈值（分钟），可配置 */
    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    private final OrderService orderService;

    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 60000) // 每分钟执行
    public void cancelExpiredOrders() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(timeoutMinutes);
        System.out.println("[OrderTimeoutScheduler] 扫描超时订单，阈值: " + before + ", 当前超时配置: " + timeoutMinutes + "分钟");
        orderService.cancelExpiredPendingOrders(before);
    }
}
