package com.cryptotrading.service;

import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {

    private final TransactionRepository transactionRepository;

    /**
     * 매수 가능 여부 체크 (모든 리스크 조건)
     */
    public RiskCheckResult canBuy(String userId, String market, BigDecimal amount, TradingSetting setting) {
        log.debug("리스크 체크 시작: userId={}, market={}, amount={}", userId, market, amount);
        
        // 1. 일일 거래 한도 체크
        if (!checkDailyLimit(userId, amount, setting)) {
            return RiskCheckResult.fail("일일 거래 한도 초과");
        }
        
        // 2. 종목당 최대 보유 건수 체크
        if (!checkMaxHoldings(userId, market, setting)) {
            return RiskCheckResult.fail(
                    String.format("종목당 최대 보유 건수 초과 (%d건)", setting.getMaxHoldingsPerCoin()));
        }
        
        // 3. 총 투자금 대비 종목별 최대 투자 비율 체크 (20%)
        // 추후 구현 가능
        
        log.info("리스크 체크 통과: userId={}, market={}", userId, market);
        return RiskCheckResult.pass();
    }

    /**
     * 일일 거래 한도 체크
     */
    public boolean checkDailyLimit(String userId, BigDecimal newAmount, TradingSetting setting) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        // 오늘 매수한 총 금액 조회
        BigDecimal todayTotal = transactionRepository
                .sumTodayBuyAmount(userId, startOfDay, endOfDay);
        
        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        
        BigDecimal afterBuy = todayTotal.add(newAmount);
        boolean withinLimit = afterBuy.compareTo(setting.getDailyLimitAmount()) <= 0;
        
        log.debug("일일 한도 체크: 오늘 매수액={}, 신규={}, 한도={}, 통과={}", 
                todayTotal, newAmount, setting.getDailyLimitAmount(), withinLimit);
        
        return withinLimit;
    }

    /**
     * 종목당 최대 보유 건수 체크
     */
    public boolean checkMaxHoldings(String userId, String market, TradingSetting setting) {
        long currentHoldings = transactionRepository
                .countByUserIdAndCoinSymbolAndStatus(userId, market, TransactionStatus.HOLDING);
        
        boolean withinLimit = currentHoldings < setting.getMaxHoldingsPerCoin();
        
        log.debug("보유 건수 체크: market={}, 현재={}건, 최대={}건, 통과={}", 
                market, currentHoldings, setting.getMaxHoldingsPerCoin(), withinLimit);
        
        return withinLimit;
    }

    /**
     * 일일 남은 거래 가능 금액 조회
     */
    public BigDecimal getRemainingDailyLimit(String userId, TradingSetting setting) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        BigDecimal todayTotal = transactionRepository
                .sumTodayBuyAmount(userId, startOfDay, endOfDay);
        
        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        
        return setting.getDailyLimitAmount().subtract(todayTotal);
    }

    /**
     * 특정 종목의 남은 보유 가능 건수
     */
    public int getRemainingHoldings(String userId, String market, TradingSetting setting) {
        long currentHoldings = transactionRepository
                .countByUserIdAndCoinSymbolAndStatus(userId, market, TransactionStatus.HOLDING);
        
        return setting.getMaxHoldingsPerCoin() - (int) currentHoldings;
    }

    /**
     * 리스크 체크 결과 클래스
     */
    public static class RiskCheckResult {
        private final boolean passed;
        private final String reason;

        private RiskCheckResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        public static RiskCheckResult pass() {
            return new RiskCheckResult(true, null);
        }

        public static RiskCheckResult fail(String reason) {
            return new RiskCheckResult(false, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }
    }
}