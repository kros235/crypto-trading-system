package com.cryptotrading.service;

import com.cryptotrading.dto.bot.TradingSignalDTO;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalStrength;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalType;
import com.cryptotrading.dto.kis.KisOrderDTO;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.entity.TransactionType;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.StockTradingSettingRepository;
import com.cryptotrading.repository.StockTransactionRepository;
import com.cryptotrading.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 주식/ETF 자동매매 봇 서비스 (Phase 2 핵심)
 * Phase 1 TradingBotService 구조를 재사용하며 KIS API / 주식 특성에 맞게 조정
 *
 * [Phase 1 TradingBotService와의 주요 차이점]
 * 1. API       : UpbitApiService → KisApiService
 * 2. 주문 방식 : 금액(원화) 기반 → 수량(정수 주) 기반
 * 3. 거래 시간 : 24시간 → 09:00~15:30 KST 정규장만 (isMarketOpen 체크)
 * 4. 수수료율  : 0.05% → 0.015% (국내 주식 기준)
 * 5. 보유기간  : 매 사이클 holdingDays 갱신 (레버리지 ETF decay 방지)
 * 6. Entity    : Transaction → StockTransaction
 * 7. Setting   : TradingSetting → StockTradingSetting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockTradingBotService {

    private final StockSignalDetectorService signalDetectorService;
    private final StockRiskManagementService riskManagementService;
    private final KisApiService kisApiService;
    private final UserRepository userRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final StockTradingSettingRepository stockTradingSettingRepository;
    private final NotificationService notificationService;
    private final DiscordBotService discordBotService;
    private final EmailService emailService;

    // ★ 주식 수수료율: 0.015% (Phase 1 코인: 0.05%)
    private static final BigDecimal FEE_RATE = new BigDecimal("0.00015");
    private static final int SCALE = 8;

    // =========================================================
    // 1. 진입점: 특정 사용자 자동매매 실행
    // =========================================================

    /**
     * 특정 사용자의 주식 자동매매 1회 실행
     * Phase 1 executeForUser()와 동일한 흐름 유지
     */
    public BotExecutionResult executeForUser(String userId) {
        log.info("========== [주식봇] 자동매매 실행 시작: {} ==========", userId);
        BotExecutionResult result = new BotExecutionResult(userId);

        try {
            // 1. 사용자 조회
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                result.setStatus("SKIP");
                result.setMessage("비활성화된 사용자");
                return result;
            }

            // 2. 주식 거래 설정 조회
            Optional<StockTradingSetting> settingOpt = stockTradingSettingRepository.findByUserId(userId);
            if (settingOpt.isEmpty()) {
                log.warn("[주식봇] 거래 설정 없음: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("주식 거래 설정 없음");
                return result;
            }
            StockTradingSetting setting = settingOpt.get();

            // 3. 봇 활성화 여부 체크 (Redis)
            if (!riskManagementService.isStockBotEnabled(userId)) {
                log.info("[주식봇] 봇 비활성화 상태: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("주식 봇 비활성화");
                return result;
            }

            // ★ Phase 1과의 차이: 주식은 거래 시간 제한 (09:00~15:30 KST)
            if (!riskManagementService.isMarketOpen()) {
                log.debug("[주식봇] 정규장 시간 외: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("정규장 시간 외");
                return result;
            }

            // 4. KIS API 키 유효성 확인 (토큰 발급 여부)
            if (!isKisApiAvailable(userId)) {
                log.warn("[주식봇] KIS API 키 없거나 토큰 오류: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("KIS API 키 미등록 또는 토큰 오류");
                return result;
            }

            // 5. 긴급 정지 체크
            if (riskManagementService.isEmergencyStop(userId)) {
                log.warn("⚠️ [주식봇] 긴급 정지 상태: {}", userId);
                result.setStatus("EMERGENCY_STOP");
                result.setMessage("긴급 정지 - 일일 손실 한도 도달");
                return result;
            }

            // 6. 보유 중인 거래 조회 + 보유기간 갱신 (★ Phase 1에 없는 주식 전용 처리)
            List<StockTransaction> holdings = stockTransactionRepository
                    .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
            updateHoldingDays(holdings);

            // 7. 매도 신호 처리 (갱신 후 재조회하여 최신 holdingDays 반영)
            List<StockTransaction> holdingsForSell = stockTransactionRepository
                    .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
            for (StockTransaction holding : holdingsForSell) {
                try {
                    processSellSignal(holding, setting, userId, result);
                } catch (Exception e) {
                    log.error("[주식봇] 매도 처리 실패: transactionId={}, error={}",
                            holding.getTransactionId(), e.getMessage());
                    result.addError(holding.getStockCode() + " 매도 실패: " + e.getMessage());
                }
            }

            // 8. 라운드로빈 매수 처리
            try {
                processRoundRobinBuy(userId, setting, result);
            } catch (Exception e) {
                log.error("[주식봇] 라운드로빈 매수 처리 실패: userId={}, error={}", userId, e.getMessage());
                result.addError("라운드로빈 매수 실패: " + e.getMessage());
            }

            // 9. 매도 처리 후 남은 보유 거래의 최고가 업데이트 (트레일링 스톱용)
            // ★ 매도된 건 제외 후 재조회 (Phase 1과 동일한 패턴)
            List<StockTransaction> remainingHoldings = stockTransactionRepository
                    .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
            updateHighestPrices(remainingHoldings, userId);

            result.setStatus("SUCCESS");
            log.info("========== [주식봇] 자동매매 완료: {} - 매수 {}건, 매도 {}건 ==========",
                    userId, result.getBuyCount(), result.getSellCount());

        } catch (Exception e) {
            log.error("[주식봇] 자동매매 실행 오류: userId={}, error={}", userId, e.getMessage(), e);
            result.setStatus("ERROR");
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 모든 활성 사용자에 대해 주식 자동매매 실행
     * Phase 1 executeForAllUsers()와 동일한 구조
     */
    public List<BotExecutionResult> executeForAllUsers() {
        log.info("========== [주식봇] 전체 사용자 자동매매 시작 ==========");
        List<User> activeUsers = userRepository.findByIsActive(true);
        List<BotExecutionResult> results = new ArrayList<>();

        for (User user : activeUsers) {
            BotExecutionResult result = executeForUser(user.getUserId());
            results.add(result);
        }

        log.info("========== [주식봇] 전체 사용자 자동매매 완료: {}명 처리 ==========", results.size());
        return results;
    }

    // =========================================================
    // 2. 매수 처리 (라운드로빈)
    // =========================================================

    /**
     * 라운드로빈 방식 매수 처리
     * Phase 1 processRoundRobinBuy()를 주식 특성에 맞게 재구현
     *
     * [Phase 1과의 차이점]
     * - 매수 금액이 아닌 수량(정수 주) 기반 주문
     * - 현재가로 매수 가능 수량 계산 후 KIS API 호출
     * - MIN_AMOUNT 기준: 5,000원(코인) → 10,000원(주식 1주 최소 가격 기준)
     */
    private void processRoundRobinBuy(String userId, StockTradingSetting setting,
            BotExecutionResult result) {
        log.info("🔄 [주식봇] 라운드로빈 매수 처리 시작: {}", userId);

        // ===== 1단계: 공통 리스크 사전 체크 =====
        // checkCumulativeLoss: true = 한도 초과 → 매수 중단
        if (riskManagementService.checkCumulativeLoss(userId, setting)) {
            log.warn("⚠️ [주식봇] 누적 손실 한도 도달 - 전체 매수 중단");
            result.addSkipped("누적 손실 한도 도달 - 전체 매수 중단");
            return;
        }
        if (riskManagementService.isDailyLimitExceeded(userId, setting)) {
            log.info("[주식봇] 일일 거래 한도 초과 - 매수 중단");
            result.addSkipped("일일 거래 한도 초과");
            return;
        }

        // ===== 2단계: 매수 후보 수집 =====
        List<String> targetStocks = parseStockCodes(setting.getStockCodes());
        if (targetStocks.isEmpty()) {
            log.info("[주식봇] 등록된 종목 없음 - 매수 처리 종료");
            return;
        }

        boolean useRoundRobin = Boolean.TRUE.equals(setting.getUseRoundRobin());
        List<BuyCandidate> candidates = new ArrayList<>();

        for (String stockCode : targetStocks) {
            try {
                // 연속 손절 제한 체크
                if (riskManagementService.isConsecutiveLossLimitExceeded(userId, stockCode, setting)) {
                    log.info("⚠️ [주식봇] {} 연속 손절 제한 - 매수 금지", stockCode);
                    result.addSkipped(stockCode + ": 연속 손절 제한");
                    continue;
                }

                // 보유 건수 초과 체크
                if (riskManagementService.isMaxHoldingsExceeded(userId, stockCode, setting)) {
                    log.info("[주식봇] 보유 건수 초과: {} - 최대 {}건",
                            stockCode, setting.getMaxHoldingsPerStock());
                    result.addSkipped(stockCode + ": 보유 건수 초과");
                    continue;
                }

                // 매수 신호 감지
                TradingSignalDTO signal = signalDetectorService.detectBuySignal(stockCode, setting, userId);
                if (signal.getSignalType() != SignalType.BUY) {
                    log.debug("[주식봇] 매수 신호 없음: {} - {}", stockCode, signal.getReason());
                    continue;
                }

                // 분할 매수 조건 체크 (이미 보유 중인 경우)
                List<StockTransaction> activeHoldings = stockTransactionRepository
                        .findByUserIdAndStatus(userId, TransactionStatus.HOLDING)
                        .stream()
                        .filter(t -> stockCode.equals(t.getStockCode()))
                        .toList();

                if (!activeHoldings.isEmpty()) {
                    BigDecimal recentBuyPrice = activeHoldings.stream()
                            .max(java.util.Comparator.comparing(StockTransaction::getCreatedAt))
                            .map(StockTransaction::getPrice)
                            .orElse(null);

                    if (recentBuyPrice != null) {
                        BigDecimal dropFromLastBuy = recentBuyPrice.subtract(signal.getCurrentPrice())
                                .divide(recentBuyPrice, SCALE, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                        BigDecimal requiredDrop = setting.getAdditionalDropPct() != null
                                ? setting.getAdditionalDropPct()
                                : new BigDecimal("1.00");

                        if (dropFromLastBuy.compareTo(requiredDrop) < 0) {
                            log.info("[주식봇] 분할 매수 조건 미달: {} - 이전 매수가 대비 하락률 {}% (기준 {}%)",
                                    stockCode, dropFromLastBuy.setScale(2, RoundingMode.HALF_UP), requiredDrop);
                            result.addSkipped(stockCode + ": 분할 매수 하락률 미달");
                            continue;
                        }
                    }
                }

                // 비중 제한 고려한 최대 매수 가능 금액
                // ★ 수정: getWeightLimitedBuyAmount는 requestedAmount(요청금액) 파라미터 필요
                // 후보 수집 시점에는 perStockAmount가 미결정이므로 dailyLimitAmount를 기준값으로 전달
                // → 실제 배정 금액은 4단계 매수 실행 시 min(allocated, maxBuyable)로 재조정됨
                BigDecimal maxBuyable = riskManagementService.getWeightLimitedBuyAmount(
                        userId, stockCode, setting.getDailyLimitAmount(), setting);  // ← 4개 파라미터 (정상)
                if (maxBuyable.compareTo(new BigDecimal("10000")) < 0) {
                    log.info("[주식봇] 비중 제한으로 매수 불가: {} - 잔여 {}원", stockCode, maxBuyable);
                    result.addSkipped(stockCode + ": 비중 제한");
                    continue;
                }

                log.info("✅ [주식봇] 매수 후보 추가: {} (강도: {}, 이격도: {}%)",
                        stockCode, signal.getStrength(), signal.getDropRate());
                candidates.add(new BuyCandidate(stockCode, signal, maxBuyable));

            } catch (Exception e) {
                log.error("[주식봇] 매수 후보 수집 실패: stockCode={}, error={}", stockCode, e.getMessage());
            }
        }

        if (candidates.isEmpty()) {
            log.info("[주식봇] 매수 후보 없음 - 라운드로빈 종료");
            return;
        }
        log.info("📋 [주식봇] 매수 후보 {}개 수집 완료", candidates.size());

        // ===== 3단계: 1종목당 매수 금액 계산 =====
        BigDecimal availableAmount = riskManagementService.getAvailableDailyBuyAmount(userId, setting);
        if (availableAmount.compareTo(new BigDecimal("10000")) < 0) {
            log.info("[주식봇] 매수 가능 금액 부족: {}원", availableAmount);
            return;
        }

        BigDecimal fixedBuyAmount = setting.getFixedBuyAmount() != null
                ? setting.getFixedBuyAmount()
                : new BigDecimal("100000");

        BigDecimal perStockAmount;
        if (useRoundRobin) {
            // 라운드로빈: 남은 한도를 종목 수로 균등 분배
            perStockAmount = availableAmount.divide(
                    new BigDecimal(candidates.size()), SCALE, RoundingMode.DOWN);
            log.info("📊 [주식봇/라운드로빈] 균등 분배 금액: {}원 ({}개 종목)",
                    perStockAmount.setScale(0, RoundingMode.DOWN), candidates.size());
        } else {
            perStockAmount = fixedBuyAmount;
            log.info("📊 [주식봇/고정금액] 1회 매수 금액: {}원", perStockAmount);
        }

        // ===== 최소 금액 미달 시 우선순위 재선정 (Phase 1과 동일 패턴) =====
        final BigDecimal MIN_AMOUNT = new BigDecimal("10000");
        if (perStockAmount.compareTo(MIN_AMOUNT) < 0) {
            if (!useRoundRobin) {
                log.warn("⚠️ [주식봇/고정금액] 설정 금액 {}원 < 최소 {}원 - 매수 불가",
                        perStockAmount, MIN_AMOUNT);
                return;
            }
            int maxStocks = availableAmount.divide(MIN_AMOUNT, 0, RoundingMode.DOWN).intValue();
            if (maxStocks == 0) {
                log.info("[주식봇] 최소 금액 미달로 매수 불가 - 종료");
                return;
            }
            candidates.sort((a, b) -> b.compareTo(a)); // 우선순위 내림차순
            candidates = candidates.subList(0, Math.min(maxStocks, candidates.size()));
            perStockAmount = availableAmount.divide(
                    new BigDecimal(candidates.size()), SCALE, RoundingMode.DOWN);
            log.info("📊 [주식봇/라운드로빈] 재분배 금액: {}원 ({}개 종목)",
                    perStockAmount.setScale(0, RoundingMode.DOWN), candidates.size());
        }

        // ===== 4단계: 매수 실행 =====
        BigDecimal usedAmount = BigDecimal.ZERO;
        BigDecimal carryOver = BigDecimal.ZERO; // 비중 제한으로 남은 금액 이월 (라운드로빈만)

        for (BuyCandidate candidate : candidates) {
            // 실제 배정 금액 계산
            BigDecimal allocated = useRoundRobin
                    ? perStockAmount.add(carryOver)
                    : fixedBuyAmount;

            BigDecimal actualAmount = allocated.min(candidate.getMaxBuyableAmount());
            BigDecimal currentRemaining = availableAmount.subtract(usedAmount);
            if (actualAmount.compareTo(currentRemaining) > 0) {
                actualAmount = currentRemaining;
            }

            if (actualAmount.compareTo(MIN_AMOUNT) < 0) {
                log.info("[주식봇] {} 매수 스킵: 실제 금액 {}원 < 최소 {}원",
                        candidate.getStockCode(), actualAmount.setScale(0, RoundingMode.DOWN), MIN_AMOUNT);
                if (useRoundRobin) carryOver = carryOver.add(perStockAmount);
                continue;
            }

            // 이월 계산 (라운드로빈만)
            if (useRoundRobin) {
                carryOver = actualAmount.compareTo(allocated) < 0
                        ? allocated.subtract(actualAmount)
                        : BigDecimal.ZERO;
            }

            try {
                executeBuyOrder(userId, candidate.getStockCode(), actualAmount,
                        candidate.getSignal(), setting, result);
                usedAmount = usedAmount.add(actualAmount);
            } catch (Exception e) {
                log.error("[주식봇] {} 매수 실패: {}", candidate.getStockCode(), e.getMessage());
                result.addError(candidate.getStockCode() + " 매수 실패: " + e.getMessage());
                if (useRoundRobin) carryOver = carryOver.add(actualAmount);
            }
        }

        log.info("[주식봇] 매수 완료: 총 {}원 사용 (한도 {}원)",
                usedAmount.setScale(0, RoundingMode.DOWN), availableAmount.setScale(0, RoundingMode.DOWN));
    }

    // =========================================================
    // 3. 실제 매수 주문 실행
    // =========================================================

    /**
     * 실제 KIS 매수 주문 실행
     *
     * ★ Phase 1과의 핵심 차이: 금액(원화) 기반 → 수량(정수 주) 기반
     *   - 코인: upbitApiService.orderBuy(key, secret, market, 매수금액)
     *   - 주식: kisApiService.placeBuyOrder(userId, OrderRequest{stockCode, quantity, price})
     *   - 수량 = 매수금액 ÷ 현재가 (소수점 버림, 정수)
     */
    private void executeBuyOrder(String userId, String stockCode, BigDecimal buyAmount,
            TradingSignalDTO signal, StockTradingSetting setting,
            BotExecutionResult result) {
        log.info("[주식봇] 매수 주문 실행: {} - {}원", stockCode, buyAmount.setScale(0, RoundingMode.DOWN));

        BigDecimal currentPrice = signal.getCurrentPrice();

        // ★ 주식은 정수 수량 단위: 소수점 버림
        int quantity = buyAmount.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
        if (quantity <= 0) {
            log.warn("[주식봇] 매수 수량 0주: {} - 현재가 {}원, 매수금액 {}원",
                    stockCode, formatPrice(currentPrice), buyAmount.setScale(0, RoundingMode.DOWN));
            result.addSkipped(stockCode + ": 매수 수량 0주 (현재가 초과)");
            return;
        }

        // 실제 투자금액 재계산 (수량 × 현재가)
        BigDecimal actualTotalAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = actualTotalAmount.multiply(FEE_RATE);

        // KIS API 매수 주문 (시장가: orderType="01", price=0)
        KisOrderDTO.OrderRequest orderRequest = KisOrderDTO.OrderRequest.builder()
                .stockCode(stockCode)
                .quantity(quantity)
                .price(0)
                .orderType("01") // 시장가
                .side("BUY")
                .build();

        KisOrderDTO.OrderResponse response = kisApiService.placeBuyOrder(userId, orderRequest);

        if (response == null || !response.isSuccess()) {
            String msg = response != null ? response.getMessage() : "응답 없음";
            log.error("[주식봇] KIS 매수 주문 실패: {} - {}", stockCode, msg);
            throw new RuntimeException("KIS 매수 주문 실패: " + msg);
        }

        String orderNo = (response.getOutput() != null && response.getOutput().getOrderNumber() != null)
                ? response.getOutput().getOrderNumber() : "N/A";
        log.info("[주식봇] KIS 매수 주문 접수: {} - {}주, 주문번호: {}", stockCode, quantity, orderNo);

        // 거래 내역 저장
        StockTransaction transaction = StockTransaction.builder()
                .userId(userId)
                .stockCode(stockCode)
                .type(TransactionType.BUY)
                .quantity(quantity)
                .price(currentPrice)
                .fee(fee)
                .totalAmount(actualTotalAmount)
                .targetSellPrice(signal.getTargetPrice())
                .stopLossPrice(Boolean.TRUE.equals(setting.getUseStopLoss())
                        ? signal.getStopLossPrice() : null)
                .highestPrice(currentPrice)
                .holdingDays(0)
                .status(TransactionStatus.HOLDING)
                .note("[매수] " + signal.getReason() + " | 주문번호: " + orderNo)
                .build();
        stockTransactionRepository.save(transaction);

        // 일일 매수 금액 누계 업데이트 (Redis)
        riskManagementService.addTodayBuyAmount(userId, actualTotalAmount);

        // 알림 발송
        notificationService.notifyBuyExecuted(
                userId, stockCode, currentPrice,
                new BigDecimal(quantity), actualTotalAmount,
                signal.getReason());

        User userEntity = userRepository.findByUserId(userId).orElse(null);
        if (userEntity != null && userEntity.getDiscordUserId() != null) {
            discordBotService.sendBuyNotification(
                    userEntity.getDiscordUserId(),
                    stockCode,
                    quantity + "주",
                    formatPrice(currentPrice),
                    String.format("%,.0f", actualTotalAmount),
                    signal.getReason());
        }
        if (userEntity != null && userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
            emailService.sendTradeNotification(
                    userEntity.getEmail(), "BUY", stockCode,
                    new BigDecimal(quantity), currentPrice, actualTotalAmount,
                    signal.getReason());
        }

        result.addBuy(stockCode, actualTotalAmount);
        log.info("[주식봇] 매수 완료: {} - {}주 × {}원 = {}원 (주문번호: {})",
                stockCode, quantity, formatPrice(currentPrice),
                String.format("%,.0f", actualTotalAmount), orderNo);
    }

    // =========================================================
    // 4. 매도 신호 처리
    // =========================================================

    /**
     * 매도 신호 처리
     * Phase 1 processSellSignal()과 동일한 구조, KIS API로 교체
     *
     * [Phase 1과의 차이점]
     * - useStopLoss 플래그: false 이면 손절 신호 무시
     * - 수량: BigDecimal → Integer (주식 정수 단위)
     */
    private void processSellSignal(StockTransaction holding, StockTradingSetting setting,
            String userId, BotExecutionResult result) {
        TradingSignalDTO signal = signalDetectorService.detectSellSignal(holding, setting, userId);

        if (signal.getSignalType() == SignalType.HOLD) {
            log.debug("[주식봇] 매도 신호 없음: {} - {}", holding.getStockCode(), signal.getReason());
            return;
        }

        // 손절 비활성화 시 STOP_LOSS 신호 무시
        if (!Boolean.TRUE.equals(setting.getUseStopLoss())
                && signal.getSignalType() == SignalType.STOP_LOSS) {
            log.info("[주식봇] 손절 비활성화 - 매도 신호 무시: {}", holding.getStockCode());
            return;
        }

        log.info("[주식봇] 매도 신호 감지: {} - {} (타입: {})",
                holding.getStockCode(), signal.getReason(), signal.getSignalType());

        BigDecimal currentPrice = signal.getCurrentPrice();
        int quantity = holding.getQuantity();

        // KIS API 매도 주문 (시장가)
        KisOrderDTO.OrderRequest orderRequest = KisOrderDTO.OrderRequest.builder()
                .stockCode(holding.getStockCode())
                .quantity(quantity)
                .price(0)
                .orderType("01") // 시장가
                .side("SELL")
                .build();

        KisOrderDTO.OrderResponse response = kisApiService.placeSellOrder(userId, orderRequest);

        if (response == null || !response.isSuccess()) {
            String msg = response != null ? response.getMessage() : "응답 없음";
            log.error("[주식봇] KIS 매도 주문 실패: {} - {}", holding.getStockCode(), msg);
            throw new RuntimeException("KIS 매도 주문 실패: " + msg);
        }

        String orderNo = (response.getOutput() != null && response.getOutput().getOrderNumber() != null)
                ? response.getOutput().getOrderNumber() : "N/A";

        // 손익 계산 (수수료 매수/매도 각각 반영)
        BigDecimal sellAmount   = currentPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal buyFee       = holding.getTotalAmount().multiply(FEE_RATE);
        BigDecimal sellFee      = sellAmount.multiply(FEE_RATE);
        BigDecimal profitLoss   = sellAmount.subtract(sellFee)
                .subtract(holding.getTotalAmount()).subtract(buyFee);
        BigDecimal profitRate   = holding.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.divide(holding.getTotalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // 거래 상태 업데이트
        holding.setStatus(TransactionStatus.SOLD);
        holding.setSoldAt(LocalDateTime.now());
        holding.setSoldPrice(currentPrice);
        holding.setProfitLoss(profitLoss);
        holding.setProfitLossPct(profitRate);
        String existingNote = holding.getNote() != null ? holding.getNote() : "";
        holding.setNote(existingNote + " → [매도] " + signal.getReason() + " | 주문번호: " + orderNo);
        stockTransactionRepository.save(holding);

        // 연속 손절/수익 카운터 업데이트
        if (signal.getSignalType() == SignalType.STOP_LOSS ||
                (signal.getSignalType() == SignalType.TRAILING_STOP
                        && profitLoss.compareTo(BigDecimal.ZERO) < 0)) {
            riskManagementService.incrementConsecutiveLoss(userId, holding.getStockCode());
        } else if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
            riskManagementService.resetConsecutiveLoss(userId, holding.getStockCode());
        }

        // 알림 발송
        if (signal.getSignalType() == SignalType.STOP_LOSS) {
            notificationService.notifyStopLoss(
                    userId, holding.getStockCode(), currentPrice, profitLoss, profitRate);
        } else {
            notificationService.notifySellExecuted(
                    userId, holding.getStockCode(), currentPrice,
                    new BigDecimal(quantity), sellAmount,
                    profitLoss, profitRate, signal.getReason());
        }

        User userEntity = userRepository.findByUserId(userId).orElse(null);
        if (userEntity != null && userEntity.getDiscordUserId() != null) {
            String sign = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            discordBotService.sendSellNotification(
                    userEntity.getDiscordUserId(),
                    holding.getStockCode(),
                    quantity + "주",
                    formatPrice(currentPrice),
                    sign + String.format("%,.0f", profitLoss),
                    sign + profitRate.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    signal.getReason());
        }
        if (userEntity != null && userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
            emailService.sendTradeNotification(
                    userEntity.getEmail(), "SELL", holding.getStockCode(),
                    new BigDecimal(quantity), currentPrice, sellAmount,
                    signal.getReason());
        }

        result.addSell(holding.getStockCode(), sellAmount, profitLoss);
        log.info("[주식봇] 매도 완료: {} - {}주 × {}원 = {}원, 손익: {}원 (주문번호: {})",
                holding.getStockCode(), quantity, formatPrice(currentPrice),
                String.format("%,.0f", sellAmount),
                String.format("%,.0f", profitLoss), orderNo);
    }

    // =========================================================
    // 5. 유틸리티 메서드
    // =========================================================

    /**
     * KIS API 토큰 유효성 확인
     * 간단히 현재가 1회 조회 시도 (삼성전자 005930 기준)
     */
    private boolean isKisApiAvailable(String userId) {
        try {
            var quote = kisApiService.getCurrentPrice(userId, "005930");
            return quote != null;
        } catch (Exception e) {
            log.warn("[주식봇] KIS API 토큰 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 보유기간(holdingDays) 갱신
     * ★ Phase 2 전용: 레버리지 ETF decay 방지를 위한 보유기간 추적
     * createdAt 기준으로 실제 경과 일수를 계산하여 갱신
     */
    private void updateHoldingDays(List<StockTransaction> holdings) {
        for (StockTransaction holding : holdings) {
            try {
                if (holding.getCreatedAt() != null) {
                    int days = (int) java.time.Duration.between(
                            holding.getCreatedAt(), LocalDateTime.now()).toDays();
                    if (!Integer.valueOf(days).equals(holding.getHoldingDays())) {
                        holding.setHoldingDays(days);
                        stockTransactionRepository.save(holding);
                        log.debug("[주식봇] 보유기간 갱신: {} - {}일", holding.getStockCode(), days);
                    }
                }
            } catch (Exception e) {
                log.warn("[주식봇] 보유기간 갱신 실패: {} - {}", holding.getStockCode(), e.getMessage());
            }
        }
    }

    /**
     * 보유 거래의 최고가 업데이트 (트레일링 스톱용)
     * Phase 1과 동일한 로직, KIS 현재가 조회로 교체
     */
    private void updateHighestPrices(List<StockTransaction> holdings, String userId) {
        for (StockTransaction holding : holdings) {
            try {
                var quote = kisApiService.getCurrentPrice(userId, holding.getStockCode());
                if (quote != null && quote.getCurrentPriceDecimal() != null
                        && quote.getCurrentPriceDecimal().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentPrice = quote.getCurrentPriceDecimal();
                    if (holding.getHighestPrice() == null
                            || currentPrice.compareTo(holding.getHighestPrice()) > 0) {
                        holding.setHighestPrice(currentPrice);
                        stockTransactionRepository.save(holding);
                        log.debug("[주식봇] 최고가 업데이트: {} - {}원",
                                holding.getStockCode(), formatPrice(currentPrice));
                    }
                }
            } catch (Exception e) {
                log.warn("[주식봇] 최고가 업데이트 실패: {} - {}", holding.getStockCode(), e.getMessage());
            }
        }
    }

    /**
     * JSON 형식의 stockCodes 파싱
     * ex) ["409820","409810"] → List.of("409820", "409810")
     */
    private List<String> parseStockCodes(String stockCodesJson) {
        if (stockCodesJson == null || stockCodesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            // 정규식 없이 간단 파싱: 따옴표·대괄호·공백 제거 후 콤마 분리
            String cleaned = stockCodesJson.replaceAll("[\\[\\]\"\\s]", "");
            if (cleaned.isEmpty()) return new ArrayList<>();
            List<String> result = new ArrayList<>();
            for (String part : cleaned.split(",")) {
                if (!part.isBlank()) result.add(part.trim());
            }
            return result;
        } catch (Exception e) {
            log.warn("[주식봇] 종목 코드 파싱 실패: {} - {}", stockCodesJson, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 가격 포맷 (원화, 정수)
     * Phase 1 formatPrice()와 동일한 구조 (주식은 원화이므로 소수점 불필요)
     */
    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        return String.format("%,.0f", price);
    }

    // =========================================================
    // 6. 내부 클래스
    // =========================================================

    /**
     * 매수 후보 클래스
     * Phase 1 BuyCandidate와 동일한 구조 (market → stockCode 명칭만 변경)
     */
    @Data
    @AllArgsConstructor
    private static class BuyCandidate {
        private String stockCode;
        private TradingSignalDTO signal;
        private BigDecimal maxBuyableAmount;

        /** 우선순위 비교 (신호 강도 → 이격도, 내림차순) */
        public int compareTo(BuyCandidate other) {
            int strengthCompare = getStrengthOrder(this.signal.getStrength())
                    - getStrengthOrder(other.signal.getStrength());
            if (strengthCompare != 0) return strengthCompare;
            BigDecimal thisDropRate = this.signal.getDropRate() != null
                    ? this.signal.getDropRate() : BigDecimal.ZERO;
            BigDecimal otherDropRate = other.signal.getDropRate() != null
                    ? other.signal.getDropRate() : BigDecimal.ZERO;
            return thisDropRate.compareTo(otherDropRate);
        }

        private int getStrengthOrder(SignalStrength strength) {
            if (strength == null) return 0;
            return switch (strength) {
                case STRONG -> 3;
                case MODERATE -> 2;
                case WEAK -> 1;
            };
        }
    }

    /**
     * 봇 실행 결과 클래스
     * Phase 1 BotExecutionResult와 동일한 구조 (StockBotController에서 직접 참조)
     */
    public static class BotExecutionResult {
        private final String userId;
        private String status;
        private String message;
        private int buyCount = 0;
        private int sellCount = 0;
        private BigDecimal totalBuyAmount = BigDecimal.ZERO;
        private BigDecimal totalSellAmount = BigDecimal.ZERO;
        private BigDecimal totalProfitLoss = BigDecimal.ZERO;
        private final List<String> buyDetails = new ArrayList<>();
        private final List<String> sellDetails = new ArrayList<>();
        private final List<String> skipped = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public BotExecutionResult(String userId) { this.userId = userId; }

        public void addBuy(String stockCode, BigDecimal amount) {
            buyCount++;
            totalBuyAmount = totalBuyAmount.add(amount);
            buyDetails.add(stockCode + ": " + String.format("%,.0f", amount) + "원");
        }

        public void addSell(String stockCode, BigDecimal amount, BigDecimal profitLoss) {
            sellCount++;
            totalSellAmount = totalSellAmount.add(amount);
            totalProfitLoss = totalProfitLoss.add(profitLoss);
            sellDetails.add(stockCode + ": " + String.format("%,.0f", amount)
                    + "원 (손익: " + String.format("%,.0f", profitLoss) + "원)");
        }

        public void addSkipped(String reason) { skipped.add(reason); }
        public void addError(String error) { errors.add(error); }

        public String getUserId() { return userId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getBuyCount() { return buyCount; }
        public int getSellCount() { return sellCount; }
        public BigDecimal getTotalBuyAmount() { return totalBuyAmount; }
        public BigDecimal getTotalSellAmount() { return totalSellAmount; }
        public BigDecimal getTotalProfitLoss() { return totalProfitLoss; }
        public List<String> getBuyDetails() { return buyDetails; }
        public List<String> getSellDetails() { return sellDetails; }
        public List<String> getSkipped() { return skipped; }
        public List<String> getErrors() { return errors; }
    }
}