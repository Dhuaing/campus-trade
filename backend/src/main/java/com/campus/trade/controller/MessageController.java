package com.campus.trade.controller;

import com.campus.trade.entity.Message;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.repository.MessageRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息接口：收件箱、发送消息
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public MessageController(MessageRepository messageRepository,
                             UserRepository userRepository,
                             ProductRepository productRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /** 当前用户的收件箱（按时间倒序） */
    @GetMapping
    public ResponseEntity<?> inbox(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = (Long) authentication.getPrincipal();
        List<Message> messages = messageRepository.findInboxByUserId(userId);
        List<Map<String, Object>> list = messages.stream().map(m -> Map.<String, Object>of(
                "id", m.getId(),
                "fromUser", Map.of(
                        "id", m.getFromUser().getId(),
                        "nickname", m.getFromUser().getNickname()
                ),
                "productId", m.getProduct() == null ? null : m.getProduct().getId(),
                "productTitle", m.getProduct() == null ? null : m.getProduct().getTitle(),
                "content", m.getContent(),
                "isRead", m.getIsRead(),
                "createdAt", m.getCreatedAt()
        )).toList();
        long unread = messageRepository.countByToUserIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("messages", list, "unread", unread));
    }

    /** 发送消息 */
    @PostMapping
    public ResponseEntity<?> send(@Valid @RequestBody SendMessageRequest req,
                                  Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long fromUserId = (Long) authentication.getPrincipal();
        User fromUser = userRepository.findById(fromUserId).orElse(null);
        User toUser = userRepository.findById(req.toUserId()).orElse(null);
        if (fromUser == null || toUser == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
        }
        Product product = null;
        if (req.productId() != null) {
            product = productRepository.findById(req.productId()).orElse(null);
        }
        Message message = new Message();
        message.setFromUser(fromUser);
        message.setToUser(toUser);
        message.setProduct(product);
        message.setContent(req.content());
        Message saved = messageRepository.save(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId()));
    }

    public record SendMessageRequest(
            @NotNull Long toUserId,
            Long productId,
            @NotBlank @Size(max = 1000) String content
    ) {}
}
