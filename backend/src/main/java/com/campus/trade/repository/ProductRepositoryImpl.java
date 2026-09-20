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

        List<Predicate> titleMatches = new ArrayList<>();
        cq.where(buildWhere(cb, root, status, keywords, titleMatches));

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

        // count query: predicates must be rebuilt against its own root (Criteria
        // predicates are bound to the root they were created from)
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<Product> countRoot = countQ.from(Product.class);
        countQ.select(cb.count(countRoot))
                .where(buildWhere(cb, countRoot, status, keywords, new ArrayList<>()));

        long total = em.createQuery(countQ).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /** 生成 status + 多关键词 OR 匹配的 where 条件，titleMatches 输出标题命中项 */
    private Predicate buildWhere(CriteriaBuilder cb, Root<Product> root, String status,
                                 List<String> keywords, List<Predicate> titleMatchesOut) {
        List<Predicate> keywordOr = new ArrayList<>();
        for (String kw : keywords) {
            String pattern = kw.toLowerCase();
            Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
            keywordOr.add(cb.or(titleLike, descLike));
            titleMatchesOut.add(titleLike);
        }
        Predicate keywordPred = cb.or(keywordOr.toArray(new Predicate[0]));
        Predicate statusPred = cb.equal(root.get("status"), status);
        return cb.and(statusPred, keywordPred);
    }
}
