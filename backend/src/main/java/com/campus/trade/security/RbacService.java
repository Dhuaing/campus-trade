package com.campus.trade.security;

import com.campus.trade.entity.Role;
import com.campus.trade.repository.RoleRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * RBAC 辅助：加载用户角色与权限，构建 Spring Security GrantedAuthority。
 * JwtAuthFilter（每请求鉴权）与 AuthController（登录/me 响应）共用。
 */
@Component
public class RbacService {

    private final RoleRepository roleRepository;

    public RbacService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /** 加载用户角色并预取权限（两步查询，避免多袋抓取） */
    public List<Role> loadRoles(Long userId) {
        List<Role> roles = roleRepository.findByUserId(userId);
        if (roles.isEmpty()) {
            return roles;
        }
        List<Long> ids = roles.stream().map(Role::getId).toList();
        return roleRepository.findWithPermissionsByIdIn(ids);
    }

    /** 角色 → ROLE_xxx + 权限码 */
    public List<GrantedAuthority> authoritiesOf(List<Role> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            role.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.getCode())));
        }
        return authorities;
    }

    /** 角色编码列表 */
    public List<String> roleCodes(List<Role> roles) {
        return roles.stream().map(Role::getCode).toList();
    }

    /** 权限编码列表（去重） */
    public List<String> permissionCodes(List<Role> roles) {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode())
                .distinct()
                .toList();
    }

    public List<GrantedAuthority> emptyAuthorities() {
        return Collections.emptyList();
    }
}
