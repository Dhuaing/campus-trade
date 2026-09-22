package com.campus.trade.audit;

import com.campus.trade.entity.AuditLog;
import com.campus.trade.repository.AuditLogRepository;
import com.campus.trade.security.Audited;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计切面：拦截 @Audited 管理端写操作，业务成功（2xx）后写审计日志。
 * 审计写入失败不阻断业务（best-effort）。
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int DETAIL_MAX = 500;

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();
        boolean success = !(result instanceof ResponseEntity<?> re) || re.getStatusCode().is2xxSuccessful();
        if (success) {
            saveLog(joinPoint, audited);
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Audited audited) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(audited.action());
            entry.setTargetType(audited.targetType());

            // 操作人：JwtAuthFilter 写入的 SecurityContext（principal=userId, details=username）
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long adminId) {
                entry.setAdminId(adminId);
                entry.setAdminUsername(String.valueOf(auth.getDetails()));
            } else {
                entry.setAdminId(0L);
                entry.setAdminUsername("unknown");
            }

            // 目标 ID：第一个 Long 参数
            Arrays.stream(joinPoint.getArgs())
                    .filter(a -> a instanceof Long)
                    .map(a -> (Long) a)
                    .findFirst()
                    .ifPresent(entry::setTargetId);

            // 入参摘要：序列化非 Long 参数（如驳回原因）
            String detail = Arrays.stream(joinPoint.getArgs())
                    .filter(a -> !(a instanceof Long))
                    .map(this::serialize)
                    .filter(s -> !s.isBlank())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            if (detail.length() > DETAIL_MAX) {
                detail = detail.substring(0, DETAIL_MAX);
            }
            entry.setDetail(detail);

            entry.setIp(resolveIp());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // 审计 best-effort：失败仅记录错误，不影响业务
            log.error("写入审计日志失败 action={}", audited.action(), e);
        }
    }

    private String serialize(Object arg) {
        if (arg == null) {
            return "";
        }
        if (arg instanceof String s) {
            return s.length() > DETAIL_MAX ? s.substring(0, DETAIL_MAX) : s;
        }
        try {
            return objectMapper.writeValueAsString(arg);
        } catch (Exception e) {
            return String.valueOf(arg);
        }
    }

    /** Railway 等代理场景优先取 X-Forwarded-For 首个 IP */
    private String resolveIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
