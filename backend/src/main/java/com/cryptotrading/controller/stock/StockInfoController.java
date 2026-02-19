package com.cryptotrading.controller;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.stock.StockInfoDTO;
import com.cryptotrading.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}