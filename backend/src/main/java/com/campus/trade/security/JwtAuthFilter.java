package com.campus.trade.security;

import com.campus.trade.entity.User;
import com.campus.trade.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 Authorization: Bearer <token> 中解析用户身份并写入 SecurityContext。
 * - 按用户当前角色/权限构建 GrantedAuthority（数据库实时加载，封禁即时生效）
 * - 账号被封禁：直接返回 403，不再放行
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RbacService rbacService;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         UserRepository userRepository,
                         RbacService rbacService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.rbacService = rbacService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    // token 对应用户已不存在：按未认证处理
                    chain.doFilter(request, response);
                    return;
                }
                if (user.isBanned()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"账号已被封禁，请联系管理员\"}");
                    return;
                }
                String username = claims.get("username", String.class);
                var roles = rbacService.loadRoles(userId);
                var authorities = rbacService.authoritiesOf(roles);
                var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails(username);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // token 无效则不设置身份，由后续安全规则决定是否放行
            }
        }
        chain.doFilter(request, response);
    }
}
