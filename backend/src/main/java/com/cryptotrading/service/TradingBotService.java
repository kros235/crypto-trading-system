package com.cryptotrading.service;

import com.cryptotrading.dto.TransactionDTO;
import com.cryptotrading.dto.bot.TradingSignalDTO;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalType;
import com.cryptotrading.dto.upbit.UpbitOrderDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.entity.Transaction.TransactionType;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.TradingSettingRepository;
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.service.RiskManagementService.RiskCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cryptotrading.service.DiscordBotService;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingBotService {

    private final SignalDetectorService signalDetectorService;
    private final RiskManagementService riskManagementService;
    private final UpbitApiService upbitApiService;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DiscordBotService discordBotService;
    private final EmailService emailService; 
    
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");  // 0.05%
    private static final int SCALE = 8;

    /**
     * 특정 사용자의 자동매매 실행
     */
    public BotExecutionResult executeForUser(String userId) {
        log.info("========== 자동매매 실행 시작: {} ==========", userId);
        
        BotExecutionResult result = new BotExecutionResult(userId);
        
        try {
            // 1. 사용자 및 설정 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            if (!user.getIsActive()) {
                log.warn("비활성화된 사용자: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("비활성화된 사용자");
                return result;
            }
            
            TradingSetting setting = tradingSettingRepository.findByUserId(userId)
                    .orElse(null);
            
            if (setting == null) {
                log.warn("거래 설정 없음: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("거래 설정 없음");
                return result;
            }

            // 긴급 정지 조건 확인
            if (riskManagementService.isEmergencyStopActive(userId, setting)) {
                log.warn("⚠️ 긴급 정지 상태: {} - 일일 손실 한도 도달로 거래 중단", userId);
                result.setStatus("EMERGENCY_STOP");
                result.setMessage("긴급 정지 - 일일 손실 한도 도달");
                return result;
            }
            
            // 2. API 키 확인
            String[] apiKeys;
            try {
                apiKeys = userService.getDecryptedApiKeys(userId);
            } catch (Exception e) {
                log.warn("API 키 없음: {}", userId);
                result.setStatus("SKIP");
                result.setMessage("API 키 미등록");
                return result;
            }
            
            // 3. 매도 신호 체크 (보유 중인 거래)
            List<Transaction> holdings = transactionRepository
                    .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
            
            for (Transaction holding : holdings) {
                try {
                    processSellSignal(holding, setting, apiKeys, result);
                } catch (Exception e) {
                    log.error("매도 처리 실패: transactionId={}, error={}", holding.getTransactionId(), e.getMessage());
                    result.addError(holding.getCoinSymbol() + " 매도 실패: " + e.getMessage());
                }
            }
            
            // 4. 매수 신호 체크 - 라운드로빈 방식으로 변경
            // 기존: 순차 처리 (코드 순서대로 선착순 매수)
            // 변경: 라운드로빈 (매수 신호 발생 코인에 균등 분배, 신호 강도 우선)
            try {
                processRoundRobinBuy(userId, setting, apiKeys, result);
            } catch (Exception e) {
                log.error("라운드로빈 매수 처리 실패: userId={}, error={}", userId, e.getMessage());
                result.addError("라운드로빈 매수 실패: " + e.getMessage());
            }
            
            // 5. 보유 중인 거래의 최고가 업데이트
            updateHighestPrices(holdings);
            
            result.setStatus("SUCCESS");
            log.info("========== 자동매매 실행 완료: {} - 매수 {}건, 매도 {}건 ==========", 
                    userId, result.getBuyCount(), result.getSellCount());
            
        } catch (Exception e) {
            log.error("자동매매 실행 오류: userId={}, error={}", userId, e.getMessage(), e);
            result.setStatus("ERROR");
            result.setMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * 매수 신호 처리
     */
    private void processBuySignal(String userId, String market, TradingSetting setting, 
                                   String[] apiKeys, BotExecutionResult result) {
        TradingSignalDTO signal = signalDetectorService.detectBuySignal(market, setting, userId);
        
        if (signal.getSignalType() != SignalType.BUY) {
            log.debug("매수 신호 없음: {} - {}", market, signal.getReason());
            return;
        }
        
        log.info("매수 신호 감지: {} - {} (강도: {})", market, signal.getReason(), signal.getStrength());
        
        // ⭐⭐⭐ 수정: 매수 금액 계산 로직 변경 ⭐⭐⭐
        // - dailyLimitAmount 대신 남은 일일 한도 사용 (총자산 스냅샷 기준)
        // - buyAmountPct 대신 fixedBuyAmount 사용
        BigDecimal remainingLimit = riskManagementService.getRemainingDailyLimit(userId, setting);
        
        // 매수 방식에 따른 금액 계산
        boolean useRoundRobin = setting.getUseRoundRobin() != null ? setting.getUseRoundRobin() : true;
        BigDecimal buyAmount;
        
        if (useRoundRobin) {
            // 라운드로빈: 이 메서드에서는 단일 코인이므로 남은 한도 전체 사용
            buyAmount = remainingLimit;
        } else {
            // 고정 금액: fixedBuyAmount 사용
            BigDecimal fixedAmount = setting.getFixedBuyAmount() != null 
                    ? setting.getFixedBuyAmount() 
                    : new BigDecimal("10000");
            buyAmount = fixedAmount.min(remainingLimit);
        }
        
        if (buyAmount.compareTo(new BigDecimal("5000")) < 0) {  // 최소 5000원
            log.info("매수 금액 부족: {} ({}원)", market, buyAmount);
            return;
        }
        
        // 리스크 체크
        RiskCheckResult riskResult = riskManagementService.canBuy(userId, market, buyAmount, setting);
        if (!riskResult.isPassed()) {
            log.info("리스크 체크 실패: {} - {}", market, riskResult.getReason());
            result.addSkipped(market + ": " + riskResult.getReason());
            return;
        }
        
        // 실제 매수 실행
        try {
            UpbitOrderDTO order = upbitApiService.orderBuy(apiKeys[0], apiKeys[1], market, buyAmount);
            
            // 체결 수량 확인을 위해 주문 조회 (최대 3회 재시도)
            BigDecimal executedVolume = BigDecimal.ZERO;
            int retryCount = 0;
            final int MAX_RETRY = 3;
            
            while (retryCount < MAX_RETRY) {
                try {
                    Thread.sleep(500);  // 0.5초 대기
                    UpbitOrderDTO orderStatus = upbitApiService.getOrder(apiKeys[0], apiKeys[1], order.getUuid());
                    
                    if (orderStatus.getExecutedVolume() != null && 
                        orderStatus.getExecutedVolume().compareTo(BigDecimal.ZERO) > 0) {
                        executedVolume = orderStatus.getExecutedVolume();
                        log.info("체결 수량 확인: {} - {}개", market, executedVolume);
                        break;
                    }
                    
                    // 주문 상태가 'done'이면 종료
                    if ("done".equals(orderStatus.getState())) {
                        executedVolume = orderStatus.getExecutedVolume() != null 
                                ? orderStatus.getExecutedVolume() 
                                : BigDecimal.ZERO;
                        break;
                    }
                    
                    retryCount++;
                    log.debug("체결 대기 중... (시도 {}/{})", retryCount, MAX_RETRY);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 체결 수량을 여전히 못 가져온 경우 금액/현재가로 추정
            if (executedVolume.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal fee = buyAmount.multiply(FEE_RATE);
                BigDecimal actualBuyAmount = buyAmount.subtract(fee);
                executedVolume = actualBuyAmount.divide(signal.getCurrentPrice(), SCALE, RoundingMode.DOWN);
                log.warn("체결 수량 조회 실패, 추정 수량 사용: {} - {}개", market, executedVolume);
            }
            
            // 거래 내역 저장
            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .coinSymbol(market)
                    .type(TransactionType.BUY)
                    .quantity(executedVolume)  // ⭐ 변경: order.getExecutedVolume() → executedVolume
                    .price(signal.getCurrentPrice())
                    .fee(buyAmount.multiply(FEE_RATE))
                    .totalAmount(buyAmount)
                    .targetSellPrice(signal.getTargetPrice())
                    .stopLossPrice(signal.getStopLossPrice())
                    .highestPrice(signal.getCurrentPrice())
                    .status(TransactionStatus.HOLDING)
                    .note("[매수] " + signal.getReason())
                    .build();
            
            transactionRepository.save(transaction);
            
            // 알림 발송
            // ⭐⭐⭐ [수정] reason 파라미터 추가 ⭐⭐⭐
            notificationService.notifyBuyExecuted(
                userId, market, signal.getCurrentPrice(), 
                transaction.getQuantity(), buyAmount,
                signal.getReason()  // ⭐⭐⭐ [추가] 매수 사유 전달 ⭐⭐⭐
            );

            // Discord DM 발송
            User userEntity = userRepository.findById(userId).orElse(null);
            if (userEntity != null && userEntity.getDiscordUserId() != null) {
                discordBotService.sendBuyNotification(
                    userEntity.getDiscordUserId(),
                    market,
                    transaction.getQuantity().toPlainString(),
                    String.format("%,.0f", signal.getCurrentPrice()),
                    String.format("%,.0f", buyAmount),
                    signal.getReason()  // ⭐⭐⭐ [추가] 매수 사유 전달 ⭐⭐⭐
                );
            }

            // 이메일 발송
            if (userEntity != null && userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
                emailService.sendTradeNotification(
                    userEntity.getEmail(),
                    "BUY",
                    market,
                    transaction.getQuantity(),
                    signal.getCurrentPrice(),
                    buyAmount,
                    signal.getReason()
                );
            }

            result.addBuy(market, buyAmount);
            log.info("매수 완료: {} - {}원 (주문ID: {})", market, buyAmount, order.getUuid());
            
        } catch (Exception e) {
            log.error("업비트 매수 주문 실패: {} - {}", market, e.getMessage());
            throw e;
        }
    }

    // 라운드로빈 방식 매수 처리
    /**
     * 라운드로빈 방식으로 매수 신호 처리
     * - 매수 신호 발생 코인들에게 남은 한도를 균등 분배
     * - 균등 분배 금액이 최소 금액 미만 시 신호 강도 순 우선 매수
     */
    private void processRoundRobinBuy(String userId, TradingSetting setting, 
                                       String[] apiKeys, BotExecutionResult result) {
        log.info("🔄 라운드로빈 매수 처리 시작: {}", userId);
        
        // ===== 1단계: 매수 후보 수집 =====
        List<BuyCandidate> candidates = new ArrayList<>();
        List<String> targetCoins = setting.getCoinSymbols();
        
        for (String market : targetCoins) {
            try {
                // 매수 신호 감지
                TradingSignalDTO signal = signalDetectorService.detectBuySignal(market, setting, userId);
                
                if (signal.getSignalType() != SignalType.BUY) {
                    log.debug("매수 신호 없음: {} - {}", market, signal.getReason());
                    continue;
                }
                
                // 리스크 사전 체크 (보유 건수만 체크, 금액은 나중에)
                if (!riskManagementService.checkMaxHoldings(userId, market, setting)) {
                    log.info("보유 건수 초과: {} - 최대 {}건", market, setting.getMaxHoldingsPerCoin());
                    result.addSkipped(market + ": 보유 건수 초과");
                    continue;
                }
                
                // 비중 제한 고려한 최대 매수 가능 금액
                BigDecimal maxBuyable = riskManagementService.getRemainingPositionAmount(userId, market, setting);
                if (maxBuyable.compareTo(new BigDecimal("5000")) < 0) {
                    log.info("비중 제한 초과: {} - 남은 가능액 {}원", market, maxBuyable);
                    result.addSkipped(market + ": 비중 제한 초과");
                    continue;
                }
                
                log.info("✅ 매수 후보 추가: {} (강도: {}, 이격도: {}%, 최대매수가능: {}원)", 
                        market, signal.getStrength(), signal.getDropRate(), maxBuyable);
                candidates.add(new BuyCandidate(market, signal, maxBuyable));
                
            } catch (Exception e) {
                log.error("매수 후보 수집 실패: market={}, error={}", market, e.getMessage());
            }
        }
        
        if (candidates.isEmpty()) {
            log.info("매수 후보 없음 - 라운드로빈 종료");
            return;
        }
        
        log.info("📋 매수 후보 {}개 수집 완료", candidates.size());
        
        // ===== 2단계: 매수 방식 확인 및 금액 계산 =====
        BigDecimal remainingLimit = riskManagementService.getRemainingDailyLimit(userId, setting);
        log.info("💰 남은 일일 한도: {}원", remainingLimit);
        
        if (remainingLimit.compareTo(new BigDecimal("5000")) < 0) {
            log.info("일일 한도 부족 - 매수 처리 종료");
            return;
        }
        
        // ⭐⭐⭐ 수정: usePerTradeLimit → useRoundRobin (의미 변경) ⭐⭐⭐
        // true: 라운드로빈 (균등 분배)
        // false: 고정 금액 (fixedBuyAmount)
        boolean useRoundRobin = setting.getUseRoundRobin() != null ? setting.getUseRoundRobin() : true;
        
        BigDecimal perCoinAmount;
        BigDecimal fixedBuyAmount = setting.getFixedBuyAmount() != null 
                ? setting.getFixedBuyAmount() 
                : new BigDecimal("10000");
        
        if (useRoundRobin) {
            // 라운드로빈: 남은 한도를 코인 수로 균등 분배
            perCoinAmount = remainingLimit.divide(
                    new BigDecimal(candidates.size()), SCALE, RoundingMode.DOWN);
            log.info("📊 [라운드로빈] 균등 분배 금액: {}원 ({}개 코인)", perCoinAmount, candidates.size());
        } else {
            // 고정 금액: 각 코인에 fixedBuyAmount만큼 매수
            perCoinAmount = fixedBuyAmount;
            log.info("📊 [고정금액] 1회 매수 금액: {}원", perCoinAmount);
        }
        
        // ===== 3단계: 최소 금액 체크 및 우선순위 선정 =====
        final BigDecimal MIN_AMOUNT = new BigDecimal("5000");
        
        if (perCoinAmount.compareTo(MIN_AMOUNT) < 0) {
            // ⭐⭐⭐ 수정: 고정 금액 모드에서 최소 금액 미달 시 매수 불가 ⭐⭐⭐
            if (!useRoundRobin) {
                log.warn("⚠️ [고정금액] 설정 금액 {}원 < 최소 {}원 - 매수 불가", perCoinAmount, MIN_AMOUNT);
                log.warn("   → 거래 설정에서 '1회 매수 금액'을 5,000원 이상으로 설정해주세요.");
                return;
            }
            
            // 라운드로빈 모드: 우선순위 높은 코인만 선정
            int maxCoins = remainingLimit.divide(MIN_AMOUNT, 0, RoundingMode.DOWN).intValue();
            
            if (maxCoins == 0) {
                log.info("최소 금액 미달로 매수 불가 - 라운드로빈 종료");
                return;
            }
            
            log.info("⚠️ [라운드로빈] 균등 분배 {}원 < 최소 {}원 → 상위 {}개 코인만 선정", 
                    perCoinAmount, MIN_AMOUNT, maxCoins);
            
            // 우선순위 정렬 (신호 강도 → 이격도)
            candidates.sort((a, b) -> b.compareTo(a)); // 내림차순 (우선순위 높은 것 먼저)
            
            // 로그: 정렬 결과
            for (int i = 0; i < candidates.size(); i++) {
                BuyCandidate c = candidates.get(i);
                log.info("  {}순위: {} (강도: {}, 이격도: {}%)", 
                        i + 1, c.getMarket(), c.getSignal().getStrength(), c.getSignal().getDropRate());
            }
            
            // 상위 N개만 선택
            candidates = candidates.subList(0, Math.min(maxCoins, candidates.size()));
            
            // 재분배
            perCoinAmount = remainingLimit.divide(
                    new BigDecimal(candidates.size()), SCALE, RoundingMode.DOWN);
            log.info("📊 [라운드로빈] 재분배 금액: {}원 ({}개 코인)", perCoinAmount, candidates.size());
        }
        
        // ===== 4단계: 매수 실행 =====
        BigDecimal usedAmount = BigDecimal.ZERO;
        BigDecimal carryOver = BigDecimal.ZERO; // 비중 제한으로 못 쓴 금액 (라운드로빈에서만 사용)
        
        for (int i = 0; i < candidates.size(); i++) {
            BuyCandidate candidate = candidates.get(i);
            
            // ⭐⭐⭐ 수정: 매수 방식에 따른 금액 계산 ⭐⭐⭐
            BigDecimal actualAmount;
            
            if (useRoundRobin) {
                // 라운드로빈: 분배 금액 + 이월 금액
                BigDecimal allocatedAmount = perCoinAmount.add(carryOver);
                actualAmount = allocatedAmount.min(candidate.getMaxBuyableAmount());
                
                // 일일 한도 초과 체크
                if (usedAmount.add(actualAmount).compareTo(remainingLimit) > 0) {
                    actualAmount = remainingLimit.subtract(usedAmount);
                }
            } else {
                // 고정 금액: fixedBuyAmount 사용 (비중 제한 적용)
                actualAmount = fixedBuyAmount.min(candidate.getMaxBuyableAmount());
                
                // 남은 한도 체크
                BigDecimal currentRemaining = remainingLimit.subtract(usedAmount);
                if (actualAmount.compareTo(currentRemaining) > 0) {
                    log.info("⚠️ {} 일일 한도 부족: 필요 {}원, 남은 한도 {}원 - 스킵", 
                            candidate.getMarket(), actualAmount, currentRemaining);
                    result.addSkipped(candidate.getMarket() + ": 일일 한도 부족");
                    continue;
                }
            }
            
            // 최소 금액 체크
            if (actualAmount.compareTo(MIN_AMOUNT) < 0) {
                log.info("❌ {} 매수 스킵: 실제 금액 {}원 < 최소 {}원", 
                        candidate.getMarket(), actualAmount, MIN_AMOUNT);
                // ⭐⭐⭐ 수정: 라운드로빈에서만 이월 처리 ⭐⭐⭐
                if (useRoundRobin) {
                    carryOver = carryOver.add(perCoinAmount); // 다음 코인에 이월
                }
                continue;
            }
            
            // ⭐⭐⭐ 수정: 이월 금액 계산 (라운드로빈에서만) ⭐⭐⭐
            if (useRoundRobin) {
                BigDecimal allocatedAmount = perCoinAmount.add(carryOver);
                if (actualAmount.compareTo(allocatedAmount) < 0) {
                    carryOver = allocatedAmount.subtract(actualAmount);
                    log.info("💫 [라운드로빈] 비중 제한으로 {}원 이월 → 다음 코인에 재분배", carryOver);
                } else {
                    carryOver = BigDecimal.ZERO;
                }
            }
            
            // 실제 매수 실행
            try {
                executeBuyOrder(userId, candidate.getMarket(), actualAmount, 
                        candidate.getSignal(), apiKeys, result);
                usedAmount = usedAmount.add(actualAmount);
                log.info("✅ {} 매수 완료: {}원", candidate.getMarket(), actualAmount);
            } catch (Exception e) {
                log.error("❌ {} 매수 실패: {}", candidate.getMarket(), e.getMessage());
                result.addError(candidate.getMarket() + " 매수 실패: " + e.getMessage());
                carryOver = carryOver.add(actualAmount); // 실패한 금액 이월
            }
        }
        
        // ⭐⭐⭐ 수정: 매수 방식에 따른 로그 ⭐⭐⭐
        if (useRoundRobin) {
            log.info("🔄 [라운드로빈] 매수 완료: 총 {}원 사용 (한도 {}원)", usedAmount, remainingLimit);
        } else {
            log.info("💵 [고정금액] 매수 완료: 총 {}원 사용 (1회 {}원 × {}건)", 
                    usedAmount, fixedBuyAmount, result.getBuyCount());
        }
    }

    /**
     * 실제 매수 주문 실행 (라운드로빈에서 호출)
     */
    private void executeBuyOrder(String userId, String market, BigDecimal buyAmount,
                                  TradingSignalDTO signal, String[] apiKeys, 
                                  BotExecutionResult result) {
        log.info("매수 주문 실행: {} - {}원", market, buyAmount);
        
        // 업비트 매수 주문
        UpbitOrderDTO order = upbitApiService.orderBuy(apiKeys[0], apiKeys[1], market, buyAmount);
        
        // 체결 수량 확인 (최대 3회 재시도)
        BigDecimal executedVolume = BigDecimal.ZERO;
        int retryCount = 0;
        final int MAX_RETRY = 3;
        
        while (retryCount < MAX_RETRY) {
            try {
                Thread.sleep(500);
                UpbitOrderDTO orderStatus = upbitApiService.getOrder(apiKeys[0], apiKeys[1], order.getUuid());
                
                if (orderStatus.getExecutedVolume() != null && 
                    orderStatus.getExecutedVolume().compareTo(BigDecimal.ZERO) > 0) {
                    executedVolume = orderStatus.getExecutedVolume();
                    break;
                }
                
                if ("done".equals(orderStatus.getState())) {
                    executedVolume = orderStatus.getExecutedVolume() != null 
                            ? orderStatus.getExecutedVolume() : BigDecimal.ZERO;
                    break;
                }
                
                retryCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 체결 수량 조회 실패 시 추정
        if (executedVolume.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal fee = buyAmount.multiply(FEE_RATE);
            BigDecimal actualBuyAmount = buyAmount.subtract(fee);
            executedVolume = actualBuyAmount.divide(signal.getCurrentPrice(), SCALE, RoundingMode.DOWN);
            log.warn("체결 수량 조회 실패, 추정 수량 사용: {} - {}개", market, executedVolume);
        }
        
        // 거래 내역 저장
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .coinSymbol(market)
                .type(TransactionType.BUY)
                .quantity(executedVolume)
                .price(signal.getCurrentPrice())
                .fee(buyAmount.multiply(FEE_RATE))
                .totalAmount(buyAmount)
                .targetSellPrice(signal.getTargetPrice())
                .stopLossPrice(signal.getStopLossPrice())
                .highestPrice(signal.getCurrentPrice())
                .status(TransactionStatus.HOLDING)
                .note("[매수] " + signal.getReason())
                .build();
        
        transactionRepository.save(transaction);
        
        // 알림 발송
        notificationService.notifyBuyExecuted(
            userId, market, signal.getCurrentPrice(), 
            transaction.getQuantity(), buyAmount,
            signal.getReason()
        );

        // Discord DM 발송
        User userEntity = userRepository.findById(userId).orElse(null);
        if (userEntity != null && userEntity.getDiscordUserId() != null) {
            discordBotService.sendBuyNotification(
                userEntity.getDiscordUserId(),
                market,
                transaction.getQuantity().toPlainString(),
                String.format("%,.0f", signal.getCurrentPrice()),
                String.format("%,.0f", buyAmount),
                signal.getReason()
            );
        }

        // 이메일 발송
        if (userEntity != null && userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
            emailService.sendTradeNotification(
                userEntity.getEmail(),
                "BUY",
                market,
                transaction.getQuantity(),
                signal.getCurrentPrice(),
                buyAmount,
                signal.getReason()
            );
        }

        result.addBuy(market, buyAmount);
        log.info("매수 완료: {} - {}원 (주문ID: {})", market, buyAmount, order.getUuid());
    }

    /**
     * 매도 신호 처리
     */
    private void processSellSignal(Transaction holding, TradingSetting setting, 
                                    String[] apiKeys, BotExecutionResult result) {
        TradingSignalDTO signal = signalDetectorService.detectSellSignal(holding, setting);
        
        if (signal.getSignalType() == SignalType.HOLD) {
            log.debug("매도 신호 없음: {} - {}", holding.getCoinSymbol(), signal.getReason());
            return;
        }
        
        log.info("매도 신호 감지: {} - {} (타입: {})", 
                holding.getCoinSymbol(), signal.getReason(), signal.getSignalType());
        
        // 실제 매도 실행
        try {
            UpbitOrderDTO order = upbitApiService.orderSell(
                    apiKeys[0], apiKeys[1], 
                    holding.getCoinSymbol(), 
                    holding.getQuantity()
            );
            
            // 거래 내역 업데이트
            BigDecimal sellPrice = signal.getCurrentPrice();
            BigDecimal sellAmount = holding.getQuantity().multiply(sellPrice);
            BigDecimal fee = sellAmount.multiply(FEE_RATE);
            BigDecimal profitLoss = sellAmount.subtract(fee)
                    .subtract(holding.getTotalAmount());

            // 수익률 계산
            BigDecimal profitRate = holding.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                    ? profitLoss.divide(holding.getTotalAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            holding.setStatus(TransactionStatus.SOLD);
            holding.setSoldAt(LocalDateTime.now());
            holding.setSoldPrice(sellPrice);
            holding.setProfitLoss(profitLoss);
            holding.setProfitLossPct(profitRate);
            // 기존 매수 사유에 매도 사유 추가
            String existingNote = holding.getNote() != null ? holding.getNote() : "";
            holding.setNote(existingNote + " → [매도] " + signal.getReason());

            transactionRepository.save(holding);

            // 일일 한도 복구 기록
            riskManagementService.recordSellForDailyLimitRecovery(holding.getUserId(), sellAmount, setting);
            
            // 알림 발송
            // reason 파라미터 추가
            if (signal.getSignalType() == SignalType.STOP_LOSS) {
                notificationService.notifyStopLoss(
                    holding.getUserId(), holding.getCoinSymbol(),
                    sellPrice, profitLoss, profitRate    
                );
            } else {
                notificationService.notifySellExecuted(
                    holding.getUserId(), holding.getCoinSymbol(),
                    sellPrice, holding.getQuantity(), sellAmount,   
                    profitLoss, profitRate,
                    signal.getReason()  // ⭐⭐⭐ [추가] 매도 사유 전달 ⭐⭐⭐
                );
            }

            // ★★★ 추가: Discord DM 발송 ★★★
            User userEntity = userRepository.findById(holding.getUserId()).orElse(null);
            if (userEntity != null && userEntity.getDiscordUserId() != null) {
                String profitSign = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                discordBotService.sendSellNotification(
                    userEntity.getDiscordUserId(),
                    holding.getCoinSymbol(),
                    holding.getQuantity().toPlainString(),
                    String.format("%,.0f", sellPrice),
                    profitSign + String.format("%,.0f", profitLoss),
                    profitSign + profitRate.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    signal.getReason()  // ⭐⭐⭐ [추가] 매도 사유 전달 ⭐⭐⭐
                );
            }

            // 이메일 발송
            if (userEntity != null && userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
                emailService.sendTradeNotification(
                    userEntity.getEmail(),
                    "SELL",
                    holding.getCoinSymbol(),
                    holding.getQuantity(),
                    sellPrice,
                    sellAmount,
                    signal.getReason()
                );
            }

            // ⭐⭐⭐ [추가] 연속 손절/수익 실현 카운터 업데이트 ⭐⭐⭐
            if (signal.getSignalType() == SignalType.STOP_LOSS || 
                signal.getSignalType() == SignalType.TRAILING_STOP && profitLoss.compareTo(BigDecimal.ZERO) < 0) {
                // 손절 발생 - 카운터 증가
                riskManagementService.recordStopLoss(holding.getUserId(), holding.getCoinSymbol(), setting);
            } else if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
                // 수익 실현 - 카운터 리셋
                riskManagementService.recordProfitSell(holding.getUserId(), holding.getCoinSymbol());
            }

            result.addSell(holding.getCoinSymbol(), sellAmount, profitLoss);
            log.info("매도 완료: {} - {}원, 손익: {}원 (주문ID: {})", 
                    holding.getCoinSymbol(), sellAmount, profitLoss, order.getUuid());
            
        } catch (Exception e) {
            log.error("업비트 매도 주문 실패: {} - {}", holding.getCoinSymbol(), e.getMessage());
            throw e;
        }
    }

    /**
     * 보유 거래의 최고가 업데이트 (트레일링 스톱용)
     */
    private void updateHighestPrices(List<Transaction> holdings) {
        for (Transaction holding : holdings) {
            try {
                List<UpbitTickerDTO> tickers = upbitApiService.getTicker(List.of(holding.getCoinSymbol()));
                if (!tickers.isEmpty()) {
                    BigDecimal currentPrice = tickers.get(0).getTradePrice();
                    
                    if (holding.getHighestPrice() == null || 
                        currentPrice.compareTo(holding.getHighestPrice()) > 0) {
                        holding.setHighestPrice(currentPrice);
                        transactionRepository.save(holding);
                        log.debug("최고가 업데이트: {} - {}", holding.getCoinSymbol(), currentPrice);
                    }
                }
            } catch (Exception e) {
                log.warn("최고가 업데이트 실패: {} - {}", holding.getCoinSymbol(), e.getMessage());
            }
        }
    }

    /**
     * 모든 활성 사용자에 대해 자동매매 실행
     */
    public List<BotExecutionResult> executeForAllUsers() {
        log.info("========== 전체 사용자 자동매매 시작 ==========");
        
        List<User> activeUsers = userRepository.findByIsActive(true);
        List<BotExecutionResult> results = new ArrayList<>();
        
        for (User user : activeUsers) {
            BotExecutionResult result = executeForUser(user.getUserId());
            results.add(result);
        }
        
        log.info("========== 전체 사용자 자동매매 완료: {}명 처리 ==========", results.size());
        return results;
    }

    // 라운드로빈 매수 후보 클래스
    /**
     * 매수 후보 정보를 담는 내부 클래스
     */
    @Data
    @AllArgsConstructor
    private static class BuyCandidate {
        private String market;              // 코인 마켓 (예: KRW-BTC)
        private TradingSignalDTO signal;    // 매수 신호 정보
        private BigDecimal maxBuyableAmount; // 비중 제한 고려한 최대 매수 가능 금액
        
        /**
         * 우선순위 비교 (신호 강도 → 이격도)
         * 강도가 높을수록, 이격도가 낮을수록(더 많이 하락) 우선
         */
        public int compareTo(BuyCandidate other) {
            // 1. 신호 강도 비교 (STRONG > MODERATE > WEAK)
            int strengthCompare = getStrengthOrder(this.signal.getStrength()) 
                    - getStrengthOrder(other.signal.getStrength());
            if (strengthCompare != 0) {
                return strengthCompare; // 양수면 this가 우선
            }
            
            // 2. 이격도 비교 (더 많이 하락한 코인 우선, 즉 dropRate가 더 작은 것)
            BigDecimal thisDropRate = this.signal.getDropRate() != null 
                    ? this.signal.getDropRate() : BigDecimal.ZERO;
            BigDecimal otherDropRate = other.signal.getDropRate() != null 
                    ? other.signal.getDropRate() : BigDecimal.ZERO;
            return thisDropRate.compareTo(otherDropRate); // 음수면 this가 더 많이 하락
        }
        
        private int getStrengthOrder(TradingSignalDTO.SignalStrength strength) {
            return switch (strength) {
                case STRONG -> 3;
                case MODERATE -> 2;
                case WEAK -> 1;
            };
        }
    }

    /**
     * 봇 실행 결과 클래스
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
        private List<String> buyDetails = new ArrayList<>();
        private List<String> sellDetails = new ArrayList<>();
        private List<String> skipped = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public BotExecutionResult(String userId) {
            this.userId = userId;
        }

        public void addBuy(String market, BigDecimal amount) {
            buyCount++;
            totalBuyAmount = totalBuyAmount.add(amount);
            buyDetails.add(market + ": " + amount + "원");
        }

        public void addSell(String market, BigDecimal amount, BigDecimal profitLoss) {
            sellCount++;
            totalSellAmount = totalSellAmount.add(amount);
            totalProfitLoss = totalProfitLoss.add(profitLoss);
            sellDetails.add(market + ": " + amount + "원 (손익: " + profitLoss + "원)");
        }

        public void addSkipped(String reason) {
            skipped.add(reason);
        }

        public void addError(String error) {
            errors.add(error);
        }

        // Getters and Setters
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