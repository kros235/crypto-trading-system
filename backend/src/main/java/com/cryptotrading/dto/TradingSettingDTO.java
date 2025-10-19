package com.cryptotrading.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSettingDTO {
    
    private Long id;
    
    @NotEmpty(message = "거래할 코인을 최소 1개 이상 선택해주세요")
    private List<String> coinSymbols;
    
    @Min(value = 7, message = "기준 기간은 최소 7일 이상이어야 합니다")
    private Integer basePeriod;
    
    @DecimalMin(value = "-20.00", message = "매수 기준은 -20% 이상이어야 합니다")
    @DecimalMax(value = "0.00", message = "매수 기준은 0% 이하여야 합니다")
    private BigDecimal buyThresholdPct;
    
    @DecimalMin(value = "0.00", message = "목표 수익률은 0% 이상이어야 합니다")
    @DecimalMax(value = "50.00", message = "목표 수익률은 50% 이하여야 합니다")
    private BigDecimal sellTargetPct;
    
    @DecimalMin(value = "-30.00", message = "손절매는 -30% 이상이어야 합니다")
    @DecimalMax(value = "0.00", message = "손절매는 0% 이하여야 합니다")
    private BigDecimal stopLossPct;
    
    @Min(value = 1, message = "종목당 최대 보유 건수는 최소 1건 이상이어야 합니다")
    private Integer maxHoldingsPerCoin;
    
    @DecimalMin(value = "10000.00", message = "일일 거래 한도는 최소 10,000원 이상이어야 합니다")
    private BigDecimal dailyLimitAmount;
    
    private Boolean useAiAnalysis;
    private Boolean useTrailingStop;
    
    @DecimalMin(value = "-20.00", message = "트레일링 스톱은 -20% 이상이어야 합니다")
    @DecimalMax(value = "0.00", message = "트레일링 스톱은 0% 이하여야 합니다")
    private BigDecimal trailingStopPct;
}