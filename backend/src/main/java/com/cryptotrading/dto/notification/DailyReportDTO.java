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

    // ⭐⭐⭐ [개선] 코인 활동(거래설정 또는 거래내역) 여부 - false면 이메일/DM에서 코인 섹션 자체를 숨김 ⭐⭐⭐
    // null이면 기존 동작과 동일하게 "표시함"으로 취급 (하위 호환)
    private Boolean hasCoinActivity;

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

    // ⭐⭐⭐ [Day 63 추가] 주식 통합 리포트용 필드 ⭐⭐⭐
    // 왜: 23:50 일일 리포트를 코인+주식 통합 발송하기 위함 (코인 전용 발송 시엔 전부 null 유지, 기존 동작 영향 없음)
    private Integer stockBuyCount;
    private Integer stockSellCount;
    private BigDecimal stockTotalBuyAmount;
    private BigDecimal stockTotalSellAmount;
    private BigDecimal stockRealizedProfit;
    private BigDecimal stockUnrealizedProfit;
    private BigDecimal stockTotalProfit;
    private BigDecimal stockProfitRate;
    private Integer stockHoldingCount;
    private BigDecimal stockTotalHoldingValue;
    private BigDecimal stockTotalInvestment;
    private List<StockSummary> stockSummaries;
    
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

    // ⭐⭐⭐ [Day 63 추가] 주식용 종목 요약 (CoinSummary와 동일 구조, stockCode/stockName만 차이) ⭐⭐⭐
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSummary {
        private String stockCode;
        private String stockName;
        private String etfType;  // ⭐ [Day 63 개선] LEVERAGE/INVERSE/NORMAL 등 - 보유 종목 테이블에 "구분" 컬럼으로 표시
        private int holdingCount;
        private BigDecimal totalQuantity;
        private BigDecimal averagePrice;
        private BigDecimal currentPrice;
        private BigDecimal profitLoss;
        private BigDecimal profitRate;
    }
}