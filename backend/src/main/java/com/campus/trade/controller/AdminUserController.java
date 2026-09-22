package com.campus.trade.controller;

import com.campus.trade.entity.User;
import com.campus.trade.repository.RoleRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.security.Audited;
import com.campus.trade.ws.WsSessionRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-用户管理：列表检索、封禁/解封
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WsSessionRegistry wsSessionRegistry;

    public AdminUserController(UserRepository userRepository,
                               RoleRepository roleRepository,
                               WsSessionRegistry wsSessionRegistry) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.wsSessionRegistry = wsSessionRegistry;
    }

    /** 用户分页列表：q 匹配用户名/昵称 */
    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public Map<String, Object> list(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<User> result = userRepository.searchByKeyword(q, pageable);

        List<Map<String, Object>> content = result.getContent().stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("studentId", u.getStudentId());
            item.put("status", u.isBanned() ? User.STATUS_BANNED : User.STATUS_ACTIVE);
            item.put("roles", roleRepository.findByUserId(u.getId()).stream()
                    .map(r -> r.getCode()).toList());
            item.put("createdAt", u.getCreatedAt());
            return item;
        }).toList();

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        body.put("totalElements", result.getTotalElements());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("hasMore", result.hasNext());
        return body;
    }

    /** 封禁用户：禁止封自己；在线 WS 连接立即关闭 */
    @PutMapping("/{id}/ban")
    @PreAuthorize("hasAuthority('user:ban')")
    @Audited(action = "USER_BAN", targetType = "USER")
    public ResponseEntity<?> ban(@PathVariable Long id, Authentication authentication) {
        Long adminId = (Long) authentication.getPrincipal();
        if (adminId.equals(id)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "不能封禁自己"));
        }
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用户不存在"));
        }
        user.setStatus(User.STATUS_BANNED);
        userRepository.save(user);
        wsSessionRegistry.closeAll(id);
        return ResponseEntity.ok(Map.of("id", id, "status", User.STATUS_BANNED));
    }

    /** 解封用户 */
    @PutMapping("/{id}/unban")
    @PreAuthorize("hasAuthority('user:ban')")
    @Audited(action = "USER_UNBAN", targetType = "USER")
    public ResponseEntity<?> unban(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "用户不存在"));
        }
        user.setStatus(User.STATUS_ACTIVE);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("id", id, "status", User.STATUS_ACTIVE));
    }
}
