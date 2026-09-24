package com.campus.trade.service;

import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.repository.OrderRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单服务：状态机 + 资金结算。
 * 状态机：PENDING → PAID → SHIPPED → COMPLETED
 *                    ↘ CANCELLED
 *         PAID/SHIPPED → REFUNDED
 * 资金规则：
 * - 买家支付：扣买家余额（或走第三方支付）
 * - 买家确认收货：买家付款 → 卖家余额入账
 * - 退款：退回买家余额，商品恢复在售
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        BalanceService balanceService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.balanceService = balanceService;
    }

    /** 买家下单：创建 PENDING 订单，商品保持 ON_SALE（支付成功后才置 SOLD） */
    @Transactional
    public Order createOrder(Long buyerId, Long productId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Product product = productRepository.findWithCreatorById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new IllegalStateException("商品当前不可购买");
        }
        if (product.getCreator() != null && buyerId.equals(product.getCreator().getId())) {
            throw new IllegalStateException("不能购买自己发布的商品");
        }
        Order order = new Order();
        order.setProduct(product);
        order.setBuyer(buyer);
        order.setAmount(product.getPrice());
        order.setStatus(Order.STATUS_PENDING);
        return orderRepository.save(order);
    }

    /** 支付成功后推进订单到 PAID，商品置 SOLD */
    @Transactional
    public void markPaid(Long orderId) {
        Order order = orderRepository.findWithProductAndBuyerById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!Order.STATUS_PENDING.equals(order.getStatus())) {
            return; // 幂等
        }
        order.setStatus(Order.STATUS_PAID);
        orderRepository.save(order);
        Product product = order.getProduct();
        if ("ON_SALE".equals(product.getStatus())) {
            product.setStatus("SOLD");
            productRepository.save(product);
        }
    }

    /** 卖家发货：PAID → SHIPPED */
    @Transactional
    public Order ship(Long orderId, Long sellerId, String shippingCompany, String trackingNo) {
        Order order = orderRepository.findWithProductAndBuyerById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        assertSeller(order, sellerId);
        if (!Order.STATUS_PAID.equals(order.getStatus())) {
            throw new IllegalStateException("仅已付款订单可发货，当前：" + order.getStatus());
        }
        order.setStatus(Order.STATUS_SHIPPED);
        order.setShippingCompany(shippingCompany);
        order.setTrackingNo(trackingNo);
        order.setShippedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /** 买家确认收货：SHIPPED → COMPLETED，卖家收款到余额 */
    @Transactional
    public Order complete(Long orderId, Long buyerId) {
        Order order = orderRepository.findWithProductAndBuyerById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        assertBuyer(order, buyerId);
        if (!Order.STATUS_SHIPPED.equals(order.getStatus())) {
            throw new IllegalStateException("仅已发货订单可确认收货，当前：" + order.getStatus());
        }
        order.setStatus(Order.STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        // 卖家收款：订单金额入账卖家余额
        Long sellerId = order.getProduct().getCreator() == null
                ? null : order.getProduct().getCreator().getId();
        if (sellerId != null) {
            balanceService.credit(sellerId, order.getAmount());
        }
        return order;
    }

    /** 取消订单：PENDING → CANCELLED，商品恢复 ON_SALE */
    @Transactional
    public Order cancel(Long orderId, Long userId) {
        Order order = orderRepository.findWithProductAndBuyerById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        assertBuyerOrSeller(order, userId);
        if (!Order.STATUS_PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("仅待付款订单可取消，当前：" + order.getStatus());
        }
        order.setStatus(Order.STATUS_CANCELLED);
        orderRepository.save(order);
        Product product = order.getProduct();
        if ("SOLD".equals(product.getStatus())) {
            product.setStatus("ON_SALE");
            productRepository.save(product);
        }
        return order;
    }

    /** 退款：PAID/SHIPPED → REFUNDED，退买家余额，商品恢复 ON_SALE */
    @Transactional
    public Order refund(Long orderId, Long buyerId, String reason) {
        Order order = orderRepository.findWithProductAndBuyerById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        assertBuyer(order, buyerId);
        if (!Order.STATUS_PAID.equals(order.getStatus()) && !Order.STATUS_SHIPPED.equals(order.getStatus())) {
            throw new IllegalStateException("仅已付款/已发货订单可退款，当前：" + order.getStatus());
        }
        order.setStatus(Order.STATUS_REFUNDED);
        order.setRefundReason(reason);
        order.setRefundedAt(LocalDateTime.now());
        orderRepository.save(order);
        // 退款到买家余额
        balanceService.credit(buyerId, order.getAmount());
        // 商品恢复在售
        Product product = order.getProduct();
        if ("SOLD".equals(product.getStatus())) {
            product.setStatus("ON_SALE");
            productRepository.save(product);
        }
        return order;
    }

    /** 超时未支付自动取消（定时任务调用） */
    @Transactional
    public void cancelExpiredPendingOrders(LocalDateTime before) {
        for (Order order : orderRepository.findByStatusAndCreatedAtBefore(Order.STATUS_PENDING, before)) {
            order.setStatus(Order.STATUS_CANCELLED);
            orderRepository.save(order);
            Product product = order.getProduct();
            if ("SOLD".equals(product.getStatus())) {
                product.setStatus("ON_SALE");
                productRepository.save(product);
            }
        }
    }

    private void assertBuyer(Order order, Long userId) {
        if (!userId.equals(order.getBuyer().getId())) {
            throw new IllegalStateException("仅买家可操作");
        }
    }

    private void assertSeller(Order order, Long userId) {
        Long sellerId = order.getProduct().getCreator() == null
                ? null : order.getProduct().getCreator().getId();
        if (sellerId == null || !userId.equals(sellerId)) {
            throw new IllegalStateException("仅卖家可操作");
        }
    }

    private void assertBuyerOrSeller(Order order, Long userId) {
        Long sellerId = order.getProduct().getCreator() == null
                ? null : order.getProduct().getCreator().getId();
        boolean isBuyer = userId.equals(order.getBuyer().getId());
        boolean isSeller = sellerId != null && userId.equals(sellerId);
        if (!isBuyer && !isSeller) {
            throw new IllegalStateException("无权操作该订单");
        }
    }
}
