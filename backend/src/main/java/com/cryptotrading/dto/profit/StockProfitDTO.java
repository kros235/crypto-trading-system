package com.cryptotrading.dto.profit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 종목별 수익 분석 DTO
 * Day 63: Phase 1 CoinProfitDTO 1:1 재사용 구조 (coinSymbol/coinName → stockCode/stockName)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProfitDTO {

    private String stockCode;
    private String stockName;

    private BigDecimal totalProfit;
    private Double profitPct;

    private Integer totalTradeCount;
    private Integer winCount;
    private Integer loseCount;
    private Double winRate;

    private BigDecimal totalBuyAmount;
    private BigDecimal totalSellAmount;
    private BigDecimal avgBuyPrice;
    private BigDecimal avgSellPrice;

    private BigDecimal maxProfit;
    private BigDecimal maxLoss;

    private Integer currentHoldingCount;
    private BigDecimal currentHoldingAmount;
    private BigDecimal unrealizedProfit; // 프론트에서 현재가 반영하여 계산 (Phase1과 동일 패턴)

    private LocalDateTime lastTradeAt;
}