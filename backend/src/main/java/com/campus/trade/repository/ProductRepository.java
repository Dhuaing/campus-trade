package com.campus.trade.repository;

import com.campus.trade.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品数据访问层
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatusOrderByCreatedAtDesc(String status);
}
