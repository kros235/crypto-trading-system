package com.cryptotrading.service;

import com.cryptotrading.dto.bot.TradingSignalDTO;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalStrength;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalType;
import com.cryptotrading.dto.indicator.IndicatorResultDTO;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalDetectorService {

    private final TechnicalIndicatorService indicatorService;
    private final TransactionRepository transactionRepository;
    private final NewsAnalysisService newsAnalysisService;
    
    private static final int SCALE = 8;

    /**
     * 매수 신호 감지 + AI 가중치 반영
     */
    public TradingSignalDTO detectBuySignal(String market, TradingSetting setting, String userId) {
        log.debug("매수 신호 감지 시작: {}", market);
        
        IndicatorResultDTO indicators = indicatorService.calculateIndicators(
                    market,
                    setting.getRsiPeriod() != null ? setting.getRsiPeriod() : 14,
                    setting.getRsiBuyThreshold() != null ? setting.getRsiBuyThreshold() : 30,
                    setting.getRsiSellThreshold() != null ? setting.getRsiSellThreshold() : 70,
                    setting.getBbPeriod() != null ? setting.getBbPeriod() : 20,
                    setting.getBbMultiplier() != null ? setting.getBbMultiplier() : 2,
                    setting.getVolumeThreshold() != null ? setting.getVolumeThreshold() : 150
            );
    
            if (indicators == null) {
                return createHoldSignal(market, "지표 데이터 부족");
            }
        
        List<String> reasons = new ArrayList<>();
        int conditionsMet = 0;
        int totalConditions = 4;
        
        // 조건 1: 이동평균선 기준 하락률 체크
        BigDecimal ma = getMAByPeriod(indicators, setting.getBasePeriod());
        BigDecimal dropRate = calculateDropRate(indicators.getCurrentPrice(), ma);
        BigDecimal baseThreshold = setting.getBuyThresholdPct();  // 음수값 (예: -5)
        
        // ⭐⭐⭐ [추가] AI 가중치 적용 ⭐⭐⭐
        BigDecimal aiWeight = BigDecimal.ZERO;
        if (setting.getUseAiAnalysis() != null && setting.getUseAiAnalysis()) {
            try {
                aiWeight = newsAnalysisService.getWeightAdjustment(userId, market);
                log.debug("AI 가중치 적용 - 코인: {}, 가중치: {}%", market, aiWeight);
            } catch (Exception e) {
                log.warn("AI 가중치 조회 실패 (기본값 0% 사용): {}", e.getMessage());
            }
        }
        
        // 최종 매수 기준값 = 기본값 + AI 가중치
        // 예: -5% + 0.5% = -4.5% (호재 시 매수 조건 완화)
        // 예: -5% + (-0.5%) = -5.5% (악재 시 매수 조건 강화)
        BigDecimal threshold = baseThreshold.add(aiWeight);
        // ⭐⭐⭐ [추가 끝] ⭐⭐⭐
        
        if (dropRate.compareTo(threshold) <= 0) {
            conditionsMet++;
            // ⭐ [수정] AI 가중치 정보 추가
            String reasonMsg = String.format("MA%d 대비 %.2f%% 하락 (기준: %.2f%%", 
                    setting.getBasePeriod(), dropRate, threshold);
            if (aiWeight.compareTo(BigDecimal.ZERO) != 0) {
                reasonMsg += String.format(", AI가중치: %+.1f%%", aiWeight);
            }
            reasonMsg += ")";
            reasons.add(reasonMsg);
        }
        
        // 조건 2: RSI 과매도 신호
        if (indicators.isRsiBuySignal()) {
            conditionsMet++;
            reasons.add(String.format("RSI 과매도: %.2f", indicators.getRsi14()));
        }
        
        // 조건 3: 볼린저 밴드 하단 접촉
        if (indicators.isBelowBBLower()) {
            conditionsMet++;
            reasons.add("볼린저 밴드 하단 접촉");
        }
        
        // 조건 4: 거래량 급증
        if (indicators.isHighVolume()) {
            conditionsMet++;
            reasons.add(String.format("거래량 급증: %.1f%%", 
                    indicators.getVolumeRatio().multiply(new BigDecimal("100"))));
        }
        
        // 최소 1개 조건 충족 시 매수 신호
        if (conditionsMet >= 1 && dropRate.compareTo(threshold) <= 0) {
            SignalStrength strength = determineStrength(conditionsMet, totalConditions);
            
            // 목표가 및 손절가 계산
            BigDecimal targetPrice = indicators.getCurrentPrice()
                    .multiply(BigDecimal.ONE.add(setting.getSellTargetPct().divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP)));
            BigDecimal stopLossPrice = indicators.getCurrentPrice()
                    .multiply(BigDecimal.ONE.add(setting.getStopLossPct().divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP)));
            
            return TradingSignalDTO.builder()
                    .market(market)
                    .signalType(SignalType.BUY)
                    .strength(strength)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(indicators.getCurrentPrice())
                    .targetPrice(targetPrice)
                    .stopLossPrice(stopLossPrice)
                    .reason(String.join(", ", reasons))
                    .conditionsMet(conditionsMet)
                    .totalConditions(totalConditions)
                    .build();
        }
        
        return createHoldSignal(market, "매수 조건 미충족");
    }

    /**
     * ⭐ [추가] 기존 호환성 유지용 오버로드 (userId 없이 호출 시)
     */
    public TradingSignalDTO detectBuySignal(String market, TradingSetting setting) {
        return detectBuySignal(market, setting, null);
    }

    /**
     * 매도 신호 감지 (보유 중인 거래에 대해)
     */
    public TradingSignalDTO detectSellSignal(Transaction transaction, TradingSetting setting) {
        String market = transaction.getCoinSymbol();
        log.debug("매도 신호 감지 시작: {} (거래ID: {})", market, transaction.getTransactionId());
        
        IndicatorResultDTO indicators = indicatorService.calculateIndicators(
                market,
                setting.getRsiPeriod() != null ? setting.getRsiPeriod() : 14,
                setting.getRsiBuyThreshold() != null ? setting.getRsiBuyThreshold() : 30,
                setting.getRsiSellThreshold() != null ? setting.getRsiSellThreshold() : 70,
                setting.getBbPeriod() != null ? setting.getBbPeriod() : 20,
                setting.getBbMultiplier() != null ? setting.getBbMultiplier() : 2,
                setting.getVolumeThreshold() != null ? setting.getVolumeThreshold() : 150
        );
    
        if (indicators == null) {
            return createHoldSignal(market, "지표 데이터 부족");
        }
        
        BigDecimal currentPrice = indicators.getCurrentPrice();
        BigDecimal buyPrice = transaction.getPrice();
        BigDecimal profitRate = currentPrice.subtract(buyPrice)
                .divide(buyPrice, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        // 조건 1: 목표 수익률 도달
        if (profitRate.compareTo(setting.getSellTargetPct()) >= 0) {
            return TradingSignalDTO.builder()
                    .market(market)
                    .signalType(SignalType.SELL)
                    .strength(SignalStrength.STRONG)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(currentPrice)
                    .reason(String.format("목표 수익률 도달: %.2f%% (목표: %.2f%%)", 
                            profitRate, setting.getSellTargetPct()))
                    .conditionsMet(1)
                    .totalConditions(1)
                    .build();
        }
        
        // 조건 2: 손절매 (음수)
        if (profitRate.compareTo(setting.getStopLossPct()) <= 0) {
            return TradingSignalDTO.builder()
                    .market(market)
                    .signalType(SignalType.STOP_LOSS)
                    .strength(SignalStrength.STRONG)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(currentPrice)
                    .reason(String.format("손절매 도달: %.2f%% (기준: %.2f%%)", 
                            profitRate, setting.getStopLossPct()))
                    .conditionsMet(1)
                    .totalConditions(1)
                    .build();
        }
        
        // 조건 3: 트레일링 스톱 (옵션)
        if (setting.getUseTrailingStop() && transaction.getHighestPrice() != null) {
            BigDecimal highestPrice = transaction.getHighestPrice();
            BigDecimal dropFromHigh = currentPrice.subtract(highestPrice)
                    .divide(highestPrice, SCALE, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            
            BigDecimal trailingPct = setting.getTrailingStopPct() != null 
                    ? setting.getTrailingStopPct().negate()  // 양수를 음수로
                    : new BigDecimal("-5");
            
            // ⭐⭐⭐ [수정] 트레일링 스톱 활성화 최소 수익률: 목표 수익률의 50% 또는 최소 1% ⭐⭐⭐
            BigDecimal minProfitForTrailing = setting.getSellTargetPct()
                    .multiply(new BigDecimal("0.5"))
                    .max(new BigDecimal("1")); // 최소 1% 수익 확보 후 트레일링 스톱 활성화
            
            // ⭐⭐⭐ [수정] profitRate > 0 → profitRate >= minProfitForTrailing ⭐⭐⭐
            if (dropFromHigh.compareTo(trailingPct) <= 0 && profitRate.compareTo(minProfitForTrailing) >= 0) {
                return TradingSignalDTO.builder()
                        .market(market)
                        .signalType(SignalType.TRAILING_STOP)
                        .strength(SignalStrength.MODERATE)
                        .detectedAt(LocalDateTime.now())
                        .currentPrice(currentPrice)
                        .reason(String.format("트레일링 스톱: 최고가 대비 %.2f%% 하락 (수익률: %.2f%%)", dropFromHigh, profitRate))
                        .conditionsMet(1)
                        .totalConditions(1)
                        .build();
            }
        }
        
        return createHoldSignal(market, String.format("현재 수익률: %.2f%%", profitRate));
    }

    /**
     * 설정된 기간에 맞는 이동평균선 반환
     */
    private BigDecimal getMAByPeriod(IndicatorResultDTO indicators, int period) {
        return switch (period) {
            case 7 -> indicators.getMa7();
            case 14 -> indicators.getMa14();
            case 30 -> indicators.getMa30();
            default -> indicators.getMa20();
        };
    }

    /**
     * 이동평균선 대비 하락률 계산
     */
    private BigDecimal calculateDropRate(BigDecimal currentPrice, BigDecimal ma) {
        if (ma.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(ma)
                .divide(ma, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 신호 강도 결정
     */
    private SignalStrength determineStrength(int conditionsMet, int totalConditions) {
        double ratio = (double) conditionsMet / totalConditions;
        if (ratio >= 0.75) return SignalStrength.STRONG;
        if (ratio >= 0.5) return SignalStrength.MODERATE;
        return SignalStrength.WEAK;
    }

    /**
     * 대기 신호 생성
     */
    private TradingSignalDTO createHoldSignal(String market, String reason) {
        return TradingSignalDTO.builder()
                .market(market)
                .signalType(SignalType.HOLD)
                .strength(SignalStrength.WEAK)
                .detectedAt(LocalDateTime.now())
                .reason(reason)
                .conditionsMet(0)
                .totalConditions(0)
                .build();
    }
}