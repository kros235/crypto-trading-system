package com.cryptotrading.controller.stock;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.profit.DailyAssetSnapshotDTO;
import com.cryptotrading.dto.profit.StockPeriodProfitDTO;
import com.cryptotrading.dto.profit.StockProfitDTO;
import com.cryptotrading.dto.profit.StockProfitSummaryDTO;
import com.cryptotrading.entity.StockAssetSnapshot;
import com.cryptotrading.service.StockAssetSnapshotService;
import com.cryptotrading.service.StockProfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stock/profit")
@RequiredArgsConstructor
@Tag(name = "StockProfit", description = "주식 수익 분석 API")
public class StockProfitController {

    private final StockProfitService stockProfitService;
    private final StockAssetSnapshotService stockAssetSnapshotService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StockProfitSummaryDTO>> getProfitSummary(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(stockProfitService.getProfitSummary(userId)));
    }

    @GetMapping("/by-period")
    public ResponseEntity<ApiResponse<StockPeriodProfitDTO>> getPeriodProfit(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(ApiResponse.success(stockProfitService.getPeriodProfit(userId, period)));
    }

    // ⭐ [Day 63 신규] 사용자 지정 기간 조회 (Phase 1 ProfitController에는 없는 엔드포인트)
    @GetMapping("/by-range")
    public ResponseEntity<ApiResponse<StockPeriodProfitDTO>> getPeriodProfitByRange(
            @AuthenticationPrincipal String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                stockProfitService.getPeriodProfitByRange(userId, LocalDate.parse(startDate), LocalDate.parse(endDate))));
    }

    @GetMapping("/by-stock")
    public ResponseEntity<ApiResponse<List<StockProfitDTO>>> getStockProfits(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(stockProfitService.getStockProfits(userId)));
    }

    // ⭐ 자산 스냅샷 API - Day 60에서 이미 만들어진 StockAssetSnapshotService 재사용 (신규 로직 없음)
    @GetMapping("/asset-snapshots")
    public ResponseEntity<ApiResponse<List<DailyAssetSnapshotDTO>>> getAssetSnapshots(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "all") String period) {
        List<StockAssetSnapshot> snapshots = stockAssetSnapshotService.getSnapshots(userId, period);
        return ResponseEntity.ok(ApiResponse.success(toDtoList(snapshots)));
    }

    @GetMapping("/asset-snapshots/range")
    public ResponseEntity<ApiResponse<List<DailyAssetSnapshotDTO>>> getAssetSnapshotsByRange(
            @AuthenticationPrincipal String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<StockAssetSnapshot> snapshots = stockAssetSnapshotService.getSnapshotsByRange(
                userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(toDtoList(snapshots)));
    }

    @PostMapping("/asset-snapshots/create")
    public ResponseEntity<ApiResponse<String>> createSnapshot(@AuthenticationPrincipal String userId) {
        stockAssetSnapshotService.createOrUpdateSnapshot(userId);
        return ResponseEntity.ok(ApiResponse.success("주식 자산 스냅샷이 생성되었습니다."));
    }

    // StockAssetSnapshot 엔티티 → 코인의 DailyAssetSnapshotDTO로 변환 (프론트 차트 컴포넌트 1:1 재사용 목적)
    // krwBalance/coinEvaluation 필드는 주식 개념에 없으므로 null
    private List<DailyAssetSnapshotDTO> toDtoList(List<StockAssetSnapshot> snapshots) {
        return snapshots.stream()
                .map(s -> DailyAssetSnapshotDTO.builder()
                        .date(s.getDate() != null ? s.getDate().toString() : null)
                        .evaluationAmount(s.getEvaluationAmount())
                        .depositAmount(s.getDepositAmount())
                        .profitAmount(s.getProfitAmount())
                        .profitRate(s.getProfitRate())
                        .build())
                .toList();
    }
}