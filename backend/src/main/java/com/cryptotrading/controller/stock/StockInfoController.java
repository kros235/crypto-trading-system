package com.cryptotrading.controller.stock;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.stock.StockInfoDTO;
// ⭐⭐⭐ [Day 60 추가] StockPriceDTO import 추가 ⭐⭐⭐
import com.cryptotrading.dto.stock.StockPriceDTO;
import com.cryptotrading.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// ⭐⭐⭐ [Day 60 추가] @AuthenticationPrincipal 사용을 위한 import ⭐⭐⭐
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock/info")
@RequiredArgsConstructor
public class StockInfoController {

    private final StockInfoService stockInfoService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<StockInfoDTO>>> getActiveStocks() {
        List<StockInfoDTO> stocks = stockInfoService.getActiveStocks();
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StockInfoDTO>>> getAllStocks() {
        List<StockInfoDTO> stocks = stockInfoService.getAllStocks();
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<ApiResponse<StockInfoDTO>> getStockByCode(@PathVariable String stockCode) {
        StockInfoDTO stock = stockInfoService.getStockByCode(stockCode);
        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StockInfoDTO>>> searchStocks(@RequestParam String keyword) {
        List<StockInfoDTO> results = stockInfoService.searchStocks(keyword);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockInfoDTO>> addStock(@RequestBody StockInfoDTO dto) {
        StockInfoDTO saved = stockInfoService.addStock(dto);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PatchMapping("/{stockCode}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateStock(@PathVariable String stockCode) {
        stockInfoService.deactivateStock(stockCode);
        return ResponseEntity.ok(ApiResponse.success("종목이 비활성화되었습니다: " + stockCode));
    }

    @PatchMapping("/{stockCode}/activate")
    public ResponseEntity<ApiResponse<String>> activateStock(@PathVariable String stockCode) {
        stockInfoService.activateStock(stockCode);
        return ResponseEntity.ok(ApiResponse.success("종목이 활성화되었습니다: " + stockCode));
    }

    @GetMapping("/etf-type/{etfType}")
    public ResponseEntity<ApiResponse<List<StockInfoDTO>>> getByEtfType(@PathVariable String etfType) {
        List<StockInfoDTO> stocks = stockInfoService.getStocksByEtfType(etfType);
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    // ⭐⭐⭐ [Day 60 추가] 다중 종목 현재가 일괄 조회 엔드포인트 ⭐⭐⭐
    /**
     * 여러 종목의 현재가/변동률을 한 번에 조회
     * - 요청: { "stockCodes": ["409820", "409810", ...] }
     * - 응답: 종목별 StockPriceDTO 리스트 (실패한 종목도 stockCode만 담아 반환)
     * - KIS API 키 미등록 사용자: 모든 항목이 stockCode만 채워진 상태로 반환됨
     */
    @PostMapping("/prices")
    public ResponseEntity<ApiResponse<List<StockPriceDTO>>> getPricesForStocks(
            @AuthenticationPrincipal String userId,
            @RequestBody PricesRequest request) {
        List<StockPriceDTO> prices = stockInfoService.getPricesForStocks(
                userId, request.getStockCodes());
        return ResponseEntity.ok(ApiResponse.success(prices));
    }

    /** 다중 종목 가격 조회 요청 바디 */
    @lombok.Data
    public static class PricesRequest {
        private List<String> stockCodes;
    }
}