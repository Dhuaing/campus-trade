package com.campus.trade.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 在线会话注册表：按 userId 维护连接（支持多端），提供定向推送。
 * REST 与 WS 两条发送路径共用，保证在线用户实时收到新消息。
 */
@Component
public class WsSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> online = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WsSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        online.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(Long userId, WebSocketSession session) {
        Set<WebSocketSession> set = online.get(userId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            online.remove(userId, set);
        }
    }

    public boolean isOnline(Long userId) {
        Set<WebSocketSession> set = online.get(userId);
        return set != null && !set.isEmpty();
    }

    /** 向该用户的所有在线会话推送 JSON；失败/关闭的会话被清理 */
    public void push(Long userId, Map<String, Object> payload) {
        Set<WebSocketSession> set = online.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        TextMessage text = new TextMessage(json);
        List<WebSocketSession> dead = new ArrayList<>();
        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) {
                    synchronized (s) {
                        s.sendMessage(text);
                    }
                } else {
                    dead.add(s);
                }
            } catch (Exception e) {
                dead.add(s);
            }
        }
        for (WebSocketSession s : dead) {
            Set<WebSocketSession> owner = online.get(userId);
            if (owner != null) {
                owner.remove(s);
            }
        }
    }
}
