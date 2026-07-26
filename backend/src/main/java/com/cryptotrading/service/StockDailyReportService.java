package com.cryptotrading.service;

import com.cryptotrading.dto.notification.DailyReportDTO;
import com.cryptotrading.dto.notification.DailyReportDTO.StockSummary;
import com.cryptotrading.dto.stock.StockPriceDTO;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.entity.TransactionType;
import com.cryptotrading.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 주식 일일 리포트 서비스
 * Day 63: Phase 1 DailyReportService 1:1 재사용 구조
 *
 * ⭐ 코인+주식 통합 리포트 발송을 위한 서비스.
 *    생성한 결과(DailyReportDTO)는 stock* 로 시작하는 필드만 채워서 반환하며,
 *    TradingScheduler.sendDailyReport()에서 코인 리포트 객체에 병합(setter)하여 사용한다.
 *    (별도 발송 채널을 새로 만들지 않고 기존 23:50 스케줄 1건에 합류)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockDailyReportService {

    private final StockTransactionRepository stockTransactionRepository;
    private final StockInfoService stockInfoService;
    // ⭐⭐⭐ [Day 63 버그 수정] 종목명 실제 조회용 (기존엔 stockName에 stockCode를 그대로 넣는 버그가 있었음) ⭐⭐⭐
    private final com.cryptotrading.repository.StockInfoRepository stockInfoRepository;

    /**
     * 특정 사용자의 주식 일일 리포트 생성 (stock* 필드만 채워진 DailyReportDTO 반환)
     */
    public DailyReportDTO generateStockDailyReport(String userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<StockTransaction> todayBuys = stockTransactionRepository
                .findByUserIdAndCreatedAtAfter(userId, startOfDay)
                .stream()
                .filter(t -> t.getType() == TransactionType.BUY && !t.getCreatedAt().isAfter(endOfDay))
                .toList();

        List<StockTransaction> todaySells = stockTransactionRepository
                .findSoldTransactionsByDateRange(userId, startOfDay, endOfDay);

        List<StockTransaction> holdings = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);

        Map<String, BigDecimal> currentPrices = getCurrentPrices(userId, holdings);

        BigDecimal totalBuyAmount = todayBuys.stream()
                .map(StockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellAmount = todaySells.stream()
                .map(t -> t.getSoldPrice() != null && t.getQuantity() != null
                        ? t.getSoldPrice().multiply(BigDecimal.valueOf(t.getQuantity()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal realizedProfit = todaySells.stream()
                .map(t -> t.getProfitLoss() != null ? t.getProfitLoss() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal totalHoldingValue = BigDecimal.ZERO;
        Map<String, List<StockTransaction>> holdingsByCode = holdings.stream()
                .collect(Collectors.groupingBy(StockTransaction::getStockCode));

        List<StockSummary> stockSummaries = new ArrayList<>();

        for (Map.Entry<String, List<StockTransaction>> entry : holdingsByCode.entrySet()) {
            String code = entry.getKey();
            List<StockTransaction> codeHoldings = entry.getValue();
            BigDecimal currentPrice = currentPrices.getOrDefault(code, BigDecimal.ZERO);

            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;

            for (StockTransaction t : codeHoldings) {
                totalQuantity = totalQuantity.add(BigDecimal.valueOf(t.getQuantity()));
                totalCost = totalCost.add(t.getTotalAmount());
            }

            BigDecimal avgPrice = totalQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal currentValue = totalQuantity.multiply(currentPrice);
            BigDecimal profitLoss = currentValue.subtract(totalCost);
            BigDecimal profitRate = totalCost.compareTo(BigDecimal.ZERO) > 0
                    ? profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            totalInvestment = totalInvestment.add(totalCost);
            totalHoldingValue = totalHoldingValue.add(currentValue);

            // ⭐⭐⭐ [Day 63 버그 수정] 이전엔 .stockName(code)로 코드를 그대로 넣었음 → 실제 종목명/ETF구분 조회로 교체 ⭐⭐⭐
            // ⭐⭐⭐ [빌드 오류 수정] StockInfo.getEtfType()은 String이 아니라 EtfType enum이라 .name()으로 변환 필요 ⭐⭐⭐
            com.cryptotrading.entity.StockInfo stockInfo = stockInfoRepository.findById(code).orElse(null);
            String resolvedStockName = stockInfo != null ? stockInfo.getStockName() : code;
            String etfType = (stockInfo != null && stockInfo.getEtfType() != null) ? stockInfo.getEtfType().name() : null;

            stockSummaries.add(StockSummary.builder()
                    .stockCode(code)
                    .stockName(resolvedStockName)
                    .etfType(etfType)
                    .holdingCount(codeHoldings.size())
                    .totalQuantity(totalQuantity)
                    .averagePrice(avgPrice)
                    .currentPrice(currentPrice)
                    .profitLoss(profitLoss)
                    .profitRate(profitRate)
                    .build());
        }

        BigDecimal unrealizedProfit = totalHoldingValue.subtract(totalInvestment);
        BigDecimal totalProfit = realizedProfit.add(unrealizedProfit);
        BigDecimal profitRate = totalInvestment.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalInvestment, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        return DailyReportDTO.builder()
                .userId(userId)
                .reportDate(today)
                .stockBuyCount(todayBuys.size())
                .stockSellCount(todaySells.size())
                .stockTotalBuyAmount(totalBuyAmount)
                .stockTotalSellAmount(totalSellAmount)
                .stockRealizedProfit(realizedProfit)
                .stockUnrealizedProfit(unrealizedProfit)
                .stockTotalProfit(totalProfit)
                .stockProfitRate(profitRate)
                .stockHoldingCount(holdingsByCode.size())
                .stockTotalHoldingValue(totalHoldingValue)
                .stockTotalInvestment(totalInvestment)
                .stockSummaries(stockSummaries)
                .build();
    }

    private Map<String, BigDecimal> getCurrentPrices(String userId, List<StockTransaction> holdings) {
        Map<String, BigDecimal> prices = new HashMap<>();

        List<String> codes = holdings.stream()
                .map(StockTransaction::getStockCode)
                .distinct()
                .toList();

        if (codes.isEmpty()) return prices;

        try {
            List<StockPriceDTO> priceList = stockInfoService.getPricesForStocks(userId, codes);
            for (StockPriceDTO p : priceList) {
                if (p.getCurrentPrice() != null) {
                    prices.put(p.getStockCode(), p.getCurrentPrice());
                }
            }
        } catch (Exception e) {
            log.error("[주식] 현재가 조회 실패: {}", e.getMessage());
        }

        return prices;
    }
}