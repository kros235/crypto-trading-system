package com.cryptotrading.service;

import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {

    private final TransactionRepository transactionRepository;
    
    private static final int SCALE = 8;

    /**
     * 매수 가능 여부 체크 (모든 리스크 조건)
     */
    public RiskCheckResult canBuy(String userId, String market, BigDecimal amount, TradingSetting setting) {
        log.debug("리스크 체크 시작: userId={}, market={}, amount={}", userId, market, amount);
        
        // 긴급 정지 조건 체크 (dailyStopLossPct)
        if (!checkDailyStopLoss(userId, setting)) {
            return RiskCheckResult.fail("긴급 정지 발동 - 일일 손실 한도 도달");
        }
        
        // 1. 일일 거래 한도 체크 (dailyTradeLimitPct 적용)
        if (!checkDailyLimit(userId, amount, setting)) {
            return RiskCheckResult.fail("일일 거래 한도 초과");
        }
        
        // 2. 종목당 최대 보유 건수 체크
        if (!checkMaxHoldings(userId, market, setting)) {
            return RiskCheckResult.fail(
                    String.format("종목당 최대 보유 건수 초과 (%d건)", setting.getMaxHoldingsPerCoin()));
        }
        
        // 단일 종목 최대 비중 체크 (maxPositionPct)
        if (!checkMaxPosition(userId, market, amount, setting)) {
            return RiskCheckResult.fail(
                    String.format("단일 종목 최대 비중 초과 (%d%%)", setting.getMaxPositionPct()));
        }
        
        log.info("리스크 체크 통과: userId={}, market={}", userId, market);
        return RiskCheckResult.pass();
    }

    /**
     * 일일 거래 한도 체크 (dailyTradeLimitPct 적용)
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
        
        // dailyTradeLimitPct 비율 적용
        BigDecimal effectiveDailyLimit = calculateEffectiveDailyLimit(setting);
        
        BigDecimal afterBuy = todayTotal.add(newAmount);
        boolean withinLimit = afterBuy.compareTo(effectiveDailyLimit) <= 0;
        
        log.debug("일일 한도 체크: 오늘 매수액={}, 신규={}, 실제한도={} (기준액={} x {}%), 통과={}", 
                todayTotal, newAmount, effectiveDailyLimit, 
                setting.getDailyLimitAmount(), setting.getDailyTradeLimitPct(), withinLimit);
        
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
     * 단일 종목 최대 비중 체크 (maxPositionPct)
     */
    public boolean checkMaxPosition(String userId, String market, BigDecimal newAmount, TradingSetting setting) {
        // maxPositionPct가 100이면 제한 없음
        Integer maxPositionPct = setting.getMaxPositionPct();
        if (maxPositionPct == null || maxPositionPct >= 100) {
            return true;
        }
        
        // 현재 해당 종목 보유 금액
        BigDecimal currentHoldingAmount = transactionRepository
                .sumHoldingAmountByCoin(userId, market);
        
        if (currentHoldingAmount == null) {
            currentHoldingAmount = BigDecimal.ZERO;
        }
        
        // 매수 후 해당 종목 총 금액
        BigDecimal afterBuyAmount = currentHoldingAmount.add(newAmount);
        
        // 최대 허용 금액 = 일일 한도 금액 × maxPositionPct / 100
        BigDecimal maxAllowedAmount = setting.getDailyLimitAmount()
                .multiply(new BigDecimal(maxPositionPct))
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        
        boolean withinLimit = afterBuyAmount.compareTo(maxAllowedAmount) <= 0;
        
        log.debug("종목 비중 체크: market={}, 현재보유={}, 신규={}, 매수후={}, 최대허용={} ({}%), 통과={}", 
                market, currentHoldingAmount, newAmount, afterBuyAmount, maxAllowedAmount, maxPositionPct, withinLimit);
        
        return withinLimit;
    }

    /**
     * 신규 추가: 긴급 정지 조건 체크 (dailyStopLossPct)
     */
    public boolean checkDailyStopLoss(String userId, TradingSetting setting) {
        // dailyStopLossPct가 0이면 사용 안함
        Integer dailyStopLossPct = setting.getDailyStopLossPct();
        if (dailyStopLossPct == null || dailyStopLossPct >= 0) {
            return true;  // 제한 없음
        }
        
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        // 오늘 실현 손익
        BigDecimal todayProfitLoss = transactionRepository
                .sumTodayProfitLoss(userId, startOfDay, endOfDay);
        
        if (todayProfitLoss == null) {
            todayProfitLoss = BigDecimal.ZERO;
        }
        
        // 손실 한도 금액 = 일일 한도 금액 × dailyStopLossPct / 100 (음수)
        BigDecimal stopLossLimit = setting.getDailyLimitAmount()
                .multiply(new BigDecimal(dailyStopLossPct))
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        
        // 오늘 손익이 손실 한도보다 크면 통과 (손실 한도는 음수)
        boolean withinLimit = todayProfitLoss.compareTo(stopLossLimit) > 0;
        
        log.debug("긴급 정지 체크: 오늘손익={}, 손실한도={} (기준액={} x {}%), 통과={}", 
                todayProfitLoss, stopLossLimit, setting.getDailyLimitAmount(), dailyStopLossPct, withinLimit);
        
        if (!withinLimit) {
            log.warn("⚠️ 긴급 정지 발동! userId={}, 오늘손익={}, 손실한도={}", 
                    userId, todayProfitLoss, stopLossLimit);
        }
        
        return withinLimit;
    }

    /**
     * 긴급 정지 상태 확인 (외부에서 호출용)
     */
    public boolean isEmergencyStopActive(String userId, TradingSetting setting) {
        return !checkDailyStopLoss(userId, setting);
    }

    /**
     * 일일 남은 거래 가능 금액 조회 (dailyTradeLimitPct 적용)
     */
    public BigDecimal getRemainingDailyLimit(String userId, TradingSetting setting) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        BigDecimal todayTotal = transactionRepository
                .sumTodayBuyAmount(userId, startOfDay, endOfDay);
        
        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        
        // dailyTradeLimitPct 비율 적용
        BigDecimal effectiveDailyLimit = calculateEffectiveDailyLimit(setting);
        
        return effectiveDailyLimit.subtract(todayTotal);
    }

    /**
     * 실제 일일 한도 계산 (dailyTradeLimitPct 적용)
     */
    public BigDecimal calculateEffectiveDailyLimit(TradingSetting setting) {
        Integer dailyTradeLimitPct = setting.getDailyTradeLimitPct();
        
        // 100%이면 dailyLimitAmount 전체 사용
        if (dailyTradeLimitPct == null || dailyTradeLimitPct >= 100) {
            return setting.getDailyLimitAmount();
        }
        
        return setting.getDailyLimitAmount()
                .multiply(new BigDecimal(dailyTradeLimitPct))
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
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
     * 특정 종목의 남은 투자 가능 금액 (maxPositionPct 기준) 
     */
    public BigDecimal getRemainingPositionAmount(String userId, String market, TradingSetting setting) {
        Integer maxPositionPct = setting.getMaxPositionPct();
        
        // 100%이면 제한 없음
        if (maxPositionPct == null || maxPositionPct >= 100) {
            return getRemainingDailyLimit(userId, setting);
        }
        
        // 현재 해당 종목 보유 금액
        BigDecimal currentHoldingAmount = transactionRepository
                .sumHoldingAmountByCoin(userId, market);
        
        if (currentHoldingAmount == null) {
            currentHoldingAmount = BigDecimal.ZERO;
        }
        
        // 최대 허용 금액
        BigDecimal maxAllowedAmount = setting.getDailyLimitAmount()
                .multiply(new BigDecimal(maxPositionPct))
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        
        return maxAllowedAmount.subtract(currentHoldingAmount);
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