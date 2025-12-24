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
            
            // 4. 매수 신호 체크 (설정된 코인들)
            List<String> targetCoins = setting.getCoinSymbols();
            for (String market : targetCoins) {
                try {
                    processBuySignal(userId, market, setting, apiKeys, result);
                } catch (Exception e) {
                    log.error("매수 처리 실패: market={}, error={}", market, e.getMessage());
                    result.addError(market + " 매수 실패: " + e.getMessage());
                }
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
        TradingSignalDTO signal = signalDetectorService.detectBuySignal(market, setting);
        
        if (signal.getSignalType() != SignalType.BUY) {
            log.debug("매수 신호 없음: {} - {}", market, signal.getReason());
            return;
        }
        
        log.info("매수 신호 감지: {} - {} (강도: {})", market, signal.getReason(), signal.getStrength());
        
        // 매수 금액 계산 (일일 한도의 10% 또는 남은 한도)
        BigDecimal remainingLimit = riskManagementService.getRemainingDailyLimit(userId, setting);
        BigDecimal buyAmount = setting.getDailyLimitAmount()
                .multiply(new BigDecimal("0.1"))
                .min(remainingLimit);
        
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
            
            // 거래 내역 저장
            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .coinSymbol(market)
                    .type(TransactionType.BUY)
                    .quantity(order.getExecutedVolume() != null ? order.getExecutedVolume() : BigDecimal.ZERO)
                    .price(signal.getCurrentPrice())
                    .fee(buyAmount.multiply(FEE_RATE))
                    .totalAmount(buyAmount)
                    .targetSellPrice(signal.getTargetPrice())
                    .stopLossPrice(signal.getStopLossPrice())
                    .highestPrice(signal.getCurrentPrice())
                    .status(TransactionStatus.HOLDING)
                    .build();
            
            transactionRepository.save(transaction);
            
            // 알림 발송
            notificationService.notifyBuyExecuted(
                userId, market, signal.getCurrentPrice(), 
                transaction.getQuantity(), buyAmount
            );

            // ★★★ 추가: Discord DM 발송 ★★★
            User userEntity = userRepository.findById(userId).orElse(null);
            if (userEntity != null && userEntity.getDiscordUserId() != null) {
                discordBotService.sendBuyNotification(
                    userEntity.getDiscordUserId(),
                    market,
                    transaction.getQuantity().toPlainString(),
                    String.format("%,.0f", signal.getCurrentPrice()),
                    String.format("%,.0f", buyAmount)
                );
            }

            result.addBuy(market, buyAmount);
            log.info("매수 완료: {} - {}원 (주문ID: {})", market, buyAmount, order.getUuid());
            
        } catch (Exception e) {
            log.error("업비트 매수 주문 실패: {} - {}", market, e.getMessage());
            throw e;
        }
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

            // ⭐ 추가: 수익률 계산
            BigDecimal profitRate = holding.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                    ? profitLoss.divide(holding.getTotalAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            holding.setStatus(TransactionStatus.SOLD);
            holding.setSoldAt(LocalDateTime.now());
            holding.setSoldPrice(sellPrice);
            holding.setProfitLoss(profitLoss);
            
            transactionRepository.save(holding);
            
            // 알림 발송
            if (signal.getSignalType() == SignalType.STOP_LOSS) {
                notificationService.notifyStopLoss(
                    holding.getUserId(), holding.getCoinSymbol(),
                    sellPrice, profitLoss, profitRate    
                );
            } else {
                notificationService.notifySellExecuted(
                    holding.getUserId(), holding.getCoinSymbol(),
                    sellPrice, holding.getQuantity(), sellAmount,   
                    profitLoss, profitRate
                );
            }

            // ★★★ 추가: Discord DM 발송 ★★★
            User userEntity = userRepository.findById(holding.getUserId()).orElse(null);
            if (userEntity != null && userEntity.getDiscordUserId() != null) {
                String profitSign = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                
                if (signal.getSignalType() == SignalType.STOP_LOSS) {
                    discordBotService.sendStopLossNotification(
                        userEntity.getDiscordUserId(),
                        holding.getCoinSymbol(),
                        holding.getQuantity().toPlainString(),
                        String.format("%,.0f", sellPrice),
                        String.format("%,.0f", profitLoss),
                        profitRate.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    );
                } else {
                    discordBotService.sendSellNotification(
                        userEntity.getDiscordUserId(),
                        holding.getCoinSymbol(),
                        holding.getQuantity().toPlainString(),
                        String.format("%,.0f", sellPrice),
                        profitSign + String.format("%,.0f", profitLoss),
                        profitSign + profitRate.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    );
                }
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