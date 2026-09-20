package com.campus.trade.repository;

import com.campus.trade.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * 商品自定义数据访问实现
 */
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Product> searchMulti(String status, List<String> keywords, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // entity query, eagerly fetch creator to avoid LazyInitializationException
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);
        root.fetch("creator", JoinType.LEFT);

        // each keyword matches title OR description (case-insensitive)
        List<Predicate> keywordOr = new ArrayList<>();
        List<Predicate> titleMatches = new ArrayList<>();
        for (String kw : keywords) {
            String pattern = kw.toLowerCase();
            Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
            keywordOr.add(cb.or(titleLike, descLike));
            titleMatches.add(titleLike);
        }
        Predicate keywordPred = cb.or(keywordOr.toArray(new Predicate[0]));
        Predicate statusPred = cb.equal(root.get("status"), status);
        Predicate where = cb.and(statusPred, keywordPred);
        cq.where(where);

        // title hits rank above description-only hits, then newest first
        Predicate anyTitleMatch = cb.or(titleMatches.toArray(new Predicate[0]));
        Expression<Integer> weight = cb.<Integer>selectCase()
                .when(anyTitleMatch, 2)
                .otherwise(1);
        cq.orderBy(cb.desc(weight), cb.desc(root.get("createdAt")));

        List<Product> content = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // count query (no fetch join to keep row count correct)
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<Product> countRoot = countQ.from(Product.class);
        countQ.select(cb.count(countRoot)).where(where);
        long total = em.createQuery(countQ).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
