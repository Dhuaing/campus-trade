package com.campus.trade.controller;

import com.campus.trade.service.BalanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资金账户接口：查询余额、充值（模拟）。
 */
@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /** 查询当前用户余额 */
    @GetMapping
    public ResponseEntity<?> get(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        BigDecimal balance = balanceService.getBalance(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "amount", balance));
    }

    /** 模拟充值（真实场景应对接支付网关） */
    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(@Valid @RequestBody RechargeRequest req,
                                      Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        Long userId = (Long) authentication.getPrincipal();
        balanceService.credit(userId, req.amount());
        return ResponseEntity.ok(Map.of("message", "充值成功", "amount", balanceService.getBalance(userId)));
    }

    public record RechargeRequest(
            @NotNull(message = "金额不能为空")
            @Positive(message = "金额必须大于0")
            BigDecimal amount
    ) {}
}
