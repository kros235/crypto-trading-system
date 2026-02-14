package com.cryptotrading.controller;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.profit.CoinProfitDTO;
import com.cryptotrading.dto.profit.PeriodProfitDTO;
import com.cryptotrading.dto.profit.ProfitSummaryDTO;
// ⭐⭐⭐ [신규 추가] 자산 스냅샷 관련 import ⭐⭐⭐
import com.cryptotrading.dto.profit.DailyAssetSnapshotDTO;
import com.cryptotrading.service.DailyAssetSnapshotService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    // ⭐⭐⭐ [신규 추가] 자산 스냅샷 서비스 ⭐⭐⭐
    private final DailyAssetSnapshotService dailyAssetSnapshotService;

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

    // ⭐⭐⭐ [신규 추가] 자산 스냅샷 API 5개 ⭐⭐⭐

    @Operation(summary = "자산 스냅샷 조회", description = "기간별 일별 자산 스냅샷 (7, month, year, all)")
    @GetMapping("/asset-snapshots")
    public ResponseEntity<ApiResponse<List<DailyAssetSnapshotDTO>>> getAssetSnapshots(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "all") String period) {
        log.info("자산 스냅샷 조회 요청 - userId: {}, period: {}", userId, period);
        List<DailyAssetSnapshotDTO> snapshots = dailyAssetSnapshotService.getSnapshots(userId, period);
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    @Operation(summary = "사용자 지정 기간 자산 스냅샷 조회")
    @GetMapping("/asset-snapshots/range")
    public ResponseEntity<ApiResponse<List<DailyAssetSnapshotDTO>>> getAssetSnapshotsByRange(
            @AuthenticationPrincipal String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("사용자 지정 기간 스냅샷 조회 - userId: {}, {} ~ {}", userId, startDate, endDate);
        List<DailyAssetSnapshotDTO> snapshots = dailyAssetSnapshotService.getSnapshotsByCustomRange(
                userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    @Operation(summary = "불입금액 설정", description = "누적 불입금액을 수동으로 설정")
    @PostMapping("/deposit-amount")
    public ResponseEntity<ApiResponse<String>> updateDepositAmount(
            @AuthenticationPrincipal String userId,
            @RequestParam BigDecimal amount) {
        log.info("불입금액 설정 요청 - userId: {}, amount: {}원", userId, amount);
        dailyAssetSnapshotService.updateDepositAmount(userId, amount);
        return ResponseEntity.ok(ApiResponse.success("불입금액이 " + amount + "원으로 설정되었습니다."));
    }

    @Operation(summary = "수동 스냅샷 생성", description = "현재 자산 기준으로 즉시 스냅샷 생성")
    @PostMapping("/asset-snapshots/create")
    public ResponseEntity<ApiResponse<String>> createSnapshot(
            @AuthenticationPrincipal String userId) {
        log.info("수동 스냅샷 생성 요청 - userId: {}", userId);
        dailyAssetSnapshotService.createDailySnapshot(userId);
        return ResponseEntity.ok(ApiResponse.success("자산 스냅샷이 생성되었습니다."));
    }

    @Operation(summary = "과거 스냅샷 마이그레이션", description = "기존 거래 이력 기반으로 과거 스냅샷 생성")
    @PostMapping("/asset-snapshots/migrate")
    public ResponseEntity<ApiResponse<String>> migrateSnapshots(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) BigDecimal initialDeposit) {
        BigDecimal deposit = initialDeposit != null ? initialDeposit : new BigDecimal("1000000");
        log.info("과거 스냅샷 마이그레이션 요청 - userId: {}, initialDeposit: {}원", userId, deposit);
        dailyAssetSnapshotService.migrateHistoricalSnapshots(userId, deposit);
        return ResponseEntity.ok(ApiResponse.success("과거 스냅샷 마이그레이션이 완료되었습니다."));
    } 
}