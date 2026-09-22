package com.campus.trade.repository;

import com.campus.trade.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    List<Role> findByCodeIn(Collection<String> codes);

    /** 查询某用户拥有的角色（通过 user_roles 关联） */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    List<Role> findByUserId(@Param("userId") Long userId);

    /** 用户是否已配置角色（用于存量数据补授权） */
    @Query("SELECT COUNT(r) > 0 FROM Role r JOIN r.users u WHERE u.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    /** 批量取角色并预取权限（单集合预取，避免多袋抓取问题） */
    @EntityGraph(attributePaths = "permissions")
    List<Role> findWithPermissionsByIdIn(Collection<Long> ids);
}
