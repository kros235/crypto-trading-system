package com.cryptotrading.dto.indicator;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class IndicatorResultDTO {
    
    private String market;
    private LocalDateTime calculatedAt;
    
    // 현재가 정보
    private BigDecimal currentPrice;
    private BigDecimal changeRate;  // 전일 대비 등락률
    
    // 이동평균선 (Moving Average)
    private BigDecimal ma7;   // 7일 이동평균
    private BigDecimal ma14;  // 14일 이동평균
    private BigDecimal ma20;  // 20일 이동평균
    private BigDecimal ma30;  // 30일 이동평균
    
    // RSI (Relative Strength Index)
    private BigDecimal rsi14;  // 14일 RSI
    
    // 볼린저 밴드 (Bollinger Bands)
    private BigDecimal bbUpper;   // 상단 밴드
    private BigDecimal bbMiddle;  // 중간 밴드 (20일 MA)
    private BigDecimal bbLower;   // 하단 밴드
    
    // 거래량 정보
    private BigDecimal avgVolume;     // 평균 거래량
    private BigDecimal currentVolume; // 현재 거래량
    private BigDecimal volumeRatio;   // 거래량 비율 (현재/평균)
    
    // 매매 신호 판단용 플래그
    private boolean belowMA20;        // 20일선 아래 여부
    private boolean rsiBuySignal;     // RSI 매수 신호 (30 이하)
    private boolean rsiSellSignal;    // RSI 매도 신호 (70 이상)
    private boolean belowBBLower;     // 볼린저 하단 접촉
    private boolean aboveBBUpper;     // 볼린저 상단 접촉
    private boolean highVolume;       // 거래량 급증 (150% 이상)
}