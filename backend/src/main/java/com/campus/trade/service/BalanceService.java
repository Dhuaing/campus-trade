package com.campus.trade.service;

import com.campus.trade.entity.Balance;
import com.campus.trade.entity.User;
import com.campus.trade.repository.BalanceRepository;
import com.campus.trade.repository.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资金账户服务：余额管理。
 * 使用 @Version 乐观锁 + 悲观锁查询防止并发超扣。
 */
@Service
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final UserRepository userRepository;

    public BalanceService(BalanceRepository balanceRepository, UserRepository userRepository) {
        this.balanceRepository = balanceRepository;
        this.userRepository = userRepository;
    }

    /** 获取或创建用户账户 */
    @Transactional
    public Balance getOrCreate(Long userId) {
        return balanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
                    Balance b = new Balance();
                    b.setUser(user);
                    b.setAmount(BigDecimal.ZERO);
                    return balanceRepository.save(b);
                });
    }

    /** 查询余额 */
    public BigDecimal getBalance(Long userId) {
        return balanceRepository.findByUserId(userId)
                .map(Balance::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * 扣款（支付时调用）。使用悲观锁确保并发安全。
     * @return true 扣款成功，false 余额不足
     */
    @Transactional
    public boolean deduct(Long userId, BigDecimal amount) {
        Balance balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("账户不存在"));
        if (balance.getAmount().compareTo(amount) < 0) {
            return false;
        }
        balance.setAmount(balance.getAmount().subtract(amount));
        balanceRepository.save(balance);
        return true;
    }

    /**
     * 入账（退款/卖家收款时调用）。使用悲观锁。
     */
    @Transactional
    public void credit(Long userId, BigDecimal amount) {
        Balance balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
                    Balance b = new Balance();
                    b.setUser(user);
                    b.setAmount(BigDecimal.ZERO);
                    return balanceRepository.save(b);
                });
        balance.setAmount(balance.getAmount().add(amount));
        balanceRepository.save(balance);
    }
}
