package com.campus.trade.repository;

import com.campus.trade.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品自定义数据访问实现
 */
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Product> searchMulti(String status, List<String> keywords) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

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
        cq.where(cb.and(statusPred, keywordPred));

        // title hits rank above description-only hits, then newest first
        Predicate anyTitleMatch = cb.or(titleMatches.toArray(new Predicate[0]));
        Expression<Integer> weight = cb.<Integer>selectCase()
                .when(anyTitleMatch, 2)
                .otherwise(1);
        cq.orderBy(cb.desc(weight), cb.desc(root.get("createdAt")));

        return em.createQuery(cq).getResultList();
    }
}
