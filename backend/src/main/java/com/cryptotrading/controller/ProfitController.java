package com.cryptotrading.controller;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.profit.CoinProfitDTO;
import com.cryptotrading.dto.profit.PeriodProfitDTO;
import com.cryptotrading.dto.profit.ProfitSummaryDTO;
import com.cryptotrading.service.ProfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 수익 분석 API 컨트롤러
 * Day 31: 기간별/코인별 수익 분석 기능
 */
@Slf4j
@RestController
@RequestMapping("/api/profit")
@RequiredArgsConstructor
@Tag(name = "Profit", description = "수익 분석 API")
public class ProfitController {

    private final ProfitService profitService;

    @Operation(summary = "기간별 수익 요약", description = "오늘/이번달/올해/1년/누적 수익 요약 조회")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProfitSummaryDTO>> getProfitSummary(
            @AuthenticationPrincipal String userId) {
        log.info("기간별 수익 요약 조회 요청 - userId: {}", userId);
        ProfitSummaryDTO summary = profitService.getProfitSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @Operation(summary = "특정 기간 수익 상세", description = "today, month, year, oneYear, total 중 선택")
    @GetMapping("/by-period")
    public ResponseEntity<ApiResponse<PeriodProfitDTO>> getPeriodProfit(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "month") String period) {
        log.info("기간별 수익 상세 조회 요청 - userId: {}, period: {}", userId, period);
        PeriodProfitDTO periodProfit = profitService.getPeriodProfit(userId, period);
        return ResponseEntity.ok(ApiResponse.success(periodProfit));
    }

    @Operation(summary = "코인별 수익 분석", description = "코인별 실현 수익 및 거래 통계")
    @GetMapping("/by-coin")
    public ResponseEntity<ApiResponse<List<CoinProfitDTO>>> getCoinProfits(
            @AuthenticationPrincipal String userId) {
        log.info("코인별 수익 분석 조회 요청 - userId: {}", userId);
        List<CoinProfitDTO> coinProfits = profitService.getCoinProfits(userId);
        return ResponseEntity.ok(ApiResponse.success(coinProfits));
    }
}