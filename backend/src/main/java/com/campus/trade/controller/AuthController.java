package com.campus.trade.controller;

import com.campus.trade.entity.Role;
import com.campus.trade.entity.User;
import com.campus.trade.repository.RoleRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.security.JwtUtil;
import com.campus.trade.security.RbacService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RbacService rbacService;

    public AuthController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RbacService rbacService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rbacService = rbacService;
    }

    /** 注册（默认授予 ROLE_USER） */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "用户名已存在"));
        }
        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank()
                ? req.username() : req.nickname());
        user.setStudentId(req.studentId());
        user.setStatus(User.STATUS_ACTIVE);
        roleRepository.findByCode("USER").ifPresent(r -> user.getRoles().add(r));
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "username", saved.getUsername()));
    }

    /** 登录（封禁账号拒绝登录），返回 token 与角色/权限 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }
        if (user.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "账号已被封禁，请联系管理员"));
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        List<Role> roles = rbacService.loadRoles(user.getId());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", userInfo(user, roles)
        ));
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Role> roles = rbacService.loadRoles(user.getId());
        return ResponseEntity.ok(userInfo(user, roles));
    }

    private Map<String, Object> userInfo(User user, List<Role> roles) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "studentId", user.getStudentId() == null ? "" : user.getStudentId(),
                "status", user.isBanned() ? User.STATUS_BANNED : User.STATUS_ACTIVE,
                "roles", rbacService.roleCodes(roles),
                "permissions", rbacService.permissionCodes(roles)
        );
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 6, max = 50) String password,
            @Size(max = 50) String nickname,
            @Size(max = 30) String studentId
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}
}
