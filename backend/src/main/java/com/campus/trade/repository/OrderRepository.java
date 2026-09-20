package com.campus.trade.repository;

import com.campus.trade.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 订单数据访问层
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"product", "product.creator", "buyer"})
    List<Order> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId);

    @EntityGraph(attributePaths = {"product", "product.creator", "buyer"})
    List<Order> findByProduct_Creator_IdOrderByCreatedAtDesc(Long sellerId);

    @EntityGraph(attributePaths = {"product", "product.creator", "buyer"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findWithProductAndBuyerById(@Param("id") Long id);
}
