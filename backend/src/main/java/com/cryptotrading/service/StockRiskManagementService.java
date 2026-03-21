package com.cryptotrading.service;

import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 주식/ETF 자동매매 리스크 관리 서비스
 * Phase 1 RiskManagementService를 주식 특성에 맞게 재구현
 *
 * 주요 차이점:
 * - 거래 시간 제한 (09:00~15:30 KST)
 * - 레버리지 ETF 보유기간 제한 (최대 20거래일)
 * - 주식 수수료율 반영 (0.015%)
 * - 휴장일 연동 (MarketHolidayService)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRiskManagementService {

    private final StockTransactionRepository stockTransactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final MarketHolidayService marketHolidayService;

    // Redis 키 패턴
    private static final String DAILY_BUY_AMOUNT_KEY  = "stock:daily_buy:%s:%s";
    private static final String DAILY_SELL_AMOUNT_KEY = "stock:daily_sell:%s:%s";
    private static final String CONSEC_LOSS_KEY       = "stock:consec_loss:%s:%s";
    private static final String EMERGENCY_STOP_KEY    = "stock:emergency_stop:%s";
    private static final String BOT_ENABLED_KEY       = "stock:bot_enabled:%s";

    // 정규장 시간 (KST)
    private static final LocalTime MARKET_OPEN  = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    // 레버리지 ETF 보유기간 경고/강제청산 임계값
    public static final int LEVERAGE_ETF_WARN_DAYS  = 15;
    public static final int LEVERAGE_ETF_MAX_DAYS   = 20;

    // =========================================================
    // 1. 거래 시간 체크
    // =========================================================

    /**
     * 현재 정규장 거래 가능 시간인지 확인 (09:00~15:30 KST, 평일, 비휴장일)
     */
    public boolean isMarketOpen() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalTime now   = LocalTime.now(ZoneId.of("Asia/Seoul"));

        // 주말 체크
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            log.debug("주말 - 거래 불가");
            return false;
        }

        // 공휴일/휴장일 체크
        if (marketHolidayService.isHoliday(today)) {
            log.debug("휴장일 - 거래 불가: {}", today);
            return false;
        }

        // 정규장 시간 체크
        boolean inTime = !now.isBefore(MARKET_OPEN) && !now.isAfter(MARKET_CLOSE);
        if (!inTime) {
            log.debug("정규장 시간 외 - 거래 불가: {}", now);
        }
        return inTime;
    }

    /**
     * 내일이 휴장일인지 확인 (전일 알림용)
     */
    public boolean isTomorrowHoliday() {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        return marketHolidayService.isHoliday(tomorrow);
    }

    // =========================================================
    // 2. 일일 거래 한도 체크
    // =========================================================

    /**
     * 오늘 매수 가능 금액 조회
     * 일일 한도 - 오늘 이미 매수한 금액
     */
    public BigDecimal getAvailableDailyBuyAmount(String userId, StockTradingSetting setting) {
        BigDecimal dailyLimit = setting.getDailyLimitAmount();
        BigDecimal usedToday  = getTodayBuyAmount(userId);
        BigDecimal available  = dailyLimit.subtract(usedToday);
        return available.max(BigDecimal.ZERO);
    }

    /**
     * 오늘 매수한 총 금액 (Redis 캐시 우선, 없으면 DB 조회)
     */
    public BigDecimal getTodayBuyAmount(String userId) {
        String key = String.format(DAILY_BUY_AMOUNT_KEY, userId,
                LocalDate.now(ZoneId.of("Asia/Seoul")));
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return new BigDecimal(cached);
        }

        // DB에서 오늘 매수 금액 합산
        BigDecimal dbAmount = stockTransactionRepository
                .sumTodayBuyAmountByDate(userId, LocalDate.now(ZoneId.of("Asia/Seoul")));
        BigDecimal amount = dbAmount != null ? dbAmount : BigDecimal.ZERO;

        // Redis 캐시 (자정까지 유지)
        redisTemplate.opsForValue().set(key, amount.toPlainString(), 24, TimeUnit.HOURS);
        return amount;
    }

    /**
     * 매수 후 일일 누계 금액 업데이트
     */
    public void addTodayBuyAmount(String userId, BigDecimal amount) {
        String key = String.format(DAILY_BUY_AMOUNT_KEY, userId,
                LocalDate.now(ZoneId.of("Asia/Seoul")));
        BigDecimal current = getTodayBuyAmount(userId);
        redisTemplate.opsForValue().set(key, current.add(amount).toPlainString(), 24, TimeUnit.HOURS);
    }

    /**
     * 일일 거래 한도 초과 여부
     */
    public boolean isDailyLimitExceeded(String userId, StockTradingSetting setting) {
        return getAvailableDailyBuyAmount(userId, setting).compareTo(BigDecimal.valueOf(1000)) < 0;
    }

    // =========================================================
    // 3. 종목별 보유 건수 / 비중 체크
    // =========================================================

    /**
     * 특정 종목의 현재 HOLDING 건수
     */
    public int getCurrentHoldingCount(String userId, String stockCode) {
        return (int) stockTransactionRepository
                .countByUserIdAndStockCodeAndStatus(userId, stockCode, TransactionStatus.HOLDING);
    }

    /**
     * 종목당 최대 보유 건수 초과 여부
     */
    public boolean isMaxHoldingsExceeded(String userId, String stockCode, StockTradingSetting setting) {
        int current = getCurrentHoldingCount(userId, stockCode);
        boolean exceeded = current >= setting.getMaxHoldingsPerStock();
        if (exceeded) {
            log.debug("종목 최대 보유 초과 - userId:{} stockCode:{} current:{}/max:{}",
                    userId, stockCode, current, setting.getMaxHoldingsPerStock());
        }
        return exceeded;
    }

    /**
     * 단일 종목 최대 비중(20%) 초과 여부
     * 현재 해당 종목 총 투자금 + 신규 매수 금액이 일일 한도의 20% 초과인지 체크
     */
    public boolean isSingleStockWeightExceeded(String userId, String stockCode,
                                                BigDecimal newBuyAmount,
                                                StockTradingSetting setting) {
        // 현재 해당 종목 총 투자금
        BigDecimal currentInvestment = stockTransactionRepository
                .sumHoldingInvestment(userId, stockCode);
        if (currentInvestment == null) currentInvestment = BigDecimal.ZERO;

        BigDecimal totalAfter = currentInvestment.add(newBuyAmount);
        BigDecimal maxAllowed = setting.getDailyLimitAmount()
                .multiply(BigDecimal.valueOf(0.20));

        boolean exceeded = totalAfter.compareTo(maxAllowed) > 0;
        if (exceeded) {
            log.debug("단일 종목 비중 초과 - userId:{} stockCode:{} totalAfter:{} maxAllowed:{}",
                    userId, stockCode, totalAfter, maxAllowed);
        }
        return exceeded;
    }

    /**
     * 비중 제한을 고려한 실제 매수 가능 금액 계산
     */
    public BigDecimal getWeightLimitedBuyAmount(String userId, String stockCode,
                                                 BigDecimal requestedAmount,
                                                 StockTradingSetting setting) {
        BigDecimal currentInvestment = stockTransactionRepository
                .sumHoldingInvestment(userId, stockCode);
        if (currentInvestment == null) currentInvestment = BigDecimal.ZERO;

        BigDecimal maxAllowed = setting.getDailyLimitAmount()
                .multiply(BigDecimal.valueOf(0.20));
        BigDecimal remaining  = maxAllowed.subtract(currentInvestment).max(BigDecimal.ZERO);

        return requestedAmount.min(remaining);
    }

    // =========================================================
    // 4. 누적 손실률 긴급정지
    // =========================================================

    /**
     * 긴급정지 상태 확인
     */
    public boolean isEmergencyStop(String userId) {
        String key = String.format(EMERGENCY_STOP_KEY, userId);
        return "true".equals(redisTemplate.opsForValue().get(key));
    }

    /**
     * 긴급정지 설정 (수동/자동)
     */
    public void setEmergencyStop(String userId, boolean stop) {
        String key = String.format(EMERGENCY_STOP_KEY, userId);
        redisTemplate.opsForValue().set(key, String.valueOf(stop));
        log.warn("주식봇 긴급정지 상태 변경 - userId:{} stop:{}", userId, stop);
    }

    /**
     * 누적 손실률 체크 → 한도 초과 시 긴급정지
     * 초기 자본 대비 누적 실현 손실
     */
    public boolean checkCumulativeLoss(String userId, StockTradingSetting setting) {
        if (isEmergencyStop(userId)) return true;

        // 실현 손익 합계 (SOLD 상태)
        BigDecimal totalProfitLoss = stockTransactionRepository
                .sumRealizedProfitLoss(userId);
        if (totalProfitLoss == null) totalProfitLoss = BigDecimal.ZERO;

        // 손실인 경우만 체크
        if (totalProfitLoss.compareTo(BigDecimal.ZERO) >= 0) return false;

        BigDecimal lossRate = totalProfitLoss
                .divide(setting.getDailyLimitAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        BigDecimal limit = setting.getCumulativeLossLimitPct() != null
                ? BigDecimal.valueOf(setting.getCumulativeLossLimitPct())
                : BigDecimal.valueOf(-10.0);

        if (lossRate.compareTo(limit) <= 0) {
            log.warn("누적 손실 한도 초과 - userId:{} lossRate:{}% limit:{}%",
                    userId, lossRate, limit);
            setEmergencyStop(userId, true);
            return true;
        }
        return false;
    }

    // =========================================================
    // 5. 연속 손절 제한
    // =========================================================

    /**
     * 연속 손절 횟수 조회
     */
    public int getConsecutiveLossCount(String userId, String stockCode) {
        String key = String.format(CONSEC_LOSS_KEY, userId, stockCode);
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * 연속 손절 횟수 증가
     */
    public void incrementConsecutiveLoss(String userId, String stockCode) {
        String key = String.format(CONSEC_LOSS_KEY, userId, stockCode);
        int current = getConsecutiveLossCount(userId, stockCode);
        redisTemplate.opsForValue().set(key, String.valueOf(current + 1), 7, TimeUnit.DAYS);
        log.info("연속 손절 카운트 증가 - userId:{} stockCode:{} count:{}",
                userId, stockCode, current + 1);
    }

    /**
     * 연속 손절 횟수 초기화 (수익 매도 시)
     */
    public void resetConsecutiveLoss(String userId, String stockCode) {
        String key = String.format(CONSEC_LOSS_KEY, userId, stockCode);
        redisTemplate.delete(key);
    }

    /**
     * 연속 손절 제한 초과 여부
     */
    public boolean isConsecutiveLossLimitExceeded(String userId, String stockCode,
                                                    StockTradingSetting setting) {
        Integer limit = setting.getConsecutiveStopLossLimit();
        if (limit == null || limit <= 0) return false;

        int count = getConsecutiveLossCount(userId, stockCode);
        boolean exceeded = count >= limit;
        if (exceeded) {
            log.debug("연속 손절 제한 초과 - userId:{} stockCode:{} count:{}/limit:{}",
                    userId, stockCode, count, limit);
        }
        return exceeded;
    }

    // =========================================================
    // 6. 레버리지 ETF 보유기간 체크
    // =========================================================

    /**
     * 레버리지 ETF 보유기간 초과 여부 (강제 청산 대상)
     * maxHoldingDays 초과 시 true
     */
    public boolean isHoldingPeriodExceeded(StockTransaction tx, StockTradingSetting setting) {
        if (tx.getHoldingDays() == null) return false;
        int maxDays = setting.getMaxHoldingDays() != null
                ? setting.getMaxHoldingDays()
                : LEVERAGE_ETF_MAX_DAYS;
        return tx.getHoldingDays() >= maxDays;
    }

    /**
     * 레버리지 ETF 보유기간 경고 대상 여부 (15일 이상)
     */
    public boolean isHoldingPeriodWarning(StockTransaction tx, StockTradingSetting setting) {
        if (tx.getHoldingDays() == null) return false;
        int maxDays = setting.getMaxHoldingDays() != null
                ? setting.getMaxHoldingDays()
                : LEVERAGE_ETF_MAX_DAYS;
        int warnDays = Math.max(maxDays - 5, LEVERAGE_ETF_WARN_DAYS);
        return tx.getHoldingDays() >= warnDays;
    }

    /**
     * 보유기간 초과로 강제 청산이 필요한 거래 목록 조회
     */
    public List<StockTransaction> getExpiredHoldings(String userId, StockTradingSetting setting) {
        List<StockTransaction> holdings = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
        return holdings.stream()
                .filter(tx -> isHoldingPeriodExceeded(tx, setting))
                .toList();
    }

    /**
     * 경고 대상 보유 목록 (15일 이상, 강제청산 미포함)
     */
    public List<StockTransaction> getWarnHoldings(String userId, StockTradingSetting setting) {
        List<StockTransaction> holdings = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
        return holdings.stream()
                .filter(tx -> isHoldingPeriodWarning(tx, setting)
                           && !isHoldingPeriodExceeded(tx, setting))
                .toList();
    }

    // =========================================================
    // 7. 봇 활성화 상태 (주식 전용)
    // =========================================================

    public boolean isStockBotEnabled(String userId) {
        String key = String.format(BOT_ENABLED_KEY, userId);
        String val = redisTemplate.opsForValue().get(key);
        return val == null || "true".equals(val); // 기본값 true
    }

    public void setStockBotEnabled(String userId, boolean enabled) {
        String key = String.format(BOT_ENABLED_KEY, userId);
        redisTemplate.opsForValue().set(key, String.valueOf(enabled));
        log.info("주식봇 활성화 상태 변경 - userId:{} enabled:{}", userId, enabled);
    }

    // =========================================================
    // 8. 종합 매수 가능 여부 체크
    // =========================================================

    /**
     * 매수 전 종합 리스크 체크
     * @return null이면 매수 가능, 아니면 불가 사유 메시지
     */
    public String checkBuyRisk(String userId, String stockCode,
                                BigDecimal buyAmount, StockTradingSetting setting) {
        // 봇 활성화
        if (!isStockBotEnabled(userId)) {
            return "주식봇이 비활성화 상태입니다.";
        }
        // 긴급정지
        if (isEmergencyStop(userId)) {
            return "긴급정지 상태입니다. 누적 손실 한도를 초과했습니다.";
        }
        // 거래 시간
        if (!isMarketOpen()) {
            return "정규장 거래 시간이 아닙니다. (09:00~15:30)";
        }
        // 일일 한도
        if (isDailyLimitExceeded(userId, setting)) {
            return "일일 거래 한도에 도달했습니다.";
        }
        // 종목 최대 보유 건수
        if (isMaxHoldingsExceeded(userId, stockCode, setting)) {
            return String.format("종목 최대 보유 건수(%d건)에 도달했습니다.", setting.getMaxHoldingsPerStock());
        }
        // 단일 종목 비중 (실제 매수 금액은 별도 조정 가능이므로 여기선 체크만)
        if (isSingleStockWeightExceeded(userId, stockCode, buyAmount, setting)) {
            return "단일 종목 최대 비중(20%)을 초과합니다.";
        }
        // 연속 손절 제한
        if (isConsecutiveLossLimitExceeded(userId, stockCode, setting)) {
            return String.format("연속 손절 제한(%d회)에 도달했습니다.", setting.getConsecutiveStopLossLimit());
        }
        // 누적 손실 체크
        if (checkCumulativeLoss(userId, setting)) {
            return "누적 손실 한도를 초과하여 거래가 중단되었습니다.";
        }
        return null; // 매수 가능
    }
}