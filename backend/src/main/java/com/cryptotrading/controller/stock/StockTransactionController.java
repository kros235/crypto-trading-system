package com.cryptotrading.controller.stock;

import com.cryptotrading.dto.stock.StockTransactionDTO;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.service.StockTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주식 거래 내역 컨트롤러
 * Phase 2 Day 58
 *
 * Phase 1 TransactionController 패턴 재사용
 * 엔드포인트: /api/stock/transactions/**
 *
 * [엔드포인트 목록]
 * GET  /api/stock/transactions              - 전체 조회 (페이징)
 * GET  /api/stock/transactions/search       - 복합 조건 검색
 * GET  /api/stock/transactions/holdings     - 보유 중인 주식
 * GET  /api/stock/transactions/{id}         - 상세 조회
 * PUT  /api/stock/transactions/{id}         - 메모 수정
 * POST /api/stock/transactions/{id}/sell    - 수동 매도
 */
@RestController
@RequestMapping("/api/stock/transactions")
@RequiredArgsConstructor
@Slf4j
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    /** 거래 내역 전체 조회 (페이징) */
    @GetMapping
    public ResponseEntity<Page<StockTransactionDTO>> getAllTransactions(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(stockTransactionService.getAllTransactions(userId, pageable));
    }

    /** 거래 내역 복합 검색 */
    @GetMapping("/search")
    public ResponseEntity<Page<StockTransactionDTO>> searchTransactions(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(stockTransactionService.searchTransactions(
                userId, stockCode, status, startDate, endDate, pageable));
    }

    /** 보유 중인 주식 조회 */
    @GetMapping("/holdings")
    public ResponseEntity<List<StockTransactionDTO>> getHoldings(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(stockTransactionService.getHoldings(userId));
    }

    /** 특정 거래 상세 조회 */
    @GetMapping("/{transactionId}")
    public ResponseEntity<StockTransactionDTO> getTransaction(
            @AuthenticationPrincipal String userId,
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(stockTransactionService.getTransaction(userId, transactionId));
    }

    /** 거래 메모 수정 */
    @PutMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> updateTransaction(
            @AuthenticationPrincipal String userId,
            @PathVariable Long transactionId,
            @RequestBody StockTransactionDTO dto) {

        Map<String, Object> response = new HashMap<>();
        try {
            StockTransactionDTO updated = stockTransactionService
                    .updateTransaction(userId, transactionId, dto);
            response.put("success", true);
            response.put("message", "거래 정보가 수정되었습니다.");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[주식 거래 수정 실패]", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /** 수동 매도 처리 */
    @PostMapping("/{transactionId}/sell")
    public ResponseEntity<Map<String, Object>> sellTransaction(
            @AuthenticationPrincipal String userId,
            @PathVariable Long transactionId,
            @RequestBody Map<String, BigDecimal> request) {

        Map<String, Object> response = new HashMap<>();
        try {
            BigDecimal soldPrice = request.get("soldPrice");
            if (soldPrice == null || soldPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("유효한 매도 가격을 입력해주세요.");
            }
            StockTransactionDTO sold = stockTransactionService
                    .sellTransaction(userId, transactionId, soldPrice);
            response.put("success", true);
            response.put("message", "매도가 완료되었습니다.");
            response.put("data", sold);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[주식 수동매도 실패]", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}