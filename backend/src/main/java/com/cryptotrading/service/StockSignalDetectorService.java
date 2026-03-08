package com.cryptotrading.service;

import com.cryptotrading.dto.bot.TradingSignalDTO;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalStrength;
import com.cryptotrading.dto.bot.TradingSignalDTO.SignalType;
import com.cryptotrading.dto.indicator.IndicatorResultDTO;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주식/ETF 거래 신호 감지 서비스
 * Phase 2 Day 53: Phase 1 SignalDetectorService 재사용
 * 
 * [Phase 1과의 차이점]
 * - Entity: TradingSetting → StockTradingSetting
 * - 수수료율: 0.05% → 0.015% (국내 주식)
 * - 보유기간 체크: 레버리지 ETF 20일 초과 시 강제 매도 신호 추가
 * - AI 뉴스 분석 가중치: 미적용 (주식에는 해당 없음)
 * - bbMultiplier/volumeThreshold: BigDecimal 타입 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockSignalDetectorService {

    private final StockTechnicalIndicatorService indicatorService;
    private final StockTransactionRepository stockTransactionRepository;

    private static final int SCALE = 8;

    // ★ 주식 수수료율: 0.015% (Phase 1 코인: 0.05%)
    private static final BigDecimal FEE_RATE = new BigDecimal("0.00015");

    /**
     * 매수 신호 감지
     * Phase 1과 동일한 4가지 조건: MA 하락률 + RSI + 볼린저밴드 + 거래량
     * (AI 뉴스 분석 가중치는 주식에 미적용)
     */
    public TradingSignalDTO detectBuySignal(String stockCode, StockTradingSetting setting, String userId) {
        log.debug("[주식 신호] 매수 신호 감지 시작: {}", stockCode);

        // ★ bbMultiplier, volumeThreshold가 BigDecimal 타입 (Phase 1은 Integer)
        int bbMultiplier = setting.getBbMultiplier() != null
                ? setting.getBbMultiplier().intValue() : 2;
        int volumeThreshold = setting.getVolumeThreshold() != null
                ? setting.getVolumeThreshold().intValue() : 120;

        IndicatorResultDTO indicators = indicatorService.calculateIndicators(
                userId, stockCode,
                setting.getRsiPeriod() != null ? setting.getRsiPeriod() : 14,
                setting.getRsiBuyThreshold() != null ? setting.getRsiBuyThreshold() : 35,
                setting.getRsiSellThreshold() != null ? setting.getRsiSellThreshold() : 65,
                setting.getBbPeriod() != null ? setting.getBbPeriod() : 20,
                bbMultiplier,
                volumeThreshold);

        if (indicators == null) {
            return createHoldSignal(stockCode, "기술적 지표 계산 실패 (데이터 부족)");
        }

        // 매수 조건 검사 (Phase 1과 동일한 로직)
        int conditionsMet = 0;
        int totalConditions = 4;
        List<String> reasons = new ArrayList<>();

        // 설정된 기간에 맞는 이동평균선 가져오기
        int basePeriod = setting.getBasePeriod() != null ? setting.getBasePeriod() : 20;
        BigDecimal ma = getMAByPeriod(indicators, basePeriod);

        // 이동평균선 대비 하락률 계산
        BigDecimal dropRate = calculateDropRate(indicators.getCurrentPrice(), ma);
        BigDecimal threshold = setting.getBuyThresholdPct() != null
                ? setting.getBuyThresholdPct() : new BigDecimal("-3.00");

        // 조건 1: 이동평균선 대비 하락률 (AI 가중치 미적용)
        if (dropRate.compareTo(threshold) <= 0) {
            conditionsMet++;
            reasons.add(String.format("MA%d 대비 %.2f%% 하락 (기준: %.2f%%)",
                    basePeriod, dropRate, threshold));
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

            // 목표가 계산 (수수료 반영)
            BigDecimal totalFeeRate = FEE_RATE.multiply(new BigDecimal("2"));
            BigDecimal adjustedTargetPct = setting.getSellTargetPct().add(
                    totalFeeRate.multiply(new BigDecimal("100")));
            BigDecimal targetPrice = indicators.getCurrentPrice()
                    .multiply(BigDecimal.ONE
                            .add(adjustedTargetPct.divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP)));

            // 손절가 계산
            BigDecimal stopLossPrice = indicators.getCurrentPrice()
                    .multiply(BigDecimal.ONE
                            .add(setting.getStopLossPct().divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP)));

            return TradingSignalDTO.builder()
                    .market(stockCode)
                    .signalType(SignalType.BUY)
                    .strength(strength)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(indicators.getCurrentPrice())
                    .targetPrice(targetPrice)
                    .stopLossPrice(stopLossPrice)
                    .reason(String.join(", ", reasons))
                    .conditionsMet(conditionsMet)
                    .totalConditions(totalConditions)
                    .dropRate(dropRate)
                    .build();
        }

        return createHoldSignal(stockCode, "매수 조건 미충족");
    }

    /**
     * 매도 신호 감지 (보유 중인 거래에 대해)
     * Phase 1 대비 추가: 보유기간 초과 강제 매도 (레버리지 ETF decay 방지)
     */
    public TradingSignalDTO detectSellSignal(StockTransaction transaction, StockTradingSetting setting, String userId) {
        String stockCode = transaction.getStockCode();
        log.debug("[주식 신호] 매도 신호 감지 시작: {} (거래ID: {})", stockCode, transaction.getTransactionId());

        int bbMultiplier = setting.getBbMultiplier() != null
                ? setting.getBbMultiplier().intValue() : 2;
        int volumeThreshold = setting.getVolumeThreshold() != null
                ? setting.getVolumeThreshold().intValue() : 120;

        IndicatorResultDTO indicators = indicatorService.calculateIndicators(
                userId, stockCode,
                setting.getRsiPeriod() != null ? setting.getRsiPeriod() : 14,
                setting.getRsiBuyThreshold() != null ? setting.getRsiBuyThreshold() : 35,
                setting.getRsiSellThreshold() != null ? setting.getRsiSellThreshold() : 65,
                setting.getBbPeriod() != null ? setting.getBbPeriod() : 20,
                bbMultiplier,
                volumeThreshold);

        if (indicators == null) {
            return createHoldSignal(stockCode, "기술적 지표 계산 실패");
        }

        BigDecimal currentPrice = indicators.getCurrentPrice();
        BigDecimal buyPrice = transaction.getPrice();

        // 수수료 반영 수익률 계산
        BigDecimal buyAmount = buyPrice.multiply(BigDecimal.valueOf(transaction.getQuantity()));
        BigDecimal buyFee = buyAmount.multiply(FEE_RATE);
        BigDecimal sellAmount = currentPrice.multiply(BigDecimal.valueOf(transaction.getQuantity()));
        BigDecimal sellFee = sellAmount.multiply(FEE_RATE);
        BigDecimal netSellAmount = sellAmount.subtract(sellFee);
        BigDecimal netBuyAmount = buyAmount.add(buyFee);

        BigDecimal priceChangeRate = currentPrice.subtract(buyPrice)
                .divide(buyPrice, SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        BigDecimal netProfitRate = netBuyAmount.compareTo(BigDecimal.ZERO) > 0
                ? netSellAmount.subtract(netBuyAmount)
                        .divide(netBuyAmount, SCALE, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // ★★★ [Phase 2 추가] 조건 0: 보유기간 초과 강제 매도 (레버리지 ETF decay 방지) ★★★
        Integer maxHoldingDays = setting.getMaxHoldingDays() != null ? setting.getMaxHoldingDays() : 20;
        Integer holdingDays = transaction.getHoldingDays() != null ? transaction.getHoldingDays() : 0;

        if (holdingDays >= maxHoldingDays) {
            return TradingSignalDTO.builder()
                    .market(stockCode)
                    .signalType(SignalType.SELL)
                    .strength(SignalStrength.STRONG)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(currentPrice)
                    .reason(String.format("보유기간 초과: %d일 (최대: %d일, 레버리지 decay 방지), 수익률: %.2f%%",
                            holdingDays, maxHoldingDays, netProfitRate))
                    .conditionsMet(1)
                    .totalConditions(1)
                    .build();
        }

        // 조건 1: 목표 수익률 도달 (수수료 포함)
        if (netProfitRate.compareTo(setting.getSellTargetPct()) >= 0) {
            return TradingSignalDTO.builder()
                    .market(stockCode)
                    .signalType(SignalType.SELL)
                    .strength(SignalStrength.STRONG)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(currentPrice)
                    .reason(String.format("목표 수익률 도달: %.2f%% (목표: %.2f%%, 수수료 반영)",
                            netProfitRate, setting.getSellTargetPct()))
                    .conditionsMet(1)
                    .totalConditions(1)
                    .build();
        }

        // 조건 2: 손절매
        if (priceChangeRate.compareTo(setting.getStopLossPct()) <= 0) {
            return TradingSignalDTO.builder()
                    .market(stockCode)
                    .signalType(SignalType.STOP_LOSS)
                    .strength(SignalStrength.STRONG)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(currentPrice)
                    .reason(String.format("손절매 도달: %.2f%% (기준: %.2f%%)",
                            priceChangeRate, setting.getStopLossPct()))
                    .conditionsMet(1)
                    .totalConditions(1)
                    .build();
        }

        // 조건 3: 트레일링 스톱 (옵션)
        if (Boolean.TRUE.equals(setting.getUseTrailingStop()) && transaction.getHighestPrice() != null) {
            BigDecimal highestPrice = transaction.getHighestPrice();
            BigDecimal dropFromHigh = currentPrice.subtract(highestPrice)
                    .divide(highestPrice, SCALE, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            BigDecimal trailingPct = setting.getTrailingStopPct() != null
                    ? setting.getTrailingStopPct().negate()
                    : new BigDecimal("-2.5");

            // 트레일링 스톱 활성화 최소 수익률: 목표 수익률의 50% 또는 최소 1%
            BigDecimal minProfitForTrailing = setting.getSellTargetPct()
                    .multiply(new BigDecimal("0.5"))
                    .max(new BigDecimal("1"));

            if (dropFromHigh.compareTo(trailingPct) <= 0 && netProfitRate.compareTo(minProfitForTrailing) >= 0) {
                return TradingSignalDTO.builder()
                        .market(stockCode)
                        .signalType(SignalType.TRAILING_STOP)
                        .strength(SignalStrength.MODERATE)
                        .detectedAt(LocalDateTime.now())
                        .currentPrice(currentPrice)
                        .reason(String.format("트레일링 스톱: 최고가 대비 %.2f%% 하락 (수익률: %.2f%%, 수수료 반영)",
                                dropFromHigh, netProfitRate))
                        .conditionsMet(1)
                        .totalConditions(1)
                        .build();
            }
        }

        // 조건 4: RSI 과매수 신호 (수익 중일 때만)
        if (indicators.isRsiSellSignal() && netProfitRate.compareTo(BigDecimal.ZERO) > 0) {
            return TradingSignalDTO.builder()
                    .market(stockCode)
                    .signalType(SignalType.SELL)
                    .strength(SignalStrength.MODERATE)
                    .detectedAt(LocalDateTime.now())
                    .currentPrice(currentPrice)
                    .reason(String.format("RSI 과매수: %.2f (임계값: %d), 수익률: %.2f%% (수수료 반영)",
                            indicators.getRsi14(), setting.getRsiSellThreshold(), netProfitRate))
                    .conditionsMet(1)
                    .totalConditions(1)
                    .build();
        }

        return createHoldSignal(stockCode, String.format("현재 수익률: %.2f%% (수수료 반영), 보유일: %d/%d일",
                netProfitRate, holdingDays, maxHoldingDays));
    }

    // ===== Private Helper Methods (Phase 1과 동일) =====

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
    private TradingSignalDTO createHoldSignal(String stockCode, String reason) {
        return TradingSignalDTO.builder()
                .market(stockCode)
                .signalType(SignalType.HOLD)
                .strength(SignalStrength.WEAK)
                .detectedAt(LocalDateTime.now())
                .reason(reason)
                .conditionsMet(0)
                .totalConditions(0)
                .build();
    }
}