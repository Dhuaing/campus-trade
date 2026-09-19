package com.campus.trade.repository;

import com.campus.trade.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 商品数据访问层
 */
public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @EntityGraph(attributePaths = "creator")
    List<Product> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = "creator")
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findWithCreatorById(@Param("id") Long id);
}
