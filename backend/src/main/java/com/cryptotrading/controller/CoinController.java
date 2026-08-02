package com.cryptotrading.controller;

import com.cryptotrading.dto.upbit.UpbitAccountDTO;
import com.cryptotrading.dto.upbit.UpbitTickerDTO;
import com.cryptotrading.entity.CoinInfo;
import com.cryptotrading.service.CoinInfoService;
import com.cryptotrading.service.Top10RebalanceService;
import com.cryptotrading.service.UpbitApiService;
import com.cryptotrading.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
@Slf4j
public class CoinController {

    private final CoinInfoService coinInfoService;
    private final UpbitApiService upbitApiService;
    private final UserService userService;
    // ⭐⭐⭐ [신규] Top10 자동 리밸런싱 수동 테스트용 ⭐⭐⭐
    private final Top10RebalanceService top10RebalanceService;

    @GetMapping("/active")
    public ResponseEntity<List<CoinInfo>> getActiveCoins() {
        List<CoinInfo> coins = coinInfoService.getActiveCoins();
        return ResponseEntity.ok(coins);
    }

    @GetMapping("/{symbol}/price")
    public ResponseEntity<UpbitTickerDTO> getCoinPrice(@PathVariable String symbol) {
        try {
            UpbitTickerDTO ticker = coinInfoService.getCurrentPrice(symbol);
            return ResponseEntity.ok(ticker);
        } catch (RuntimeException e) {
            log.error("현재가 조회 실패: symbol={}, error={}", symbol, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/prices")
    public ResponseEntity<List<UpbitTickerDTO>> getMultipleCoinPrices(
            @RequestParam String symbols
    ) {
        try {
            List<String> symbolList = Arrays.asList(symbols.split(","));
            List<UpbitTickerDTO> tickers = upbitApiService.getTicker(symbolList);
            return ResponseEntity.ok(tickers);
        } catch (RuntimeException e) {
            log.error("현재가 조회 실패: symbols={}, error={}", symbols, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(Authentication authentication) {
        try {
            String userId = authentication.getName();
        
            // 사용자의 API 키 조회 및 복호화
            String[] apiKeys = userService.getDecryptedApiKeys(userId);
            String accessKey = apiKeys[0];
            String secretKey = apiKeys[1];
            
            // 업비트 계좌 조회 (API 키 전달)
            List<UpbitAccountDTO> accounts = upbitApiService.getAccounts(accessKey, secretKey);
            return ResponseEntity.ok(accounts);
        } catch (RuntimeException e) {
            log.error("계좌 조회 실패: error={}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateCoinInfo(Authentication authentication) {
        try {
            coinInfoService.updateCoinInfo();
            Map<String, String> response = new HashMap<>();
            response.put("message", "코인 정보가 업데이트되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("코인 정보 업데이트 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * ★★★ 신규: 시가총액 순위 갱신 API ★★★
     * - CoinGecko API로 글로벌 시가총액 조회 후 업비트 내 순위 재매핑
     */
    @PostMapping("/update-market-cap-ranks")
    public ResponseEntity<?> updateMarketCapRanks() {
        try {
            Map<String, Object> result = coinInfoService.updateMarketCapRanks();
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("시가총액 순위 갱신 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    
    /**
     * ⭐⭐⭐ [신규] Top10 자동 리밸런싱 수동 테스트 API ⭐⭐⭐
     * - 04:00 KST 스케줄을 기다리지 않고 즉시 리밸런싱 실행
     * - use_top10_auto_rebalance = true 인 사용자 전원 대상
     * - 테스트 목적: 실제 배포 후에도 관리자가 필요 시 즉시 재실행 가능 (운영 편의성)
     */
    @PostMapping("/rebalance-top10")
    public ResponseEntity<?> rebalanceTop10() {
        try {
            top10RebalanceService.rebalanceAllUsers();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Top10 자동 리밸런싱이 실행되었습니다");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Top10 리밸런싱 수동 실행 실패: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}