package com.campus.trade.repository;

import com.campus.trade.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    /** 全量用户并预取角色（用于启动期存量补授权） */
    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u FROM User u")
    List<User> findAllWithRoles();

    /** 管理端用户列表：q 匹配用户名或昵称（忽略大小写），q 为空时全量分页 */
    @Query("SELECT u FROM User u WHERE (:q IS NULL OR :q = '' "
            + "OR lower(u.username) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(coalesce(u.nickname, '')) LIKE lower(concat('%', :q, '%')))")
    Page<User> searchByKeyword(@Param("q") String q, Pageable pageable);
}
