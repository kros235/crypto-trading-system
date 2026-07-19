package com.cryptotrading.controller.stock;

import com.cryptotrading.dto.stock.StockInfoDTO;
import com.cryptotrading.dto.stock.backtest.StockBacktestRequestDTO;
import com.cryptotrading.dto.stock.backtest.StockBacktestResultDTO;
import com.cryptotrading.service.StockBacktestService;
import com.cryptotrading.service.StockInfoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 주식/ETF 백테스트 컨트롤러
 * Phase 2 Day 62: StockBacktestService + StockBacktestView
 *
 * ⭐⭐⭐ [Day 62 신규] Phase 1 BacktestController(/api/backtest) 구조 재사용 ⭐⭐⭐
 * - 엔드포인트: /api/stock/backtest/**
 * - SecurityConfig의 "/api/stock/**" → authenticated() 규칙에 그대로 포함되므로 별도 보안 설정 불필요
 */
@RestController
@RequestMapping("/api/stock/backtest")
@RequiredArgsConstructor
@Slf4j
public class StockBacktestController {

    private final StockBacktestService stockBacktestService;
    private final StockInfoService stockInfoService;

    /**
     * 백테스트 실행
     */
    @PostMapping("/run")
    public ResponseEntity<StockBacktestResultDTO> runBacktest(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody StockBacktestRequestDTO request) {

        log.info("주식 백테스트 요청: userId={}, stocks={}, period={} ~ {}",
                userId, request.getStockCodes(), request.getStartDate(), request.getEndDate());

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        if (request.getEndDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("종료일은 오늘 이전이어야 합니다.");
        }

        // backtest_master_plan.md 기준: 주식 백테스트 최대 1년
        if (request.getStartDate().plusYears(1).isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("백테스트 기간은 최대 1년입니다.");
        }

        StockBacktestResultDTO result = stockBacktestService.runBacktest(userId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * 백테스트 가능 종목 목록 (사용자가 등록한 활성 종목 기준)
     */
    @GetMapping("/available-stocks")
    public ResponseEntity<Map<String, Object>> getAvailableStocks() {
        List<StockInfoDTO> stocks = stockInfoService.getActiveStocks();

        return ResponseEntity.ok(Map.of(
                "stocks", stocks,
                "maxPeriodDays", 365,
                "minInitialBalance", 100000,
                "totalCount", stocks.size()
        ));
    }

    /**
     * 백테스트 기본 설정값 (StockTradingSetting 기본값과 동일)
     */
    @GetMapping("/default-settings")
    public ResponseEntity<StockBacktestRequestDTO> getDefaultSettings() {
        StockBacktestRequestDTO defaults = StockBacktestRequestDTO.builder()
                .stockCodes(List.of("409820", "409810"))
                .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now().minusDays(1))
                .initialBalance(new BigDecimal("1000000"))
                .basePeriod(20)
                .buyThresholdPct(new BigDecimal("-3"))
                .sellTargetPct(new BigDecimal("2.5"))
                .stopLossPct(new BigDecimal("-5"))
                .maxHoldingsPerStock(3)
                .useTrailingStop(true)
                .trailingStopPct(new BigDecimal("2.5"))
                .rsiPeriod(14)
                .rsiBuyThreshold(35)
                .rsiSellThreshold(65)
                .bbPeriod(20)
                .bbMultiplier(2)
                .volumeThreshold(120)
                .maxHoldingDays(20)
                .dailyTradeLimitPct(20)
                .maxPositionPct(25)
                .dailyStopLossPct(-5)
                .fixedBuyAmount(new BigDecimal("100000"))
                .useRoundRobin(true)
                .build();

        return ResponseEntity.ok(defaults);
    }
}