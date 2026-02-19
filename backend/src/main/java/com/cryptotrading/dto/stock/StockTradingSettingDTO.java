package com.cryptotrading.dto.stock;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTradingSettingDTO {
    
    @NotEmpty(message = "거래 종목을 1개 이상 선택해주세요")
    private List<String> stockCodes;
    
    @Min(value = 5, message = "이동평균선 기간은 최소 5일입니다")
    private Integer basePeriod;
    
    @DecimalMax(value = "0", message = "매수 기준은 0% 이하여야 합니다")
    private BigDecimal buyThresholdPct;
    
    @DecimalMin(value = "0", message = "목표 수익률은 0% 이상이어야 합니다")
    private BigDecimal sellTargetPct;
    
    @DecimalMax(value = "0", message = "손절매 기준은 0% 이하여야 합니다")
    private BigDecimal stopLossPct;
    
    @Min(value = 1, message = "종목당 최소 1건 이상 보유해야 합니다")
    private Integer maxHoldingsPerStock;
    
    private BigDecimal dailyLimitAmount;
    private Boolean useTrailingStop;
    private BigDecimal trailingStopPct;
    
    // 기술적 지표 설정
    private Integer rsiPeriod;
    private Integer rsiBuyThreshold;
    private Integer rsiSellThreshold;
    private Integer bbPeriod;
    private Integer bbMultiplier;
    private Integer volumeThreshold;
    
    // 리스크 관리
    private Integer dailyTradeLimitPct;
    private Integer maxPositionPct;
    private Integer dailyStopLossPct;
    private Boolean useMarketTrendFilter;
    private Integer cumulativeLossLimitPct;
    private Integer consecutiveStopLossLimit;
    private BigDecimal fixedBuyAmount;
    private Boolean useDailyLimitRecovery;
    private Boolean useRoundRobin;
    
    // Phase 2 전용
    private Integer maxHoldingDays;      // 최대 보유일수 (레버리지 decay 방지)
    
    // KIS API 키 (등록 시에만 사용, 조회 시에는 빈값 반환)
    private String kisAppKey;
    private String kisAppSecret;
    private String kisAccountNo;
    private Boolean kisMockMode;
    
    // 조회 전용 (API 키 등록 여부)
    private Boolean hasKisApiKey;
}