package com.campus.trade.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端写操作审计注解：由 AuditAspect 拦截，操作成功（2xx）后落审计日志。
 * 约定：方法第一个 Long 参数作为目标 ID。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** 动作码，如 USER_BAN / PRODUCT_APPROVE */
    String action();

    /** 目标类型，如 USER / PRODUCT */
    String targetType();
}
