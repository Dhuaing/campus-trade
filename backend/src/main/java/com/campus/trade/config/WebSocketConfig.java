package com.campus.trade.config;

import com.campus.trade.ws.ChatWebSocketHandler;
import com.campus.trade.security.WsAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：原生 WS 端点 /ws，握手时通过 ?token= 做 JWT 认证
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WsAuthInterceptor wsAuthInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           WsAuthInterceptor wsAuthInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.wsAuthInterceptor = wsAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws")
                .addInterceptors(wsAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
