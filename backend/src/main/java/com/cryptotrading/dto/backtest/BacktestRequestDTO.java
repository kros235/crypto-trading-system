package com.cryptotrading.dto.backtest;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestRequestDTO {
    
    @NotEmpty(message = "거래할 코인을 선택해주세요")
    private List<String> coinSymbols;
    
    @NotNull(message = "시작일을 입력해주세요")
    private LocalDate startDate;
    
    @NotNull(message = "종료일을 입력해주세요")
    private LocalDate endDate;
    
    @NotNull(message = "초기 투자금을 입력해주세요")
    @DecimalMin(value = "100000", message = "최소 10만원 이상 입력해주세요")
    private BigDecimal initialBalance;
    
    // 거래 설정 (선택 - 없으면 기본값 사용)
    @Min(value = 7, message = "이동평균선 기간은 7일 이상")
    @Max(value = 30, message = "이동평균선 기간은 30일 이하")
    @Builder.Default
    private Integer basePeriod = 20;
    
    @DecimalMax(value = "0", message = "매수 기준은 0 이하")
    @DecimalMin(value = "-20", message = "매수 기준은 -20% 이상")
    @Builder.Default
    private BigDecimal buyThresholdPct = new BigDecimal("-3");
    
    @DecimalMin(value = "0.5", message = "목표 수익률은 0.5% 이상")
    @DecimalMax(value = "50", message = "목표 수익률은 50% 이하")
    @Builder.Default
    private BigDecimal sellTargetPct = new BigDecimal("3");
    
    @DecimalMax(value = "0", message = "손절매 기준은 0 이하")
    @DecimalMin(value = "-30", message = "손절매 기준은 -30% 이상")
    @Builder.Default
    private BigDecimal stopLossPct = new BigDecimal("-5");
    
    @Min(value = 1, message = "최소 1건 이상")
    @Max(value = 10, message = "최대 10건 이하")
    @Builder.Default
    private Integer maxHoldingsPerCoin = 3;
    
    @Builder.Default
    private Boolean useTrailingStop = false;
    
    @DecimalMin(value = "1", message = "트레일링 스톱은 1% 이상")
    @DecimalMax(value = "10", message = "트레일링 스톱은 10% 이하")
    @Builder.Default
    private BigDecimal trailingStopPct = new BigDecimal("5");
}