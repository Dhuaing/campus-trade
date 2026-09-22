package com.campus.trade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理端 SPA 转发：/admin 与 /admin/ 转发到 /admin/index.html。
 * 静态资源由 Spring Boot 默认 classpath:/static/** 映射（/admin/assets/**）。
 */
@Configuration
public class AdminSpaConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/admin", "/admin/");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }
}
