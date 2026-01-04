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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {

    private final TransactionRepository transactionRepository;

    private final TechnicalIndicatorService technicalIndicatorService;
    
    private static final int SCALE = 8;

    // 연속 손절 추적용 캐시 (코인별)
    // Key: "userId:coinSymbol", Value: 연속 손절 횟수
    private final Map<String, Integer> consecutiveStopLossMap = new ConcurrentHashMap<>();
    // Key: "userId:coinSymbol", Value: 매수 금지 해제 시각
    private final Map<String, LocalDateTime> buyBlockedUntilMap = new ConcurrentHashMap<>();


    /**
     * 매수 가능 여부 체크 (모든 리스크 조건)
     */
    public RiskCheckResult canBuy(String userId, String market, BigDecimal amount, TradingSetting setting) {
        log.debug("리스크 체크 시작: userId={}, market={}, amount={}", userId, market, amount);
        
        // 시장 추세 필터 체크 (BTC MA20) 
        if (setting.getUseMarketTrendFilter() != null && setting.getUseMarketTrendFilter()) {
            if (!checkMarketTrendFilter()) {
                return RiskCheckResult.fail("시장 추세 필터 발동 - BTC가 20일 이동평균선 하회");
            }
        }
        
        // 누적 손실률 체크 
        if (!checkCumulativeLossLimit(userId, setting)) {
            return RiskCheckResult.fail("누적 손실 한도 도달 - 거래 중단");
        }
        
        // 연속 손절 제한 체크
        if (!checkConsecutiveStopLossLimit(userId, market, setting)) {
            return RiskCheckResult.fail("연속 손절 제한 - 해당 코인 24시간 매수 금지");
        }

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
     * 시장 추세 필터 체크 (BTC MA20 기준)
     * BTC가 20일 이동평균선 위에 있으면 true (매수 허용)
     * BTC가 20일 이동평균선 아래에 있으면 false (전체 매수 중단)
     */
    public boolean checkMarketTrendFilter() {
        try {
            var btcIndicators = technicalIndicatorService.calculateIndicators("KRW-BTC");
            
            if (btcIndicators == null || btcIndicators.getCurrentPrice() == null || btcIndicators.getMa20() == null) {
                log.warn("BTC 지표 조회 실패 - 시장 추세 필터 우회");
                return true; // 데이터 없으면 필터 우회
            }
            
            BigDecimal btcPrice = btcIndicators.getCurrentPrice();
            BigDecimal btcMa20 = btcIndicators.getMa20();
            
            boolean isAboveMa20 = btcPrice.compareTo(btcMa20) >= 0;
            
            if (!isAboveMa20) {
                log.info("시장 추세 필터 발동: BTC {}원 < MA20 {}원 - 전체 매수 중단", btcPrice, btcMa20);
            }
            
            return isAboveMa20;
        } catch (Exception e) {
            log.error("시장 추세 필터 체크 오류: {}", e.getMessage());
            return true; // 오류 시 필터 우회
        }
    }

    /**
     * 누적 손실률 체크
     * 초기 자본 대비 누적 손실이 한도에 도달하면 false
     */
    public boolean checkCumulativeLossLimit(String userId, TradingSetting setting) {
        if (setting.getCumulativeLossLimitPct() == null || setting.getCumulativeLossLimitPct() >= 0) {
            return true; // 설정 없거나 0% 이상이면 체크 안함
        }
        
        try {
            // 전체 실현 손익 합계 조회
            BigDecimal totalProfitLoss = transactionRepository.sumTotalProfitLossByUser(userId);
            if (totalProfitLoss == null) {
                totalProfitLoss = BigDecimal.ZERO;
            }
            
            // 초기 자본은 일일 한도 금액으로 추정 (설정에서)
            BigDecimal initialCapital = setting.getDailyLimitAmount();
            if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
                initialCapital = new BigDecimal("1000000"); // 기본값
            }
            
            // 누적 손실률 계산
            BigDecimal cumulativeLossRate = totalProfitLoss
                    .divide(initialCapital, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            BigDecimal limitPct = new BigDecimal(setting.getCumulativeLossLimitPct());
            
            if (cumulativeLossRate.compareTo(limitPct) <= 0) {
                log.warn("누적 손실 한도 도달: userId={}, 누적손실률={}%, 한도={}%", 
                        userId, cumulativeLossRate, limitPct);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("누적 손실률 체크 오류: {}", e.getMessage());
            return true; // 오류 시 체크 우회
        }
    }

    /**
     * 연속 손절 제한 체크
     * 동일 코인 연속 손절 횟수가 한도에 도달하면 24시간 매수 금지
     */
    public boolean checkConsecutiveStopLossLimit(String userId, String market, TradingSetting setting) {
        if (setting.getConsecutiveStopLossLimit() == null || setting.getConsecutiveStopLossLimit() <= 0) {
            return true; // 설정 없으면 체크 안함
        }
        
        String key = userId + ":" + market;
        
        // 매수 금지 시간 체크
        LocalDateTime blockedUntil = buyBlockedUntilMap.get(key);
        if (blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil)) {
            log.info("연속 손절 제한 중: {} - 해제 시각: {}", market, blockedUntil);
            return false;
        } else if (blockedUntil != null) {
            // 금지 시간 만료 - 카운터 리셋
            buyBlockedUntilMap.remove(key);
            consecutiveStopLossMap.remove(key);
        }
        
        return true;
    }

    /**
     * 손절 발생 시 연속 카운터 업데이트
     * TradingBotService에서 손절 매도 후 호출
     */
    public void recordStopLoss(String userId, String market, TradingSetting setting) {
        String key = userId + ":" + market;
        int limit = setting.getConsecutiveStopLossLimit() != null ? setting.getConsecutiveStopLossLimit() : 3;
        
        int currentCount = consecutiveStopLossMap.getOrDefault(key, 0) + 1;
        consecutiveStopLossMap.put(key, currentCount);
        
        log.info("연속 손절 카운터 업데이트: {} = {}회 (한도: {}회)", market, currentCount, limit);
        
        if (currentCount >= limit) {
            // 24시간 매수 금지 설정
            LocalDateTime blockedUntil = LocalDateTime.now().plusHours(24);
            buyBlockedUntilMap.put(key, blockedUntil);
            log.warn("연속 손절 한도 도달: {} - {}까지 매수 금지", market, blockedUntil);
        }
    }

    /**
     * 수익 실현 시 연속 손절 카운터 리셋
     */
    public void recordProfitSell(String userId, String market) {
        String key = userId + ":" + market;
        if (consecutiveStopLossMap.containsKey(key)) {
            consecutiveStopLossMap.remove(key);
            log.debug("연속 손절 카운터 리셋: {}", market);
        }
    }

    /**
     * 긴급 정지 조건 체크 (dailyStopLossPct)
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