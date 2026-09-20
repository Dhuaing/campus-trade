package com.campus.trade.ws;

import com.campus.trade.entity.Message;
import com.campus.trade.entity.User;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 聊天 WebSocket 处理器：
 * - 连接建立后按 userId 注册会话（支持同一账号多端）
 * - 收到 {type:"message", toUserId, content, productId?} 后落库，并实时推送给接收者的所有在线会话
 * - 协议错误回 {type:"error", message}
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final WsSessionRegistry registry;

    public ChatWebSocketHandler(MessageRepository messageRepository,
                                UserRepository userRepository,
                                ProductRepository productRepository,
                                ObjectMapper objectMapper,
                                WsSessionRegistry registry) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        registry.register(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        registry.remove(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage text) {
        Long fromUserId = userId(session);
        if (fromUserId == null) {
            closeQuietly(session);
            return;
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(text.getPayload());
        } catch (Exception e) {
            sendError(session, "无效的消息格式");
            return;
        }
        if (!"message".equals(node.path("type").asText())) {
            sendError(session, "不支持的消息类型");
            return;
        }
        Long toUserId = node.path("toUserId").asLong(0);
        String content = node.path("content").asText("").trim();
        Long productId = node.hasNonNull("productId") ? node.path("productId").asLong() : null;
        if (toUserId <= 0 || content.isEmpty()) {
            sendError(session, "参数不完整");
            return;
        }
        if (content.length() > 1000) {
            sendError(session, "消息最长 1000 字");
            return;
        }
        if (toUserId.equals(fromUserId)) {
            sendError(session, "不能给自己发消息");
            return;
        }
        var fromUser = userRepository.findById(fromUserId).orElse(null);
        var toUser = userRepository.findById(toUserId).orElse(null);
        if (fromUser == null || toUser == null) {
            sendError(session, "用户不存在");
            return;
        }
        var product = productId == null ? null : productRepository.findById(productId).orElse(null);
        Message message = new Message();
        message.setFromUser(fromUser);
        message.setToUser(toUser);
        message.setProduct(product);
        message.setContent(content);
        Message saved;
        try {
            saved = messageRepository.save(message);
        } catch (Exception e) {
            sendError(session, "消息发送失败");
            return;
        }
        registry.push(toUserId, messagePayload(saved, fromUser, toUserId));
    }

    /** 新消息推送 payload（与 REST 会话接口字段一致） */
    public static Map<String, Object> messagePayload(Message saved, User fromUser, Long toUserId) {
        return Map.of(
                "type", "message",
                "message", Map.of(
                        "id", saved.getId(),
                        "fromUserId", fromUser.getId(),
                        "fromUserNickname", fromUser.getNickname(),
                        "toUserId", toUserId,
                        "content", saved.getContent(),
                        "isRead", false,
                        "createdAt", String.valueOf(saved.getCreatedAt())
                )
        );
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "error", "message", message))));
        } catch (Exception ignored) {
        }
    }

    private Long userId(WebSocketSession session) {
        Object v = session.getAttributes().get("userId");
        return v instanceof Long l ? l : null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception ignored) {
        }
    }
}
