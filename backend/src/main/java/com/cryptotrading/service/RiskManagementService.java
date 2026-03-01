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
import java.time.ZoneId;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {

    private final TransactionRepository transactionRepository;

    private final TechnicalIndicatorService technicalIndicatorService;

    // ⭐⭐⭐ 신규 추가: 업비트 API 서비스 (총자산 조회용) ⭐⭐⭐
    private final UpbitApiService upbitApiService;
    
    // ⭐⭐⭐ 신규 추가: 사용자별 API 키 조회용 ⭐⭐⭐
    private final UserService userService;
    
    private static final int SCALE = 8;

    // 연속 손절 추적용 캐시 (코인별)
    // Key: "userId:coinSymbol", Value: 연속 손절 횟수
    private final Map<String, Integer> consecutiveStopLossMap = new ConcurrentHashMap<>();
    // Key: "userId:coinSymbol", Value: 매수 금지 해제 시각
    private final Map<String, LocalDateTime> buyBlockedUntilMap = new ConcurrentHashMap<>();

    // 일일 매도 금액 추적 (한도 복구용)
    // Key: "userId:날짜(yyyy-MM-dd)", Value: 오늘 총 매도 금액
    private final Map<String, BigDecimal> dailySellAmountMap = new ConcurrentHashMap<>();

    // ⭐⭐⭐ 신규 추가: 일일 총자산 스냅샷 캐시 ⭐⭐⭐
    // Key: "userId:날짜(yyyy-MM-dd)", Value: 당일 00:00 KST 기준 총자산 (KRW + 코인 평가액)
    // 하루 동안 고정되어 급등/급락에도 일일 한도가 변동되지 않음
    private final Map<String, BigDecimal> dailyTotalAssetSnapshot = new ConcurrentHashMap<>();


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
     * ⭐⭐⭐ 수정: 일일 거래 한도 체크 (총자산 스냅샷 기준) ⭐⭐⭐
     */
    public boolean checkDailyLimit(String userId, BigDecimal newAmount, TradingSetting setting) {
        // KST 기준 오늘
        LocalDateTime startOfDay = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).atTime(LocalTime.MAX);
        
        // 오늘 매수한 총 금액 조회
        BigDecimal todayTotal = transactionRepository
                .sumTodayBuyAmount(userId, startOfDay, endOfDay);
        
        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        
        // ⭐⭐⭐ 수정: 총자산 스냅샷 기준으로 일일 한도 계산 ⭐⭐⭐
        BigDecimal effectiveDailyLimit = calculateEffectiveDailyLimit(userId, setting);
        
        BigDecimal afterBuy = todayTotal.add(newAmount);
        boolean withinLimit = afterBuy.compareTo(effectiveDailyLimit) <= 0;
        
        // ⭐⭐⭐ 수정: 로그에서 dailyLimitAmount 대신 총자산 표시 ⭐⭐⭐
        BigDecimal totalAsset = getDailyTotalAssetSnapshot(userId);
        log.debug("일일 한도 체크: 오늘 매수액={}, 신규={}, 실제한도={} (총자산={} x {}%), 통과={}", 
                todayTotal, newAmount, effectiveDailyLimit, 
                totalAsset, setting.getDailyTradeLimitPct(), withinLimit);
        
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
        
        // ⭐⭐⭐ 수정: 최대 허용 금액 = 총자산 스냅샷 × maxPositionPct / 100 ⭐⭐⭐
        BigDecimal totalAsset = getDailyTotalAssetSnapshot(userId);
        BigDecimal maxAllowedAmount = totalAsset
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
            
            // ⭐⭐⭐ 수정: 초기 자본은 총자산 스냅샷 사용 ⭐⭐⭐
            BigDecimal initialCapital = getDailyTotalAssetSnapshot(userId);
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
     * 매도 시 일일 한도 복구 기록
     * @param userId 사용자 ID
     * @param sellAmount 매도 금액
     * @param setting 거래 설정
     */
    public void recordSellForDailyLimitRecovery(String userId, BigDecimal sellAmount, TradingSetting setting) {
        // 복구 옵션이 꺼져있으면 무시
        if (!Boolean.TRUE.equals(setting.getUseDailyLimitRecovery())) {
            return;
        }
        
        String key = userId + ":" + LocalDate.now().toString();
        BigDecimal currentRecovered = dailySellAmountMap.getOrDefault(key, BigDecimal.ZERO);
        
        // 현재 남은 한도 계산 (복구 전)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
       BigDecimal todayBuyTotal = transactionRepository.sumTodayBuyAmount(userId, startOfDay, endOfDay);
        if (todayBuyTotal == null) {
            todayBuyTotal = BigDecimal.ZERO;
        }
        
        // ⭐⭐⭐ 수정: deprecated 메서드 → userId 포함 정상 메서드 호출 ⭐⭐⭐
        // 수정 이유: deprecated 메서드는 DB의 dailyLimitAmount(기본 100만원)를 사용하여
        //           실제 총자산 기준 일일 한도(약 2만원)와 크게 차이가 발생,
        //           복구 금액 계산이 비정상적으로 0원 처리됨
        BigDecimal effectiveDailyLimit = calculateEffectiveDailyLimit(userId, setting);
        BigDecimal currentRemaining = effectiveDailyLimit.subtract(todayBuyTotal).add(currentRecovered);
        
        // 복구 가능한 최대 금액 = 일일 한도 - 현재 남은 한도
        BigDecimal maxRecoverable = effectiveDailyLimit.subtract(currentRemaining);
        if (maxRecoverable.compareTo(BigDecimal.ZERO) < 0) {
            maxRecoverable = BigDecimal.ZERO;
        }
        
        // 실제 복구 금액 = min(매도금액, 복구가능최대금액)
        BigDecimal actualRecovery = sellAmount.min(maxRecoverable);
        
        if (actualRecovery.compareTo(BigDecimal.ZERO) > 0) {
            dailySellAmountMap.put(key, currentRecovered.add(actualRecovery));
            log.info("일일 한도 복구: userId={}, 매도금액={}, 실제복구={}, 총복구액={}", 
                    userId, sellAmount, actualRecovery, currentRecovered.add(actualRecovery));
        }
    }

    /**
     * 일일 매도 복구 캐시 초기화 (자정에 호출)
     */
    public void clearDailySellAmountCache() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        dailySellAmountMap.entrySet().removeIf(entry -> entry.getKey().contains(yesterday));
        log.info("어제 일일 매도 복구 캐시 정리 완료");
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
        
       // ⭐⭐⭐ 수정: 손실 한도 금액 = 총자산 스냅샷 × dailyStopLossPct / 100 (음수) ⭐⭐⭐
        BigDecimal totalAsset = getDailyTotalAssetSnapshot(userId);
        BigDecimal stopLossLimit = totalAsset
                .multiply(new BigDecimal(dailyStopLossPct))
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        
        // 오늘 손익이 손실 한도보다 크면 통과 (손실 한도는 음수)
        boolean withinLimit = todayProfitLoss.compareTo(stopLossLimit) > 0;
        
        // ⭐⭐⭐ 수정: 로그에서 dailyLimitAmount 대신 총자산 표시 ⭐⭐⭐
        log.debug("긴급 정지 체크: 오늘손익={}, 손실한도={} (총자산={} x {}%), 통과={}", 
                todayProfitLoss, stopLossLimit, totalAsset, dailyStopLossPct, withinLimit);
        
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
     * ⭐⭐⭐ 수정: 일일 남은 거래 가능 금액 조회 (총자산 스냅샷 기준) ⭐⭐⭐
     */
    public BigDecimal getRemainingDailyLimit(String userId, TradingSetting setting) {
        // KST 기준 오늘 시작/끝
        LocalDateTime startOfDay = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).atTime(LocalTime.MAX);
        
        BigDecimal todayBuyTotal = transactionRepository
                .sumTodayBuyAmount(userId, startOfDay, endOfDay);
        
        if (todayBuyTotal == null) {
            todayBuyTotal = BigDecimal.ZERO;
        }
        
        // ⭐⭐⭐ 수정: 총자산 스냅샷 기준으로 일일 한도 계산 ⭐⭐⭐
        BigDecimal effectiveDailyLimit = calculateEffectiveDailyLimit(userId, setting);
        
        // ⭐⭐⭐ [수정] 일일 한도 복구: 당일 매수+매도 완료된 거래의 투입금만 복구 ⭐⭐⭐
        // 수정 이유: sumTodaySoldAmount(soldAt 기준)를 사용하면
        //           이전 날짜에 매수한 거래가 오늘 매도될 때 복구액에 포함됨.
        //           그러나 이전 매수는 오늘의 일일 한도를 사용하지 않았으므로
        //           복구 대상이 아님.
        //           → sumTodayBoughtAndSoldAmount(createdAt+soldAt 모두 오늘)를 사용하여
        //           오늘 매수하고 오늘 매도된 거래의 투입금(totalAmount)만 복구.
        //           이렇게 하면 매수액(sumTodayBuyAmount)과 1:1 대응됨.
        BigDecimal recoveredAmount = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(setting.getUseDailyLimitRecovery())) {
            BigDecimal todayBoughtAndSold = transactionRepository
                    .sumTodayBoughtAndSoldAmount(userId, startOfDay, endOfDay);
            if (todayBoughtAndSold == null) {
                todayBoughtAndSold = BigDecimal.ZERO;
            }
            recoveredAmount = todayBoughtAndSold;
            log.debug("일일 한도 복구 적용 (DB 기반): userId={}, 당일매수매도복구={}", userId, recoveredAmount);
        }
        
        // 남은 한도 = 일일 한도 - 오늘 매수액 + 복구액
        BigDecimal remaining = effectiveDailyLimit.subtract(todayBuyTotal).add(recoveredAmount);
        
        // 최대치는 일일 한도까지만 (복구액이 매수액보다 커도 한도 초과 불가)
        if (remaining.compareTo(effectiveDailyLimit) > 0) {
            remaining = effectiveDailyLimit;
        }
        
        return remaining;
    }

     /**
     * ⭐⭐⭐ 신규: 당일 총자산 스냅샷 조회 ⭐⭐⭐
     * 
     * 매일 00:00 KST 기준으로 스냅샷이 저장됨
     * 스냅샷이 없으면 업비트 API를 호출하여 현재 총자산을 조회 후 저장
     * 
     * @param userId 사용자 ID
     * @return 당일 총자산 (KRW + 코인 평가액)
     */
    public BigDecimal getDailyTotalAssetSnapshot(String userId) {
        // KST 기준 오늘 날짜
        String today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
        String key = userId + ":" + today;
        
        // 캐시에 있으면 반환
        BigDecimal cachedSnapshot = dailyTotalAssetSnapshot.get(key);
        if (cachedSnapshot != null) {
            log.debug("총자산 스냅샷 캐시 히트: userId={}, date={}, amount={}", userId, today, cachedSnapshot);
            return cachedSnapshot;
        }
        
        // 캐시에 없으면 업비트 API 호출
        try {
            BigDecimal totalAsset = fetchTotalAssetFromUpbit(userId);
            dailyTotalAssetSnapshot.put(key, totalAsset);
            log.info("총자산 스냅샷 저장: userId={}, date={}, amount={}원", userId, today, totalAsset);
            return totalAsset;
        } catch (Exception e) {
            log.error("총자산 조회 실패: userId={}, error={}", userId, e.getMessage());
            // 조회 실패 시 기본값 100만원 반환 (안전장치)
            BigDecimal defaultAmount = new BigDecimal("1000000");
            log.warn("기본값 사용: {}원", defaultAmount);
            return defaultAmount;
        }
    }

    /**
     * ⭐⭐⭐ 신규: 업비트에서 총자산 조회 ⭐⭐⭐
     * 
     * KRW 잔고 + 모든 코인의 현재 평가액 합계
     */
    // ⭐⭐⭐ [변경] private → public: DailyAssetSnapshotService에서 캐시 우회하여 직접 호출 필요 ⭐⭐⭐
    // 왜: 23:59 스냅샷 저장 시 캐시된 값이 아닌 업비트 실시간 잔고를 조회해야 정확한 평가금액 기록 가능
    public BigDecimal fetchTotalAssetFromUpbit(String userId) {
        // 사용자 API 키 조회 (String[] 반환: [0]=accessKey, [1]=secretKey)
        String[] apiKeys = userService.getDecryptedApiKeys(userId);
        if (apiKeys == null || apiKeys[0] == null) {
            throw new RuntimeException("API 키가 설정되지 않았습니다");
        }
        
        // 업비트 계좌 조회
        var accounts = upbitApiService.getAccounts(apiKeys[0], apiKeys[1]);
        if (accounts == null || accounts.isEmpty()) {
            throw new RuntimeException("계좌 정보를 조회할 수 없습니다");
        }
        
        BigDecimal totalAsset = BigDecimal.ZERO;
        
        for (var account : accounts) {
            String currency = account.getCurrency();
            BigDecimal balance = account.getBalance();
            
            if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            if ("KRW".equals(currency)) {
                // 원화 잔고
                totalAsset = totalAsset.add(balance);
            } else {
                // 코인 평가액 = 보유수량 × 현재가
                try {
                    String market = "KRW-" + currency;
                    var ticker = upbitApiService.getTicker(java.util.List.of(market));
                    if (ticker != null && !ticker.isEmpty()) {
                        BigDecimal currentPrice = ticker.get(0).getTradePrice();
                        BigDecimal coinValue = balance.multiply(currentPrice);
                        totalAsset = totalAsset.add(coinValue);
                    }
                } catch (Exception e) {
                    log.warn("코인 시세 조회 실패: {} - {}", currency, e.getMessage());
                    // 평가액을 0으로 처리 (보수적 계산)
                }
            }
        }
        
        log.info("업비트 총자산 조회 완료: userId={}, totalAsset={}원", userId, totalAsset);
        return totalAsset;
    }

    /**
     * ⭐⭐⭐ 신규: 총자산 스냅샷 수동 갱신 ⭐⭐⭐
     * 
     * 관리자 또는 스케줄러에서 호출하여 강제 갱신
     */
    public void refreshTotalAssetSnapshot(String userId) {
        String today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
        String key = userId + ":" + today;
        
        try {
            BigDecimal totalAsset = fetchTotalAssetFromUpbit(userId);
            dailyTotalAssetSnapshot.put(key, totalAsset);
            log.info("총자산 스냅샷 강제 갱신: userId={}, amount={}원", userId, totalAsset);
        } catch (Exception e) {
            log.error("총자산 스냅샷 갱신 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * ⭐⭐⭐ 신규: 어제 총자산 스냅샷 캐시 정리 (자정에 호출) ⭐⭐⭐
     */
    public void clearYesterdayTotalAssetSnapshot() {
        String yesterday = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1).toString();
        dailyTotalAssetSnapshot.entrySet().removeIf(entry -> entry.getKey().contains(yesterday));
        log.info("어제 총자산 스냅샷 캐시 정리 완료: {}", yesterday);
    }

/**
     * ⭐⭐⭐ 수정: 실제 일일 한도 계산 (총자산 스냅샷 × dailyTradeLimitPct) ⭐⭐⭐
     * 
     * 변경 전: dailyLimitAmount × dailyTradeLimitPct%
     * 변경 후: 총자산 스냅샷 × dailyTradeLimitPct%
     * 
     * @param userId 사용자 ID (총자산 스냅샷 조회용)
     * @param setting 거래 설정
     * @return 실제 일일 거래 한도
     */
    public BigDecimal calculateEffectiveDailyLimit(String userId, TradingSetting setting) {
        // 당일 총자산 스냅샷 조회
        BigDecimal totalAsset = getDailyTotalAssetSnapshot(userId);
        
        Integer dailyTradeLimitPct = setting.getDailyTradeLimitPct();
        
        // 100%이면 총자산 전체 사용
        if (dailyTradeLimitPct == null || dailyTradeLimitPct >= 100) {
            log.debug("일일 한도 계산: 총자산={}원, 한도비율=100%, 실제한도={}원", totalAsset, totalAsset);
            return totalAsset;
        }
        
        BigDecimal effectiveLimit = totalAsset
                .multiply(new BigDecimal(dailyTradeLimitPct))
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        
        log.debug("일일 한도 계산: 총자산={}원, 한도비율={}%, 실제한도={}원", 
                totalAsset, dailyTradeLimitPct, effectiveLimit);
        
        return effectiveLimit;
    }
    
    /**
     * ⚠️ DEPRECATED: 기존 메서드 (하위 호환성 유지)
     * 새로운 코드에서는 calculateEffectiveDailyLimit(userId, setting) 사용
     */
    @Deprecated
    public BigDecimal calculateEffectiveDailyLimit(TradingSetting setting) {
        log.warn("DEPRECATED 메서드 호출: calculateEffectiveDailyLimit(setting) - userId 파라미터 필요");
        Integer dailyTradeLimitPct = setting.getDailyTradeLimitPct();
        
        // 기존 로직 유지 (dailyLimitAmount 사용)
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
        
        // ⭐⭐⭐ 수정: 최대 허용 금액 = 총자산 스냅샷 × maxPositionPct / 100 ⭐⭐⭐
        BigDecimal totalAsset = getDailyTotalAssetSnapshot(userId);
        BigDecimal maxAllowedAmount = totalAsset
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