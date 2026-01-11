package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 코인별 수익 분석 DTO
 * Day 31: 기간별/코인별 수익 분석 기능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinProfitDTO {
    
    private String coinSymbol;       // 코인 심볼 (예: KRW-BTC)
    private String coinName;         // 코인 한글명 (예: 비트코인)
    
    // 수익 정보
    private BigDecimal totalProfit;  // 총 실현 수익
    private Double profitPct;        // 수익률
    
    // 거래 통계
    private Integer totalTradeCount; // 총 거래 건수
    private Integer winCount;        // 익절 건수
    private Integer loseCount;       // 손절 건수
    private Double winRate;          // 승률
    
    // 금액 정보
    private BigDecimal totalBuyAmount;   // 총 매수 금액
    private BigDecimal totalSellAmount;  // 총 매도 금액
    private BigDecimal avgBuyPrice;      // 평균 매수가
    private BigDecimal avgSellPrice;     // 평균 매도가
    
    // 최고/최저 거래
    private BigDecimal maxProfit;        // 최대 수익 거래
    private BigDecimal maxLoss;          // 최대 손실 거래
    
    // 현재 보유 정보
    private Integer currentHoldingCount; // 현재 보유 건수
    private BigDecimal currentHoldingAmount; // 현재 보유 금액
    private BigDecimal unrealizedProfit; // 미실현 손익
    
    // 최근 거래 시간
    private LocalDateTime lastTradeAt;
}