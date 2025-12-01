package com.cryptotrading.dto.notification;

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
public class DailyReportDTO {
    
    private String userId;
    private LocalDate reportDate;
    
    // 거래 요약
    private int buyCount;
    private int sellCount;
    private BigDecimal totalBuyAmount;
    private BigDecimal totalSellAmount;
    
    // 수익 요약
    private BigDecimal realizedProfit;      // 실현 손익
    private BigDecimal unrealizedProfit;    // 평가 손익
    private BigDecimal totalProfit;         // 총 손익
    private BigDecimal profitRate;          // 수익률 (%)
    
    // 보유 현황
    private int holdingCount;
    private BigDecimal totalHoldingValue;
    private BigDecimal totalInvestment;
    
    // 코인별 상세
    private List<CoinSummary> coinSummaries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoinSummary {
        private String coinSymbol;
        private int holdingCount;
        private BigDecimal totalQuantity;
        private BigDecimal averagePrice;
        private BigDecimal currentPrice;
        private BigDecimal profitLoss;
        private BigDecimal profitRate;
    }
}