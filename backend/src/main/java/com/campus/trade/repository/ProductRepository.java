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
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "creator")
    List<Product> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 多关键词搜索：关键词按空格拆分，标题或描述命中任一关键词即返回；
     * 标题命中权重高于仅描述命中，同权重按发布时间倒序。
     * keywords 每个元素需自带 LIKE 通配符（如 %iPad%）。
     */
    @Query(value = "SELECT p.* FROM products p WHERE p.status = :status AND (" +
            "p.title ILIKE ANY (:keywords) OR p.description ILIKE ANY (:keywords)" +
            ") ORDER BY CASE WHEN p.title ILIKE ANY (:keywords) THEN 2 ELSE 1 END DESC, " +
            "p.created_at DESC",
            nativeQuery = true)
    List<Product> searchMulti(@Param("status") String status,
                              @Param("keywords") String[] keywords);

    @EntityGraph(attributePaths = "creator")
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findWithCreatorById(@Param("id") Long id);
}
