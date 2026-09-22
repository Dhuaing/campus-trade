package com.campus.trade.config;

import com.campus.trade.entity.Permission;
import com.campus.trade.entity.Role;
import com.campus.trade.entity.User;
import com.campus.trade.repository.PermissionRepository;
import com.campus.trade.repository.RoleRepository;
import com.campus.trade.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * RBAC 种子初始化：
 * 1. 权限（5 个接口级权限码）与角色（ADMIN/USER）及关联；
 * 2. 存量用户补授 ROLE_USER、状态归一化为 ACTIVE；
 * 3. 确保默认管理员存在（ADMIN_USERNAME/ADMIN_PASSWORD 可覆盖）。
 * 幂等：重复启动不做重复写入。
 */
@Configuration
public class AdminBootstrap {

    /** 权限码 → 名称 */
    private static final Map<String, String> PERMISSIONS = Map.of(
            "user:view", "用户查看",
            "user:ban", "用户封禁",
            "product:audit", "商品审核",
            "product:remove", "商品强制下架",
            "auditlog:view", "审计日志查看"
    );

    @Bean
    public CommandLineRunner initRbac(RoleRepository roleRepository,
                                      PermissionRepository permissionRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${app.admin.username:admin}") String adminUsername,
                                      @Value("${app.admin.password:admin1234}") String adminPassword) {
        return args -> {
            // 1. 种子权限
            Map<String, Permission> permissionMap = new java.util.HashMap<>();
            PERMISSIONS.forEach((code, name) -> {
                Permission p = permissionRepository.findByCode(code).orElseGet(() -> {
                    Permission np = new Permission();
                    np.setCode(code);
                    np.setName(name);
                    return permissionRepository.save(np);
                });
                permissionMap.put(code, p);
            });

            // 2. 种子角色：USER（无权限）与 ADMIN（全部权限）
            Role userRole = roleRepository.findByCode("USER").orElseGet(() -> {
                Role r = new Role();
                r.setCode("USER");
                r.setName("普通用户");
                return roleRepository.save(r);
            });
            Role adminRole = roleRepository.findByCode("ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setCode("ADMIN");
                r.setName("管理员");
                return roleRepository.save(r);
            });
            if (adminRole.getPermissions().isEmpty()) {
                adminRole.setPermissions(new java.util.HashSet<>(permissionMap.values()));
                roleRepository.save(adminRole);
            }

            // 3. 存量用户补授 ROLE_USER + 状态归一化
            for (User user : userRepository.findAll()) {
                boolean changed = false;
                if (!roleRepository.existsByUserId(user.getId())) {
                    user.getRoles().add(userRole);
                    changed = true;
                }
                if (user.getStatus() == null) {
                    user.setStatus(User.STATUS_ACTIVE);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(user);
                }
            }

            // 4. 默认管理员
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setNickname("系统管理员");
                admin.setStatus(User.STATUS_ACTIVE);
                admin.getRoles().add(adminRole);
                userRepository.save(admin);
            } else {
                // 已存在同名账号时确保其拥有 ADMIN 角色
                User existing = userRepository.findByUsername(adminUsername).orElse(null);
                if (existing != null && !roleRepository.existsByUserId(existing.getId())) {
                    existing.getRoles().add(adminRole);
                    userRepository.save(existing);
                }
            }
        };
    }
}
