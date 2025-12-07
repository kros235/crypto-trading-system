package com.cryptotrading.service;

import com.cryptotrading.dto.indicator.IndicatorResultDTO;
import com.cryptotrading.dto.upbit.UpbitCandleDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalIndicatorService {

    private final UpbitApiService upbitApiService;
    
    private static final int SCALE = 8;  // 소수점 자릿수
    
    // ★★★ 기본값 (상수 → 기본 파라미터용) ★★★
    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final int DEFAULT_RSI_BUY_THRESHOLD = 30;
    private static final int DEFAULT_RSI_SELL_THRESHOLD = 70;
    private static final int DEFAULT_BB_PERIOD = 20;
    private static final int DEFAULT_BB_MULTIPLIER = 2;
    private static final int DEFAULT_VOLUME_THRESHOLD = 150;

    /**
     * 기본 설정으로 기술적 지표 계산 (기존 메서드 - 호환성 유지)
     */
    public IndicatorResultDTO calculateIndicators(String market) {
        return calculateIndicators(market, 
                DEFAULT_RSI_PERIOD, DEFAULT_RSI_BUY_THRESHOLD, DEFAULT_RSI_SELL_THRESHOLD,
                DEFAULT_BB_PERIOD, DEFAULT_BB_MULTIPLIER, DEFAULT_VOLUME_THRESHOLD);
    }

    /**
     * ★★★ 신규 추가: 사용자 설정으로 기술적 지표 계산 ★★★
     */
    public IndicatorResultDTO calculateIndicators(String market,
                                                   int rsiPeriod, int rsiBuyThreshold, int rsiSellThreshold,
                                                   int bbPeriod, int bbMultiplier, int volumeThreshold) {
        log.info("기술적 지표 계산 시작: {} (RSI:{}, BB:{}/{}배, 거래량:{}%)", 
                market, rsiPeriod, bbPeriod, bbMultiplier, volumeThreshold);
        
        // 필요한 데이터 개수 계산 (가장 긴 기간 + 여유분)
        int requiredDays = Math.max(Math.max(rsiPeriod, bbPeriod), 30) + 10;
        
        // 일봉 데이터 조회
        List<UpbitCandleDTO> candles = upbitApiService.getDayCandles(market, requiredDays);
        
        if (candles == null || candles.size() < bbPeriod) {
            log.warn("일봉 데이터 부족: {} ({}개)", market, candles != null ? candles.size() : 0);
            return null;
        }
        
        // 현재가 조회
        List<UpbitTickerDTO> tickers = upbitApiService.getTicker(List.of(market));
        UpbitTickerDTO ticker = tickers.isEmpty() ? null : tickers.get(0);
        
        BigDecimal currentPrice = ticker != null ? ticker.getTradePrice() : candles.get(0).getTradePrice();
        BigDecimal changeRate = ticker != null ? ticker.getSignedChangeRate() : BigDecimal.ZERO;
        
        // 종가 리스트 추출 (최신순 → 과거순)
        List<BigDecimal> closePrices = candles.stream()
                .map(UpbitCandleDTO::getTradePrice)
                .toList();
        
        // 거래량 리스트 추출
        List<BigDecimal> volumes = candles.stream()
                .map(UpbitCandleDTO::getCandleAccTradeVolume)
                .toList();
        
        // 이동평균선 계산
        BigDecimal ma7 = calculateMA(closePrices, 7);
        BigDecimal ma14 = calculateMA(closePrices, 14);
        BigDecimal ma20 = calculateMA(closePrices, 20);
        BigDecimal ma30 = calculateMA(closePrices, 30);
        
        // ★ RSI 계산 (사용자 설정 기간)
        BigDecimal rsi = calculateRSI(closePrices, rsiPeriod);
        
        // ★ 볼린저 밴드 계산 (사용자 설정 기간/승수)
        BigDecimal[] bollingerBands = calculateBollingerBands(closePrices, bbPeriod, bbMultiplier);
        
        // 평균 거래량 계산 (20일)
        BigDecimal avgVolume = calculateMA(volumes, 20);
        BigDecimal currentVolume = volumes.get(0);
        BigDecimal volumeRatio = avgVolume.compareTo(BigDecimal.ZERO) > 0 
                ? currentVolume.divide(avgVolume, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // ★ 임계값을 BigDecimal로 변환
        BigDecimal rsiBuyThresholdBD = new BigDecimal(rsiBuyThreshold);
        BigDecimal rsiSellThresholdBD = new BigDecimal(rsiSellThreshold);
        BigDecimal volumeThresholdBD = new BigDecimal(volumeThreshold).divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        
        // 결과 생성
        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .market(market)
                .calculatedAt(LocalDateTime.now())
                .currentPrice(currentPrice)
                .changeRate(changeRate)
                .ma7(ma7)
                .ma14(ma14)
                .ma20(ma20)
                .ma30(ma30)
                .rsi14(rsi)  // 필드명은 rsi14 유지 (호환성), 실제 계산은 rsiPeriod 사용
                .bbUpper(bollingerBands[0])
                .bbMiddle(bollingerBands[1])
                .bbLower(bollingerBands[2])
                .avgVolume(avgVolume)
                .currentVolume(currentVolume)
                .volumeRatio(volumeRatio)
                // ★ 신호 플래그 (사용자 설정 임계값 적용)
                .belowMA20(currentPrice.compareTo(ma20) < 0)
                .rsiBuySignal(rsi != null && rsi.compareTo(rsiBuyThresholdBD) <= 0)
                .rsiSellSignal(rsi != null && rsi.compareTo(rsiSellThresholdBD) >= 0)
                .belowBBLower(currentPrice.compareTo(bollingerBands[2]) <= 0)
                .aboveBBUpper(currentPrice.compareTo(bollingerBands[0]) >= 0)
                .highVolume(volumeRatio.compareTo(volumeThresholdBD) >= 0)
                .build();
        
        log.info("기술적 지표 계산 완료: {} - MA20={}, RSI={}, BB하단={}", 
                market, ma20, rsi, bollingerBands[2]);
        
        return result;
    }
    
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
        
        // 가격 변화 계산 (최신 → 과거 순이므로 역방향)
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
            return new BigDecimal("100");  // 손실이 없으면 RSI 100
        }
        
        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        BigDecimal rsi = new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), SCALE, RoundingMode.HALF_UP)
        );
        
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
        
        // 표준편차 계산
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            BigDecimal diff = prices.get(i).subtract(ma);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
        
        BigDecimal deviation = stdDev.multiply(BigDecimal.valueOf(multiplier));
        
        return new BigDecimal[]{
                ma.add(deviation).setScale(SCALE, RoundingMode.HALF_UP),    // 상단
                ma,                                                          // 중간
                ma.subtract(deviation).setScale(SCALE, RoundingMode.HALF_UP) // 하단
        };
    }
}