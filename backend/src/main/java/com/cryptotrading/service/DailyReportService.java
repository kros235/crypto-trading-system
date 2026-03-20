package com.cryptotrading.service;

import com.cryptotrading.dto.notification.DailyReportDTO;
import com.cryptotrading.dto.notification.DailyReportDTO.CoinSummary;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UpbitApiService upbitApiService;
    private final NotificationService notificationService;

    /**
     * 특정 사용자의 일일 리포트 생성
     */
    public DailyReportDTO generateDailyReport(String userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        // 오늘 거래 내역
        List<Transaction> todayBuys = transactionRepository
                .findByUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay)
                .stream()
                .filter(t -> t.getType() == Transaction.TransactionType.BUY)
                .toList();
        
        List<Transaction> todaySells = transactionRepository
                .findByUserIdAndSoldAtBetween(userId, startOfDay, endOfDay);
        
        // 보유 중인 거래
        List<Transaction> holdings = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);
        
        // 현재가 조회
        Map<String, BigDecimal> currentPrices = getCurrentPrices(holdings);
        
        // 거래 요약 계산
        BigDecimal totalBuyAmount = todayBuys.stream()
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalSellAmount = todaySells.stream()
                .map(t -> t.getSoldPrice().multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 실현 손익 (오늘 매도한 것)
        BigDecimal realizedProfit = todaySells.stream()
                .map(Transaction::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 평가 손익 (보유 중인 것)
        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal totalHoldingValue = BigDecimal.ZERO;
        Map<String, List<Transaction>> holdingsBySymbol = holdings.stream()
                .collect(Collectors.groupingBy(Transaction::getCoinSymbol));
        
        List<CoinSummary> coinSummaries = new ArrayList<>();
        
        for (Map.Entry<String, List<Transaction>> entry : holdingsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<Transaction> coinHoldings = entry.getValue();
            BigDecimal currentPrice = currentPrices.getOrDefault(symbol, BigDecimal.ZERO);
            
            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            
            for (Transaction t : coinHoldings) {
                totalQuantity = totalQuantity.add(t.getQuantity());
                totalCost = totalCost.add(t.getTotalAmount());
            }
            
            BigDecimal avgPrice = totalQuantity.compareTo(BigDecimal.ZERO) > 0 
                    ? totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            BigDecimal currentValue = totalQuantity.multiply(currentPrice);
            BigDecimal profitLoss = currentValue.subtract(totalCost);
            BigDecimal profitRate = totalCost.compareTo(BigDecimal.ZERO) > 0
                    ? profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            
            totalInvestment = totalInvestment.add(totalCost);
            totalHoldingValue = totalHoldingValue.add(currentValue);
            
            coinSummaries.add(CoinSummary.builder()
                    .coinSymbol(symbol)
                    .holdingCount(coinHoldings.size())
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
                .buyCount(todayBuys.size())
                .sellCount(todaySells.size())
                .totalBuyAmount(totalBuyAmount)
                .totalSellAmount(totalSellAmount)
                .realizedProfit(realizedProfit)
                .unrealizedProfit(unrealizedProfit)
                .totalProfit(totalProfit)
                .profitRate(profitRate)
                .holdingCount(holdingsBySymbol.size())
                .totalHoldingValue(totalHoldingValue)
                .totalInvestment(totalInvestment)
                .coinSummaries(coinSummaries)
                .build();
    }

    /**
     * 전체 활성 사용자 일일 리포트 발송
     */
    public void sendDailyReportsForAllUsers() {
        List<User> activeUsers = userRepository.findByIsActive(true);    
        
        for (User user : activeUsers) {
            try {
                DailyReportDTO report = generateDailyReport(user.getUserId());
                notificationService.sendDailyReport(report);
                log.info("일일 리포트 발송 완료: {}", user.getUserId());
            } catch (Exception e) {
                log.error("일일 리포트 생성/발송 실패: {} - {}", user.getUserId(), e.getMessage());
            }
        }
    }

     /**
     * ★ 추가: 주간 리포트 생성 (이번 주 월~일)
     */
    public DailyReportDTO generateWeeklyReport(String userId) {
        LocalDate today = LocalDate.now();
        // 이번 주 월요일 ~ 오늘
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return generatePeriodReport(userId, weekStart, today, "주간");
    }

    /**
     * ★ 추가: 월간 리포트 생성 (이번 달 1일~말일)
     */
    public DailyReportDTO generateMonthlyReport(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        return generatePeriodReport(userId, monthStart, monthEnd, "월간");
    }

    /**
     * ★ 추가: 연간 리포트 생성 (올해 1월1일~말일)
     */
    public DailyReportDTO generateYearlyReport(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate yearEnd = today.with(TemporalAdjusters.lastDayOfYear());
        return generatePeriodReport(userId, yearStart, yearEnd, "연간");
    }

    /**
     * ★ 추가: 기간별 리포트 공통 집계 (일간/주간/월간/연간 공통 사용)
     * - 기존 generateDailyReport()와 동일한 집계 로직, 날짜 범위만 다름
     */
    public DailyReportDTO generatePeriodReport(String userId, LocalDate startDate, LocalDate endDate, String periodLabel) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Transaction> periodBuys = transactionRepository
                .findByUserIdAndCreatedAtBetween(userId, start, end)
                .stream()
                .filter(t -> t.getType() == Transaction.TransactionType.BUY)
                .toList();

        List<Transaction> periodSells = transactionRepository
                .findByUserIdAndSoldAtBetween(userId, start, end);

        List<Transaction> holdings = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.HOLDING);

        Map<String, BigDecimal> currentPrices = getCurrentPrices(holdings);

        BigDecimal totalBuyAmount = periodBuys.stream()
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellAmount = periodSells.stream()
                .map(t -> t.getSoldPrice().multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal realizedProfit = periodSells.stream()
                .map(Transaction::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal totalHoldingValue = BigDecimal.ZERO;
        Map<String, List<Transaction>> holdingsBySymbol = holdings.stream()
                .collect(Collectors.groupingBy(Transaction::getCoinSymbol));

        List<CoinSummary> coinSummaries = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : holdingsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<Transaction> coinHoldings = entry.getValue();
            BigDecimal currentPrice = currentPrices.getOrDefault(symbol, BigDecimal.ZERO);

            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;

            for (Transaction t : coinHoldings) {
                totalQuantity = totalQuantity.add(t.getQuantity());
                totalCost = totalCost.add(t.getTotalAmount());
            }

            BigDecimal avgPrice = totalQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal currentValue = totalQuantity.multiply(currentPrice);
            BigDecimal profitLoss = currentValue.subtract(totalCost);
            BigDecimal profitRate = totalCost.compareTo(BigDecimal.ZERO) > 0
                    ? profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            totalInvestment = totalInvestment.add(totalCost);
            totalHoldingValue = totalHoldingValue.add(currentValue);

            coinSummaries.add(CoinSummary.builder()
                    .coinSymbol(symbol)
                    .holdingCount(coinHoldings.size())
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
                .reportDate(endDate)        // 리포트 기준일 = 마지막 날
                .buyCount(periodBuys.size())
                .sellCount(periodSells.size())
                .totalBuyAmount(totalBuyAmount)
                .totalSellAmount(totalSellAmount)
                .realizedProfit(realizedProfit)
                .unrealizedProfit(unrealizedProfit)
                .totalProfit(totalProfit)
                .profitRate(profitRate)
                .holdingCount(holdingsBySymbol.size())
                .totalHoldingValue(totalHoldingValue)
                .totalInvestment(totalInvestment)
                .coinSummaries(coinSummaries)
                .build();
    }

    private Map<String, BigDecimal> getCurrentPrices(List<Transaction> holdings) {
        Map<String, BigDecimal> prices = new HashMap<>();
        
        List<String> symbols = holdings.stream()
                .map(Transaction::getCoinSymbol)
                .distinct()
                .toList();
        
        if (symbols.isEmpty()) return prices;
        
        try {
            List<UpbitTickerDTO> tickers = upbitApiService.getTicker(symbols);   
            
            for (UpbitTickerDTO ticker : tickers) {
                prices.put(ticker.getMarket(), ticker.getTradePrice());
            }
        } catch (Exception e) {
            log.error("현재가 조회 실패: {}", e.getMessage());
        }
        
        return prices;
    }
}