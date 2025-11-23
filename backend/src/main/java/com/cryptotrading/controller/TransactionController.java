package com.cryptotrading.controller;

import com.cryptotrading.dto.DashboardStatsDTO;
import com.cryptotrading.dto.TransactionDTO;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 거래 내역 전체 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<TransactionDTO>> getAllTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDTO> transactions = transactionService
                .getAllTransactions(userDetails.getUsername(), pageable);
        
        return ResponseEntity.ok(transactions);
    }

    /**
     * 거래 내역 검색 (복합 조건)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TransactionDTO>> searchTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String coinSymbol,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDTO> transactions = transactionService.searchTransactions(
                userDetails.getUsername(), coinSymbol, status, startDate, endDate, pageable);
        
        return ResponseEntity.ok(transactions);
    }

    /**
     * 보유 중인 자산 조회
     */
    @GetMapping("/holdings")
    public ResponseEntity<List<TransactionDTO>> getHoldings(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        List<TransactionDTO> holdings = transactionService
                .getHoldings(userDetails.getUsername());
        
        return ResponseEntity.ok(holdings);
    }

    /**
     * 특정 거래 상세 조회
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long transactionId) {
        
        TransactionDTO transaction = transactionService
                .getTransaction(userDetails.getUsername(), transactionId);
        
        return ResponseEntity.ok(transaction);
    }

    /**
     * 거래 생성 (매수)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransactionDTO dto) {
        
        try {
            TransactionDTO created = transactionService
                    .createTransaction(userDetails.getUsername(), dto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "거래가 생성되었습니다.");
            response.put("data", created);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("거래 생성 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 거래 수정 (메모 등)
     */
    @PutMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> updateTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long transactionId,
            @RequestBody TransactionDTO dto) {
        
        try {
            TransactionDTO updated = transactionService
                    .updateTransaction(userDetails.getUsername(), transactionId, dto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "거래 정보가 수정되었습니다.");
            response.put("data", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("거래 수정 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 매도 처리
     */
    @PostMapping("/{transactionId}/sell")
    public ResponseEntity<Map<String, Object>> sellTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long transactionId,
            @RequestBody Map<String, BigDecimal> request) {
        
        try {
            BigDecimal soldPrice = request.get("soldPrice");
            if (soldPrice == null || soldPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("유효한 매도 가격을 입력해주세요.");
            }
            
            TransactionDTO sold = transactionService
                    .sellTransaction(userDetails.getUsername(), transactionId, soldPrice);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "매도가 완료되었습니다.");
            response.put("data", sold);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("매도 처리 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 대시보드 통계 조회
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        DashboardStatsDTO stats = transactionService
                .getDashboardStats(userDetails.getUsername());
        
        return ResponseEntity.ok(stats);
    }
}