package com.campus.trade.repository;

import com.campus.trade.entity.Product;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 商品自定义数据访问接口
 */
public interface ProductRepositoryCustom {

    /**
     * 多关键词搜索（分页）：标题或描述命中任一关键词即返回，标题命中优先排序，同权重按发布时间倒序。
     * keywords 每个元素需自带 LIKE 通配符（如 %iPad%）。
     */
    Page<Product> searchMulti(String status, List<String> keywords, Pageable pageable);
}
