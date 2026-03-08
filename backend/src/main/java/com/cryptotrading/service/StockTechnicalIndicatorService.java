package com.cryptotrading.service;

import com.cryptotrading.dto.indicator.IndicatorResultDTO;
import com.cryptotrading.dto.kis.KisQuoteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주식/ETF 기술적 지표 계산 서비스
 * Phase 2 Day 53: Phase 1 TechnicalIndicatorService 재사용
 * 
 * [Phase 1과의 차이점]
 * - 데이터 소스: Upbit API → KIS API
 * - 기본 RSI 임계값: 30/70 → 35/65 (주식 변동성 반영)
 * - 기본 거래량 기준: 150% → 120% (ETF 특성 반영)
 * - 수학 계산 로직: 100% 동일 (MA, RSI, BB)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockTechnicalIndicatorService {

    private final KisApiService kisApiService;

    private static final int SCALE = 8;

    // ★ 주식/ETF 기본값 (Phase 1과 다른 값)
    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final int DEFAULT_RSI_BUY_THRESHOLD = 35;   // 코인: 30
    private static final int DEFAULT_RSI_SELL_THRESHOLD = 65;  // 코인: 70
    private static final int DEFAULT_BB_PERIOD = 20;
    private static final int DEFAULT_BB_MULTIPLIER = 2;
    private static final int DEFAULT_VOLUME_THRESHOLD = 120;   // 코인: 150

    /**
     * 기본 설정으로 기술적 지표 계산
     */
    public IndicatorResultDTO calculateIndicators(String userId, String stockCode) {
        return calculateIndicators(userId, stockCode,
                DEFAULT_RSI_PERIOD, DEFAULT_RSI_BUY_THRESHOLD, DEFAULT_RSI_SELL_THRESHOLD,
                DEFAULT_BB_PERIOD, DEFAULT_BB_MULTIPLIER, DEFAULT_VOLUME_THRESHOLD);
    }

    /**
     * 사용자 설정으로 기술적 지표 계산
     * 
     * @param userId 사용자 ID (KIS API 인증용)
     * @param stockCode 종목코드 (예: "409820")
     */
    public IndicatorResultDTO calculateIndicators(String userId, String stockCode,
                                                   int rsiPeriod, int rsiBuyThreshold, int rsiSellThreshold,
                                                   int bbPeriod, int bbMultiplier, int volumeThreshold) {
        log.info("[주식 지표] 계산 시작: {} (RSI:{}, BB:{}/{}배, 거래량:{}%)",
                stockCode, rsiPeriod, bbPeriod, bbMultiplier, volumeThreshold);

        // 필요한 데이터 개수 계산 (가장 긴 기간 + 여유분)
        int requiredDays = Math.max(Math.max(rsiPeriod, bbPeriod), 30) + 10;

        // ★ KIS API로 일봉 데이터 조회 (Phase 1: Upbit API)
        List<KisQuoteDTO.DailyCandle> candles = kisApiService.getDailyCandles(
                userId, stockCode, "D", requiredDays);

        if (candles == null || candles.size() < bbPeriod) {
            log.warn("[주식 지표] 일봉 데이터 부족: {} ({}개)", stockCode,
                    candles != null ? candles.size() : 0);
            return null;
        }

        // ★ KIS API로 현재가 조회 (Phase 1: Upbit Ticker API)
        KisQuoteDTO.CurrentPrice currentPriceData = kisApiService.getCurrentPrice(userId, stockCode);

        BigDecimal currentPrice;
        BigDecimal changeRate;
        BigDecimal currentVolume;

        if (currentPriceData != null) {
            currentPrice = currentPriceData.getCurrentPriceDecimal();
            changeRate = currentPriceData.getChangeRate() != null
                    ? new BigDecimal(currentPriceData.getChangeRate()).divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            currentVolume = currentPriceData.getVolumeDecimal();
        } else {
            // 현재가 조회 실패 시 최신 일봉 데이터 사용
            currentPrice = candles.get(0).getClosePriceDecimal();
            changeRate = BigDecimal.ZERO;
            currentVolume = candles.get(0).getVolumeDecimal();
        }

        // 종가 리스트 추출 (최신순 → 과거순, KIS API 일봉은 최신순 정렬)
        List<BigDecimal> closePrices = candles.stream()
                .map(KisQuoteDTO.DailyCandle::getClosePriceDecimal)
                .collect(Collectors.toList());

        // 거래량 리스트 추출
        List<BigDecimal> volumes = candles.stream()
                .map(KisQuoteDTO.DailyCandle::getVolumeDecimal)
                .collect(Collectors.toList());

        // ===== 이하 수학 계산 로직은 Phase 1과 100% 동일 =====

        // 이동평균선 계산
        BigDecimal ma7 = calculateMA(closePrices, 7);
        BigDecimal ma14 = calculateMA(closePrices, 14);
        BigDecimal ma20 = calculateMA(closePrices, 20);
        BigDecimal ma30 = calculateMA(closePrices, 30);

        // RSI 계산 (사용자 설정 기간)
        BigDecimal rsi = calculateRSI(closePrices, rsiPeriod);

        // 볼린저 밴드 계산 (사용자 설정 기간/승수)
        BigDecimal[] bollingerBands = calculateBollingerBands(closePrices, bbPeriod, bbMultiplier);

        // 평균 거래량 계산 (20일)
        BigDecimal avgVolume = calculateMA(volumes, 20);
        BigDecimal volumeRatio = avgVolume.compareTo(BigDecimal.ZERO) > 0
                ? currentVolume.divide(avgVolume, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 임계값을 BigDecimal로 변환
        BigDecimal rsiBuyThresholdBD = new BigDecimal(rsiBuyThreshold);
        BigDecimal rsiSellThresholdBD = new BigDecimal(rsiSellThreshold);
        BigDecimal volumeThresholdBD = new BigDecimal(volumeThreshold)
                .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);

        // 결과 생성 (IndicatorResultDTO 재사용 - market 필드에 stockCode 저장)
        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .market(stockCode)  // 주식: stockCode 사용
                .calculatedAt(LocalDateTime.now())
                .currentPrice(currentPrice)
                .changeRate(changeRate)
                .ma7(ma7)
                .ma14(ma14)
                .ma20(ma20)
                .ma30(ma30)
                .rsi14(rsi)
                .bbUpper(bollingerBands[0])
                .bbMiddle(bollingerBands[1])
                .bbLower(bollingerBands[2])
                .avgVolume(avgVolume)
                .currentVolume(currentVolume)
                .volumeRatio(volumeRatio)
                .belowMA20(currentPrice.compareTo(ma20) < 0)
                .rsiBuySignal(rsi != null && rsi.compareTo(rsiBuyThresholdBD) <= 0)
                .rsiSellSignal(rsi != null && rsi.compareTo(rsiSellThresholdBD) >= 0)
                .belowBBLower(currentPrice.compareTo(bollingerBands[2]) <= 0)
                .aboveBBUpper(currentPrice.compareTo(bollingerBands[0]) >= 0)
                .highVolume(volumeRatio.compareTo(volumeThresholdBD) >= 0)
                .build();

        log.info("[주식 지표] 계산 완료: {} - 현재가={}, MA20={}, RSI={}, BB하단={}",
                stockCode, currentPrice, ma20, rsi, bollingerBands[2]);

        return result;
    }

    // ===== 수학 계산 메서드 (Phase 1 TechnicalIndicatorService와 100% 동일) =====

    /**
     * 이동평균선 (Moving Average) 계산
     */
    private BigDecimal calculateMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * RSI (Relative Strength Index) 계산
     */
    private BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) {
            return null;
        }
        BigDecimal gainSum = BigDecimal.ZERO;
        BigDecimal lossSum = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i + 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gainSum = gainSum.add(change);
            } else {
                lossSum = lossSum.add(change.abs());
            }
        }

        BigDecimal avgGain = gainSum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal avgLoss = lossSum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100");
        }

        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        BigDecimal rsi = new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP));

        return rsi.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 볼린저 밴드 계산
     * @return [상단밴드, 중간밴드(MA), 하단밴드]
     */
    private BigDecimal[] calculateBollingerBands(List<BigDecimal> prices, int period, int multiplier) {
        BigDecimal ma = calculateMA(prices, period);

        if (prices.size() < period) {
            return new BigDecimal[]{ma, ma, ma};
        }

        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            BigDecimal diff = prices.get(i).subtract(ma);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }

        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        BigDecimal deviation = stdDev.multiply(BigDecimal.valueOf(multiplier));

        return new BigDecimal[]{
                ma.add(deviation).setScale(SCALE, RoundingMode.HALF_UP),
                ma,
                ma.subtract(deviation).setScale(SCALE, RoundingMode.HALF_UP)
        };
    }
}